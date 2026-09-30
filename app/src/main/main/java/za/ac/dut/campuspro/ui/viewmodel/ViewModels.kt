package za.ac.dut.campuspro.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import za.ac.dut.campuspro.CampusProApp
import za.ac.dut.campuspro.data.CampusRepository
import za.ac.dut.campuspro.data.Direction
import za.ac.dut.campuspro.data.Role
import za.ac.dut.campuspro.data.TransportAlert
import za.ac.dut.campuspro.data.TripSession
import za.ac.dut.campuspro.data.User
import za.ac.dut.campuspro.data.isServedBy
import za.ac.dut.campuspro.data.shouldSee
import za.ac.dut.campuspro.domain.TripMath

/** Creates every ViewModel with the shared repository from CampusProApp. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { AuthViewModel(campusApp().repository) }
        initializer { StudentViewModel(campusApp().repository) }
        initializer { DriverViewModel(campusApp().repository) }
        initializer { TrackingViewModel(campusApp().repository, createSavedStateHandle()) }
    }
}

private fun CreationExtras.campusApp(): CampusProApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CampusProApp

private val WHILE_SUBSCRIBED = SharingStarted.WhileSubscribed(5_000)

// -------------------------------------------------------------------- Auth

/** Network calls run in viewModelScope; onResult gets null on success or an error message. */
class AuthViewModel(private val repo: CampusRepository) : ViewModel() {

    fun login(email: String, password: String, role: Role, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repo.login(email, password, role)) }
    }

    fun registerStudent(
        first: String, surname: String, email: String, password: String,
        residence: String?, direction: Direction?, onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repo.registerStudent(first, surname, email, password, residence, direction))
        }
    }

    fun registerDriver(
        first: String, surname: String, email: String, password: String,
        bus: String, phone: String, onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repo.registerDriver(first, surname, email, password, bus, phone))
        }
    }
}

// ----------------------------------------------------------------- Student

class StudentViewModel(private val repo: CampusRepository) : ViewModel() {

    val user: StateFlow<User?> = repo.currentUser
    val now: StateFlow<Long> = repo.now

    /** Only active trips that serve the student's residence and direction. */
    val availableTrips: StateFlow<List<TripSession>> =
        combine(repo.currentUser, repo.sessions) { user, trips ->
            if (user == null) emptyList() else trips.filter { user.isServedBy(it) }
        }.stateIn(viewModelScope, WHILE_SUBSCRIBED, emptyList())

    /** Live buses on other routes, so students can still see what is running. */
    val otherTrips: StateFlow<List<TripSession>> =
        combine(repo.currentUser, repo.sessions) { user, trips ->
            trips.filter { user == null || !user.isServedBy(it) }
        }.stateIn(viewModelScope, WHILE_SUBSCRIBED, emptyList())

    val alerts: StateFlow<List<TransportAlert>> =
        combine(repo.currentUser, repo.alerts) { user, alerts ->
            if (user == null) emptyList() else alerts.filter { user.shouldSee(it) }.take(10)
        }.stateIn(viewModelScope, WHILE_SUBSCRIBED, emptyList())

    val syncError: StateFlow<String?> = repo.syncError

    fun changeRoute(residence: String, direction: Direction, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repo.updateStudentRoute(residence, direction)) }
    }

    fun logout() = repo.logout()
}

// ------------------------------------------------------------------ Driver

class DriverViewModel(private val repo: CampusRepository) : ViewModel() {

    val user: StateFlow<User?> = repo.currentUser
    val now: StateFlow<Long> = repo.now

    val activeTrip: StateFlow<TripSession?> =
        combine(repo.currentUser, repo.sessions) { user, trips ->
            trips.find { it.driverId == user?.id }
        }.stateIn(viewModelScope, WHILE_SUBSCRIBED, null)

    val syncError: StateFlow<String?> = repo.syncError

    fun startTrip(residence: String?, direction: Direction, minutes: Int, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repo.startTrip(residence, direction, minutes)) }
    }

    fun endTrip(onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repo.endTrip()) }
    }

    fun sendUpdate(text: String, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repo.sendDriverUpdate(text)) }
    }

    fun logout() = repo.logout()
}

// ---------------------------------------------------------------- Tracking

data class TrackingUiState(
    val trip: TripSession? = null,
    val snapshot: TripMath.Snapshot? = null,
    val isLive: Boolean = false,
    val alerts: List<TransportAlert> = emptyList(),
    /** True when the trip could not be found (it ended before the screen opened). */
    val notFound: Boolean = false
)

/**
 * Tracks one bus (by driver id from the navigation route). When the trip ends
 * the last known trip is kept so the screen can show "Arrived" instead of
 * going blank.
 */
class TrackingViewModel(
    private val repo: CampusRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>("tripId") ?: -1L
    private var lastSeen: TripSession? = null

    val state: StateFlow<TrackingUiState> =
        combine(repo.sessions, repo.now, repo.alerts, repo.hasSynced) { trips, now, alerts, synced ->
            val live = trips.find { it.id == tripId }
            if (live != null) lastSeen = live
            val trip = live ?: lastSeen
            if (trip == null) {
                TrackingUiState(notFound = synced) // before the first sync: keep loading
            } else {
                val snapshot = if (live != null) {
                    TripMath.snapshot(trip.startedAt, trip.durationMinutes, now)
                } else {
                    TripMath.Snapshot(progress = 1f, remainingMs = 0L, status = TripMath.Status.ARRIVED)
                }
                TrackingUiState(
                    trip = trip,
                    snapshot = snapshot,
                    isLive = live != null,
                    alerts = alerts.filter { it.residence == trip.residence && it.direction == trip.direction }.take(5)
                )
            }
        }.stateIn(viewModelScope, WHILE_SUBSCRIBED, TrackingUiState())
}
