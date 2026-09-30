package za.ac.dut.campuspro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun dutEmails_areAccepted() {
        assertTrue(Validators.isDutEmail("21234567@dut4life.ac.za"))
        assertTrue(Validators.isDutEmail("driver.one@dut.ac.za"))
        assertTrue(Validators.isDutEmail("  Student@DUT4LIFE.AC.ZA "))
    }

    @Test
    fun nonDutEmails_areRejected() {
        assertFalse(Validators.isDutEmail("student@gmail.com"))
        assertFalse(Validators.isDutEmail("student@dut.ac.za.fake.com"))
        assertFalse(Validators.isDutEmail("no-at-sign.dut.ac.za"))
        assertFalse(Validators.isDutEmail(""))
    }

    @Test
    fun password_mustBeDollarDollarDutPlusSixDigits() {
        assertTrue(Validators.isValidPassword("\$\$Dut123456"))
        assertFalse(Validators.isValidPassword("\$\$dut123456"))   // case-sensitive
        assertFalse(Validators.isValidPassword("\$\$Dut12345"))    // 5 digits
        assertFalse(Validators.isValidPassword("\$\$Dut1234567"))  // 7 digits
        assertFalse(Validators.isValidPassword("\$Dut1234567"))    // one dollar sign
        assertFalse(Validators.isValidPassword("\$\$Dut12a456"))   // letter in digits
    }

    @Test
    fun loginError_reportsEachInvalidCase() {
        assertNotNull(Validators.loginError("", ""))
        assertNotNull(Validators.loginError("a@gmail.com", "\$\$Dut123456"))
        assertNotNull(Validators.loginError("a@dut4life.ac.za", "password"))
        assertNull(Validators.loginError("a@dut4life.ac.za", "\$\$Dut123456"))
    }

    @Test
    fun driverDetails_areValidated() {
        assertNull(Validators.driverDetailsError("B1033", "0821234567"))
        assertNotNull(Validators.driverDetailsError("", "0821234567"))
        assertNotNull(Validators.driverDetailsError("1033", "0821234567"))
        assertNotNull(Validators.driverDetailsError("B1033", "821234567"))
    }

    @Test
    fun tripDuration_mustBeBetween1And180() {
        assertNull(Validators.tripDurationError("20"))
        assertNull(Validators.tripDurationError("1"))
        assertNotNull(Validators.tripDurationError("0"))
        assertNotNull(Validators.tripDurationError("181"))
        assertNotNull(Validators.tripDurationError("abc"))
    }

    @Test
    fun passwordHash_isStableAndNotPlainText() {
        val a = Validators.hashPassword("\$\$Dut123456", "A@dut4life.ac.za")
        val b = Validators.hashPassword("\$\$Dut123456", "a@dut4life.ac.za")
        assertEquals(a, b)                    // email is normalised
        assertEquals(64, a.length)            // SHA-256 hex
        assertNotEquals("\$\$Dut123456", a)
        assertNotEquals(a, Validators.hashPassword("\$\$Dut123456", "b@dut4life.ac.za")) // salted per user
    }
}
