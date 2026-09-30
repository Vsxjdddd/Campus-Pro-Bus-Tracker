package za.ac.dut.campuspro.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TripMathTest {

    private val start = 1_000_000L

    @Test
    fun startOfTrip_isEnRouteWithFullTimeRemaining() {
        val s = TripMath.snapshot(start, 20, start)
        assertEquals(TripMath.Status.EN_ROUTE, s.status)
        assertEquals(0f, s.progress, 0.0001f)
        assertEquals(20 * 60_000L, s.remainingMs)
    }

    @Test
    fun halfWay_isFiftyPercent() {
        val s = TripMath.snapshot(start, 20, start + 10 * 60_000L)
        assertEquals(0.5f, s.progress, 0.0001f)
        assertEquals(TripMath.Status.EN_ROUTE, s.status)
    }

    @Test
    fun lastFiveMinutes_isArrivingSoon() {
        val s = TripMath.snapshot(start, 20, start + 15 * 60_000L)
        assertEquals(TripMath.Status.ARRIVING_SOON, s.status)
    }

    @Test
    fun shortTrip_usesLastQuarterForArrivingSoon() {
        // 1-minute demo trip: arriving soon in the last 15 seconds
        assertEquals(TripMath.Status.EN_ROUTE, TripMath.snapshot(start, 1, start + 44_000L).status)
        assertEquals(TripMath.Status.ARRIVING_SOON, TripMath.snapshot(start, 1, start + 45_000L).status)
    }

    @Test
    fun endOfTrip_isArrivedAndClamped() {
        val s = TripMath.snapshot(start, 20, start + 60 * 60_000L)
        assertEquals(TripMath.Status.ARRIVED, s.status)
        assertEquals(1f, s.progress, 0.0001f)
        assertEquals(0L, s.remainingMs)
    }

    @Test
    fun clockBeforeStart_isTreatedAsStart() {
        val s = TripMath.snapshot(start, 10, start - 5_000L)
        assertEquals(0f, s.progress, 0.0001f)
    }

    @Test
    fun countdown_isFormattedAsMinutesAndSeconds() {
        assertEquals("20:00", TripMath.formatCountdown(20 * 60_000L))
        assertEquals("0:59", TripMath.formatCountdown(58_100L))
        assertEquals("0:00", TripMath.formatCountdown(0L))
    }

    @Test
    fun shortEta_roundsUpToWholeMinutes() {
        assertEquals("Arrived", TripMath.formatEtaShort(0L))
        assertEquals("< 1 min", TripMath.formatEtaShort(30_000L))
        assertEquals("2 min", TripMath.formatEtaShort(61_000L))
    }
}
