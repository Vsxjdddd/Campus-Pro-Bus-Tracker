package za.ac.dut.campuspro.data

/** The two kinds of account supported by Campus Pro. */
enum class Role(val label: String) {
    STUDENT("Student"),
    DRIVER("Bus Driver")
}

/**
 * Direction of a shuttle trip. A student's pickup point decides the direction:
 * picked up at the residence -> travelling to campus, and vice versa.
 */
enum class Direction(val label: String, val pickup: String, val destination: String) {
    RESIDENCE_TO_CAMPUS("Residence → Campus", "Residence", "Campus"),
    CAMPUS_TO_RESIDENCE("Campus → Residence", "Campus", "Residence")
}

data class User(
    val id: String,            // Supabase auth user id (UUID)
    val role: Role,
    val firstName: String,
    val surname: String,
    val email: String,
    // Student-only fields
    val residence: String? = null,
    val direction: Direction? = null,
    // Driver-only fields
    val busNumber: String? = null,
    val phone: String? = null
) {
    val fullName: String get() = "$firstName $surname"
}

/** An active trip started by a driver. Location is simulated from elapsed time. */
data class TripSession(
    val id: Long,              // trips.id in Supabase
    val driverId: String,
    val driverName: String,
    val driverPhone: String,
    val busNumber: String,
    val residence: String,
    val direction: Direction,
    val durationMinutes: Int,
    val startedAt: Long
) {
    /** Unique key for one run of a trip (a driver can run many trips). */
    val tripKey: String get() = id.toString()
}

/** A message shown to students: trip started/completed or a driver update. */
data class TransportAlert(
    val id: Long,
    val title: String,
    val message: String,
    val busNumber: String,
    val residence: String?,
    val direction: Direction?,
    val timestamp: Long
)

/** True when [trip] serves the student's saved residence and direction. */
fun User.isServedBy(trip: TripSession): Boolean =
    role == Role.STUDENT && residence == trip.residence && direction == trip.direction

/** True when [alert] is relevant to this student (general alerts go to everyone). */
fun User.shouldSee(alert: TransportAlert): Boolean =
    role == Role.STUDENT &&
        (alert.residence == null || (alert.residence == residence && alert.direction == direction))
