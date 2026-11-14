package id.nexrify.music.data.account

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class NexUser(
    val id: String,
    val username: String,
    val display: String,
    /** Foto profil berupa data URL (data:image/jpeg;base64,...), atau kosong. */
    val avatar: String,
    val createdAt: Long,
    /** Role yang ditetapkan server melalui allowlist; bukan input pengguna. */
    val role: String = "user"
)

/**
 * Akun Nexrify (buatan sendiri, bukan login YouTube). Wajib masuk sebelum memakai aplikasi.
 * Token disimpan di perangkat. [user] baru terisi setelah sinkron awal selesai supaya layar utama
 * tidak tampil dengan data lama.
 */
class AccountManager private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("nexrify_account", Context.MODE_PRIVATE)

    @Volatile private var token: String? = prefs.getString(KEY_TOKEN, null)
    private var pending: NexUser? = null

    private val _user = MutableStateFlow(if (token != null) loadUser() else null)
    val user: StateFlow<NexUser?> = _user.asStateFlow()

    fun token(): String? = token
    /** Id akun yang sedang masuk, termasuk yang baru login dan belum dipublikasikan. */
    fun userId(): String? = pending?.id ?: _user.value?.id
    fun isLoggedIn(): Boolean = token != null && _user.value != null

    suspend fun register(username: String, password: String, display: String) {
        val json = AccountApi.auth(
            JSONObject().put("action", "register").put("username", username).put("password", password).put("display", display)
        )
        stage(json)
    }

    suspend fun login(username: String, password: String) {
        val json = AccountApi.auth(JSONObject().put("action", "login").put("username", username).put("password", password))
        stage(json)
    }

    /** Simpan token dan profil, tapi belum dipublikasikan ke UI. Panggil [publish] setelah sinkron awal. */
    private fun stage(json: JSONObject) {
        val u = parseUser(json.getJSONObject("user"))
        token = json.getString("token")
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_USER, userJson(u).toString()).apply()
        pending = u
    }

    fun publish() {
        pending?.let { _user.value = it }
        pending = null
    }

    /** Segarkan profil dari server. Token tidak berlaku lagi: keluar dari akun. */
    suspend fun refresh(): Boolean {
        val t = token ?: return false
        return try {
            val json = AccountApi.auth(JSONObject().put("action", "me"), t)
            val u = parseUser(json.getJSONObject("user"))
            prefs.edit().putString(KEY_USER, userJson(u).toString()).apply()
            _user.value = u
            true
        } catch (e: AccountError) {
            if (e.code == "auth" || e.code == "banned") {
                clearLocal()
                false
            } else true
        }
    }

    suspend fun updateProfile(display: String?, avatar: String?): NexUser {
        val body = JSONObject().put("action", "update")
        if (display != null) body.put("display", display)
        if (avatar != null) body.put("avatar", avatar)
        val json = AccountApi.auth(body, token)
        val u = parseUser(json.getJSONObject("user"))
        prefs.edit().putString(KEY_USER, userJson(u).toString()).apply()
        _user.value = u
        return u
    }

    suspend fun changePassword(old: String, new: String) {
        val json = AccountApi.auth(JSONObject().put("action", "password").put("old", old).put("new", new), token)
        token = json.getString("token")
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    suspend fun deleteAccount(password: String) {
        AccountApi.auth(JSONObject().put("action", "delete").put("password", password), token)
        clearLocal()
    }

    fun clearLocal() {
        token = null
        pending = null
        prefs.edit().clear().apply()
        _user.value = null
    }

    private fun loadUser(): NexUser? = try {
        parseUser(JSONObject(prefs.getString(KEY_USER, null) ?: ""))
    } catch (_: Exception) {
        null
    }

    private fun parseUser(o: JSONObject) = NexUser(
        id = o.getString("id"),
        username = o.getString("username"),
        display = o.optString("display").ifBlank { o.getString("username") },
        avatar = o.optString("avatar", ""),
        createdAt = o.optLong("createdAt", 0L),
        role = o.optString("role", "user").lowercase().takeIf { it in setOf("user", "staff", "admin", "developer") } ?: "user"
    )

    private fun userJson(u: NexUser) = JSONObject()
        .put("id", u.id).put("username", u.username).put("display", u.display)
        .put("avatar", u.avatar).put("createdAt", u.createdAt).put("role", u.role)

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER = "user"

        @Volatile private var instance: AccountManager? = null

        fun init(context: Context): AccountManager =
            instance ?: synchronized(this) {
                instance ?: AccountManager(context.applicationContext).also { instance = it }
            }

        fun getInstance(): AccountManager =
            instance ?: throw IllegalStateException("AccountManager belum di inisialisasi")
    }
}
