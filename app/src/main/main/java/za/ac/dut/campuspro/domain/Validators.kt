package za.ac.dut.campuspro.domain

import java.security.MessageDigest

/**
 * Input rules for Campus Pro. Kept free of Android classes so they can be
 * unit tested on the JVM (see ValidatorsTest).
 */
object Validators {

    /** Students use @dut4life.ac.za, staff (drivers) use @dut.ac.za. */
    private val DUT_EMAIL = Regex("^[A-Za-z0-9._%+-]+@(dut4life\\.ac\\.za|dut\\.ac\\.za)$", RegexOption.IGNORE_CASE)

    /** Exactly 11 characters: "\$\$Dut" followed by 6 digits. Case-sensitive. */
    private val PASSWORD = Regex("^\\\$\\\$Dut\\d{6}$")

    /** South African mobile number, e.g. 0821234567. */
    private val PHONE = Regex("^0\\d{9}$")

    /** Bus fleet number, e.g. B1033. */
    private val BUS_NUMBER = Regex("^[A-Za-z]{1,3}\\d{1,5}$")

    private val NAME = Regex("^[A-Za-z][A-Za-z' -]{0,39}$")

    const val PASSWORD_HINT = "Exactly 11 characters: \$\$Dut followed by 6 digits, e.g. \$\$Dut123456"

    fun isDutEmail(email: String) = DUT_EMAIL.matches(email.trim())
    fun isValidPassword(password: String) = PASSWORD.matches(password)
    fun isValidPhone(phone: String) = PHONE.matches(phone.trim().replace(" ", ""))
    fun isValidBusNumber(bus: String) = BUS_NUMBER.matches(bus.trim())
    fun isValidName(name: String) = NAME.matches(name.trim())

    /** Returns an error message, or null when the login input is well formed. */
    fun loginError(email: String, password: String): String? = when {
        email.isBlank() || password.isBlank() -> "Enter your email and password."
        !isDutEmail(email) -> "Only DUT email addresses (@dut4life.ac.za or @dut.ac.za) can sign in."
        !isValidPassword(password) -> "Invalid password. $PASSWORD_HINT."
        else -> null
    }

    /** Common registration checks shared by students and drivers. */
    fun registrationError(firstName: String, surname: String, email: String, password: String): String? = when {
        firstName.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank() ->
            "Please complete all required fields."
        !isValidName(firstName) || !isValidName(surname) -> "Names may only contain letters, spaces, ' and -."
        !isDutEmail(email) -> "Only DUT email addresses (@dut4life.ac.za or @dut.ac.za) can register."
        !isValidPassword(password) -> "Invalid password. $PASSWORD_HINT."
        else -> null
    }

    fun driverDetailsError(busNumber: String, phone: String): String? = when {
        busNumber.isBlank() || phone.isBlank() -> "Enter the bus number and driver phone number."
        !isValidBusNumber(busNumber) -> "Bus number should look like B1033."
        !isValidPhone(phone) -> "Phone number must be 10 digits starting with 0, e.g. 0821234567."
        else -> null
    }

    fun tripDurationError(minutesText: String): String? {
        val minutes = minutesText.trim().toIntOrNull()
        return when {
            minutes == null -> "Enter the expected trip duration in minutes."
            minutes < 1 || minutes > 180 -> "Trip duration must be between 1 and 180 minutes."
            else -> null
        }
    }

    /** Passwords are never stored in plain text; only a SHA-256 hash is kept. */
    fun hashPassword(password: String, email: String): String {
        val salted = email.trim().lowercase() + ":" + password
        val bytes = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
