package za.ac.dut.campuspro.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import za.ac.dut.campuspro.domain.TripMath
import za.ac.dut.campuspro.domain.Validators

/** Sends a system notification. Implemented by NotificationHelper. */
fun interface TripNotifier {
    fun sendNotification(id: Int, title: String, text: String)
}

/**
 * Single source of truth for Campus Pro (the real-time data layer).
 *
 * Accounts use Supabase Auth; profiles, trips and alerts live in the Supabase
 * Postgres database. Every 2 seconds the app fetches the active trips and the
 * latest alerts, so a driver and students on different phones see the same
 * bus. A 1-second clock drives the countdown and the moving bus marker.
 * Screens only see StateFlows, so they did not change when Supabase was added.
 */
class CampusRepository(
    context: Context,
    private val notifier: TripNotifier
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val api = SupabaseApi(prefs)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val syncLock = Mutex()

    private val _currentUser = MutableStateFlow(if (api.session != null) loadCachedUser() else null)
    private val _allTrips = MutableStateFlow<List<TripSession>>(emptyList())
    private val _sessions = MutableStateFlow<List<TripSession>>(emptyList())
    private val _alerts = MutableStateFlow<List<TransportAlert>>(emptyList())
    private val _now = MutableStateFlow(System.currentTimeMillis())
    private val _syncError = MutableStateFlow<String?>(null)
    private val _hasSynced = MutableStateFlow(false)

    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    /** Trips that are live right now (expired ones are filtered out). */
    val sessions: StateFlow<List<TripSession>> = _sessions.asStateFlow()
    val alerts: StateFlow<List<TransportAlert>> = _alerts.asStateFlow()
    /** Server-aligned clock, ticks once per second. */
    val now: StateFlow<Long> = _now.asStateFlow()
    /** Last connection problem, or null when syncing works. */
    val syncError: StateFlow<String?> = _syncError.asStateFlow()
    /** True once trips have been loaded from the server at least once. */
    val hasSynced: StateFlow<Boolean> = _hasSynced.asStateFlow()

    private val arrivingSoonNotified = mutableSetOf<Long>()
    private val arrivedNotified = mutableSetOf<Long>()
    private val autoEndRequested = mutableSetOf<Long>()
    private var lastAlertIdSeen: Long? = null

    init {
        scope.launch {
            var seconds = 0
            while (isActive) {
                _now.value = System.currentTimeMillis() + api.serverOffsetMs
                tick(_now.value)
                if (seconds % SYNC_EVERY_SECONDS == 0) sync()
                seconds++
                delay(1_000)
            }
        }
    }

    // ---------------------------------------------------------------- Auth

    /** Returns null on success, otherwise an error message. */
    suspend fun registerStudent(
        firstName: String, surname: String, email: String, password: String,
        residence: String?, direction: Direction?
    ): String? {
        Validators.registrationError(firstName, surname, email, password)?.let { return it }
        if (residence == null || direction == null) return "Please select your residence and pickup point."
        val metadata = JSONObject()
            .put("role", Role.STUDENT.name)
            .put("first_name", firstName.trim())
            .put("surname", surname.trim())
            .put("residence", residence)
            .put("direction", direction.name)
        return call { api.signUp(email.trim().lowercase(), password, metadata) }
    }

    suspend fun registerDriver(
        firstName: String, surname: String, email: String, password: String,
        busNumber: String, phone: String
    ): String? {
        Validators.registrationError(firstName, surname, email, password)?.let { return it }
        Validators.driverDetailsError(busNumber, phone)?.let { return it }
        val metadata = JSONObject()
            .put("role", Role.DRIVER.name)
            .put("first_name", firstName.trim())
            .put("surname", surname.trim())
            .put("bus_number", busNumber.trim().uppercase())
            .put("phone", phone.trim().replace(" ", ""))
        return call { api.signUp(email.trim().lowercase(), password, metadata) }
    }

    suspend fun login(email: String, password: String, role: Role): String? {
        Validators.loginError(email, password)?.let { return it }
        return call {
            val session = api.signIn(email.trim().lowercase(), password)
            val user = fetchProfile(session.userId)
            if (user == null || user.role != role) {
                api.signOut()
                throw SupabaseException("No ${role.label.lowercase()} account matches this email and password.")
            }
            cacheUser(user)
            withContext(Dispatchers.Main) {
                resetTracking()
                _currentUser.value = user
            }
            syncNow()
        }
    }

    fun logout() {
        _currentUser.value = null
        _allTrips.value = emptyList()
        _sessions.value = emptyList()
        _alerts.value = emptyList()
        _hasSynced.value = false
        _syncError.value = null
        resetTracking()
        prefs.edit().remove(KEY_USER).apply()
        scope.launch(Dispatchers.IO) { api.signOut() } // clears the local session first, then revokes
    }

    /** Students can change their pickup point / residence at any time. */
    suspend fun updateStudentRoute(residence: String, direction: Direction): String? {
        val user = _currentUser.value ?: return "Please log in again."
        return call {
            api.rpc("update_route", JSONObject().put("p_residence", residence).put("p_direction", direction.name))
            val updated = user.copy(residence = residence, direction = direction)
            cacheUser(updated)
            withContext(Dispatchers.Main) { _currentUser.value = updated }
        }
    }

    // --------------------------------------------------------------- Trips

    suspend fun startTrip(residence: String?, direction: Direction, durationMinutes: Int): String? {
        val driver = _currentUser.value ?: return "Please log in again."
        if (driver.role != Role.DRIVER) return "Only drivers can start trips."
        if (residence == null) return "Select the residence served by this trip."
        return call {
            api.rpc(
                "start_trip",
                JSONObject()
                    .put("p_residence", residence)
                    .put("p_direction", direction.name)
                    .put("p_duration", durationMinutes)
            )
            syncNow()
        }
    }

    /** Ends the signed-in driver's trip. */
    suspend fun endTrip(autoCompleted: Boolean = false): String? = call {
        api.rpc("end_trip", JSONObject().put("p_auto", autoCompleted))
        syncNow()
    }

    suspend fun sendDriverUpdate(text: String): String? {
        if (text.isBlank()) return "Type a message first."
        if (text.length > 160) return "Keep updates under 160 characters."
        return call {
            api.rpc("send_update", JSONObject().put("p_message", text.trim()))
            syncNow()
        }
    }

    // ---------------------------------------------------------------- Sync

    private fun sync() {
        if (_currentUser.value == null || api.session == null) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) { syncNow() }
                _syncError.value = null
            } catch (e: SupabaseException) {
                _syncError.value = e.message
            } catch (e: Exception) {
                _syncError.value = "Could not reach the server."
            }
        }
    }

    /** Fetches active trips and the latest alerts. Runs on Dispatchers.IO. */
    private suspend fun syncNow() = syncLock.withLock {
        val trips = api.select(
            "trips",
            "select=*&active=eq.true&order=started_at.desc"
        ).toList { it.toTrip() }
        val alerts = api.select(
            "alerts",
            "select=*&order=id.desc&limit=$MAX_ALERTS"
        ).toList { it.toAlert() }

        withContext(Dispatchers.Main) {
            _allTrips.value = trips
            _sessions.value = trips.filter { !isExpired(it, _now.value) }
            notifyNewAlerts(alerts)
            _alerts.value = alerts
            _hasSynced.value = true
        }
    }

    /** Student gets a notification for new driver updates on their route. */
    private fun notifyNewAlerts(alerts: List<TransportAlert>) {
        val newestId = alerts.maxOfOrNull { it.id } ?: return
        val previous = lastAlertIdSeen
        lastAlertIdSeen = maxOf(newestId, previous ?: 0L)
        if (previous == null) return // first load: don't replay old alerts
        val user = _currentUser.value ?: return
        alerts.filter { it.id > previous && it.title.startsWith("Driver update") && user.shouldSee(it) }
            .forEach { notifier.sendNotification(it.id.toInt(), it.title, it.message) }
    }

    /** Called every second: moves buses, sends arrival alerts, ends finished trips. */
    private fun tick(now: Long) {
        val user = _currentUser.value ?: return
        val trips = _allTrips.value
        _sessions.value = trips.filter { !isExpired(it, now) }

        for (trip in trips) {
            val snap = TripMath.snapshot(trip.startedAt, trip.durationMinutes, now)
            when (snap.status) {
                TripMath.Status.ARRIVING_SOON -> {
                    if (user.isServedBy(trip) && arrivingSoonNotified.add(trip.id)) {
                        notifier.sendNotification(
                            (trip.id * 10 + 1).toInt(),
                            "Bus ${trip.busNumber} arriving soon",
                            "About ${TripMath.formatEtaShort(snap.remainingMs)} away. Please make your way to the pickup point."
                        )
                    }
                }
                TripMath.Status.ARRIVED -> {
                    if (user.isServedBy(trip) && arrivedNotified.add(trip.id)) {
                        notifier.sendNotification(
                            (trip.id * 10 + 2).toInt(),
                            "Bus ${trip.busNumber} has arrived",
                            "Your bus has reached the ${trip.direction.destination.lowercase()} stop."
                        )
                    }
                    // The driver's own phone closes the trip on the server
                    if (trip.driverId == user.id && autoEndRequested.add(trip.id)) {
                        scope.launch { endTrip(autoCompleted = true) }
                    }
                }
                TripMath.Status.EN_ROUTE -> Unit
            }
        }
    }

    private fun isExpired(trip: TripSession, now: Long) =
        now >= trip.startedAt + TripMath.durationMs(trip.durationMinutes)

    private fun resetTracking() {
        arrivingSoonNotified.clear()
        arrivedNotified.clear()
        autoEndRequested.clear()
        lastAlertIdSeen = null
    }

    /** Runs a network call on IO; returns null on success or the error message. */
    private suspend fun call(block: suspend () -> Unit): String? = withContext(Dispatchers.IO) {
        try {
            block()
            null
        } catch (e: SupabaseException) {
            e.message ?: "Something went wrong."
        } catch (e: Exception) {
            "Something went wrong: ${e.message}"
        }
    }

    // ------------------------------------------------------------- Parsing

    private fun fetchProfile(userId: String): User? {
        val rows = api.select("profiles", "select=*&id=eq.${SupabaseApi.encode(userId)}")
        return if (rows.length() == 0) null else rows.getJSONObject(0).toUser()
    }

    private fun cacheUser(user: User) {
        prefs.edit().putString(KEY_USER, user.toJson().toString()).apply()
    }

    private fun loadCachedUser(): User? = try {
        prefs.getString(KEY_USER, null)?.let { JSONObject(it).toUser() }
    } catch (e: Exception) {
        null
    }

    private fun <T> JSONArray.toList(parse: (JSONObject) -> T): List<T> =
        (0 until length()).map { parse(getJSONObject(it)) }

    private fun User.toJson() = JSONObject().apply {
        put("id", id); put("role", role.name); put("first_name", firstName); put("surname", surname)
        put("email", email)
        residence?.let { put("residence", it) }
        direction?.let { put("direction", it.name) }
        busNumber?.let { put("bus_number", it) }
        phone?.let { put("phone", it) }
    }

    private fun JSONObject.toUser() = User(
        id = getString("id"),
        role = Role.valueOf(getString("role")),
        firstName = getString("first_name"),
        surname = getString("surname"),
        email = getString("email"),
        residence = stringOrNull("residence"),
        direction = stringOrNull("direction")?.let { Direction.valueOf(it) },
        busNumber = stringOrNull("bus_number"),
        phone = stringOrNull("phone")
    )

    private fun JSONObject.toTrip() = TripSession(
        id = getLong("id"),
        driverId = getString("driver_id"),
        driverName = getString("driver_name"),
        driverPhone = stringOrNull("driver_phone").orEmpty(),
        busNumber = getString("bus_number"),
        residence = getString("residence"),
        direction = Direction.valueOf(getString("direction")),
        durationMinutes = getInt("duration_minutes"),
        startedAt = getLong("started_at")
    )

    private fun JSONObject.toAlert() = TransportAlert(
        id = getLong("id"),
        title = getString("title"),
        message = getString("message"),
        busNumber = getString("bus_number"),
        residence = stringOrNull("residence"),
        direction = stringOrNull("direction")?.let { Direction.valueOf(it) },
        timestamp = getLong("created_at")
    )

    private fun JSONObject.stringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private companion object {
        const val PREFS_NAME = "campus_pro"
        const val KEY_USER = "current_user_json"
        const val MAX_ALERTS = 30
        const val SYNC_EVERY_SECONDS = 2
    }
}
