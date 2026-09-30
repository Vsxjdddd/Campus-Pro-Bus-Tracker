package za.ac.dut.campuspro.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A readable error from Supabase or the network. */
class SupabaseException(message: String) : Exception(message)

/**
 * Minimal Supabase client built on HttpURLConnection (no extra libraries).
 *
 * - Auth: /auth/v1/signup, /auth/v1/token (password + refresh_token)
 * - Data: /rest/v1/<table> for reads, /rest/v1/rpc/<function> for writes
 *
 * All calls block, so they must run on Dispatchers.IO (CampusRepository does this).
 */
class SupabaseApi(private val prefs: SharedPreferences) {

    data class Session(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val expiresAtSec: Long
    )

    @Volatile
    var session: Session? = loadSession()
        private set

    /** Server clock minus phone clock, from the HTTP Date header. Keeps ETAs in sync across phones. */
    @Volatile
    var serverOffsetMs: Long = 0L
        private set

    // ------------------------------------------------------------------ auth

    /** Creates the account. The database trigger creates the matching profile row. */
    fun signUp(email: String, password: String, metadata: JSONObject) {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("data", metadata)
        request("POST", "/auth/v1/signup", body.toString(), authenticated = false)
        // Keep the user signed out after registering: they log in on the Login tab.
        clearSession()
    }

    fun signIn(email: String, password: String): Session {
        val body = JSONObject().put("email", email).put("password", password)
        val json = JSONObject(request("POST", "/auth/v1/token?grant_type=password", body.toString(), authenticated = false))
        return saveSession(json)
    }

    /** Signs out locally at once, then revokes that session's token on the server. */
    fun signOut() {
        val current = session ?: return
        clearSession()
        try {
            request("POST", "/auth/v1/logout", "{}", authenticated = true, tokenOverride = current.accessToken)
        } catch (_: Exception) {
            // Signing out locally is enough if the network call fails
        }
    }

    private fun refresh(): Boolean {
        val current = session ?: return false
        return try {
            val body = JSONObject().put("refresh_token", current.refreshToken)
            val json = JSONObject(request("POST", "/auth/v1/token?grant_type=refresh_token", body.toString(), authenticated = false))
            saveSession(json)
            true
        } catch (e: SupabaseException) {
            clearSession()
            false
        }
    }

    // ------------------------------------------------------------------ data

    /** GET /rest/v1/<table>?<query> as a JSON array. */
    fun select(table: String, query: String): JSONArray =
        JSONArray(authedRequest("GET", "/rest/v1/$table?$query", null))

    /** POST /rest/v1/rpc/<function> with named arguments. Returns the raw response body. */
    fun rpc(function: String, args: JSONObject = JSONObject()): String =
        authedRequest("POST", "/rest/v1/rpc/$function", args.toString())

    private fun authedRequest(method: String, path: String, body: String?): String {
        val current = session ?: throw SupabaseException("Please log in again.")
        if (current.expiresAtSec - 60 < System.currentTimeMillis() / 1000) {
            if (!refresh()) throw SupabaseException("Your session expired. Please log in again.")
        }
        return try {
            request(method, path, body, authenticated = true)
        } catch (e: UnauthorizedException) {
            if (!refresh()) throw SupabaseException("Your session expired. Please log in again.")
            request(method, path, body, authenticated = true)
        }
    }

    private class UnauthorizedException(message: String) : Exception(message)

    // --------------------------------------------------------------- network

    private fun request(
        method: String,
        path: String,
        body: String?,
        authenticated: Boolean,
        tokenOverride: String? = null
    ): String {
        if (!SupabaseConfig.isConfigured) {
            throw SupabaseException("Supabase key missing: paste your anon key into SupabaseConfig.kt.")
        }
        val connection = try {
            URL(SupabaseConfig.URL + path).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw SupabaseException("No internet connection.")
        }
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
            connection.setRequestProperty("Accept", "application/json")
            if (authenticated) {
                val token = tokenOverride ?: session?.accessToken ?: throw SupabaseException("Please log in again.")
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            val code = connection.responseCode
            connection.date.takeIf { it > 0 }?.let { serverOffsetMs = it - System.currentTimeMillis() }
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (code == 401 && authenticated) throw UnauthorizedException(text)
            if (code !in 200..299) throw SupabaseException(readableError(text, code))
            return text
        } catch (e: IOException) {
            throw SupabaseException("Could not reach the server. Check your internet connection.")
        } finally {
            connection.disconnect()
        }
    }

    /** Turns Supabase error JSON into a message people can read. */
    private fun readableError(body: String, code: Int): String {
        val raw = try {
            val json = JSONObject(body)
            listOf("message", "msg", "error_description", "error")
                .firstNotNullOfOrNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
        } catch (_: Exception) {
            null
        } ?: "Server error ($code)."

        return when {
            raw.contains("Invalid login credentials", ignoreCase = true) -> "Incorrect email or password."
            raw.contains("already registered", ignoreCase = true) -> "An account with this email already exists."
            raw.contains("Email not confirmed", ignoreCase = true) ->
                "Email not confirmed. Turn off 'Confirm email' in Supabase › Authentication › Sign In / Providers › Email."
            raw.contains("Database error saving new user", ignoreCase = true) ->
                "Could not create the account. Check the details and use a DUT email address."
            raw.contains("rate limit", ignoreCase = true) -> "Too many attempts. Wait a minute and try again."
            else -> raw
        }
    }

    // ------------------------------------------------------- session storage

    private fun saveSession(json: JSONObject): Session {
        val user = json.optJSONObject("user") ?: throw SupabaseException("Unexpected login response.")
        val expiresIn = json.optLong("expires_in", 3600)
        val saved = Session(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            userId = user.getString("id"),
            expiresAtSec = System.currentTimeMillis() / 1000 + expiresIn
        )
        prefs.edit()
            .putString(KEY_ACCESS, saved.accessToken)
            .putString(KEY_REFRESH, saved.refreshToken)
            .putString(KEY_USER, saved.userId)
            .putLong(KEY_EXPIRES, saved.expiresAtSec)
            .apply()
        session = saved
        return saved
    }

    private fun loadSession(): Session? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return null
        val user = prefs.getString(KEY_USER, null) ?: return null
        return Session(access, refresh, user, prefs.getLong(KEY_EXPIRES, 0L))
    }

    fun clearSession() {
        prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).remove(KEY_USER).remove(KEY_EXPIRES).apply()
        session = null
    }

    companion object {
        private const val KEY_ACCESS = "sb_access_token"
        private const val KEY_REFRESH = "sb_refresh_token"
        private const val KEY_USER = "sb_user_id"
        private const val KEY_EXPIRES = "sb_expires_at"

        fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    }
}
