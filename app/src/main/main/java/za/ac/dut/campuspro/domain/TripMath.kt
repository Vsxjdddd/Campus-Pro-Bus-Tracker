package za.ac.dut.campuspro.domain

/**
 * Pure functions that turn a trip's start time and duration into progress,
 * ETA and status. In production these values would come from GPS updates;
 * in the prototype they are derived from elapsed time.
 */
object TripMath {

    enum class Status(val label: String, val description: String) {
        EN_ROUTE("En route", "The bus is on its way."),
        ARRIVING_SOON("Arriving soon", "Get ready: the bus is almost there."),
        ARRIVED("Arrived", "The bus has reached its destination.")
    }

    data class Snapshot(
        val progress: Float,     // 0.0 .. 1.0
        val remainingMs: Long,
        val status: Status
    )

    private const val MINUTE_MS = 60_000L
    private const val FIVE_MINUTES_MS = 5 * MINUTE_MS

    fun durationMs(minutes: Int): Long = minutes.coerceAtLeast(1) * MINUTE_MS

    /** "Arriving soon" starts 5 minutes out, or at the last quarter of short trips. */
    fun arrivingSoonThresholdMs(totalMs: Long): Long = minOf(FIVE_MINUTES_MS, totalMs / 4)

    fun snapshot(startedAt: Long, durationMinutes: Int, now: Long): Snapshot {
        val total = durationMs(durationMinutes)
        val elapsed = (now - startedAt).coerceIn(0L, total)
        val remaining = total - elapsed
        val status = when {
            remaining <= 0L -> Status.ARRIVED
            remaining <= arrivingSoonThresholdMs(total) -> Status.ARRIVING_SOON
            else -> Status.EN_ROUTE
        }
        return Snapshot(progress = elapsed.toFloat() / total, remainingMs = remaining, status = status)
    }

    /** Countdown text, e.g. "12:05". */
    fun formatCountdown(remainingMs: Long): String {
        val totalSeconds = ((remainingMs + 999) / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    /** Short ETA text for lists, e.g. "8 min" or "< 1 min". */
    fun formatEtaShort(remainingMs: Long): String = when {
        remainingMs <= 0L -> "Arrived"
        remainingMs < MINUTE_MS -> "< 1 min"
        else -> "${(remainingMs + MINUTE_MS - 1) / MINUTE_MS} min"
    }
}
