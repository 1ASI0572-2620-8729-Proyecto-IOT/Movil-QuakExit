package com.quakexit.app.data.local

import android.content.Context
import com.google.gson.Gson
import com.quakexit.app.BuildConfig
import com.quakexit.app.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("quakexit_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _user = MutableStateFlow(prefs.getString("user", null)?.let { runCatching { gson.fromJson(it, User::class.java) }.getOrNull() })
    val user: StateFlow<User?> = _user

    var token: String?
        get() = prefs.getString("token", null)
        private set(v) = prefs.edit().putString("token", v).apply()

    var baseUrl: String
        get() = prefs.getString("base_url", BuildConfig.DEFAULT_BASE_URL)!!
        set(v) = prefs.edit().putString("base_url", if (v.endsWith("/")) v else "$v/").apply()

    var forceSimulated: Boolean
        get() = prefs.getBoolean("force_sim", false)
        set(v) = prefs.edit().putBoolean("force_sim", v).apply()

    var pushToken: String?
        get() = prefs.getString("push_token", null)
        set(v) = prefs.edit().putString("push_token", v).apply()

    fun save(token: String, user: User) {
        this.token = token
        prefs.edit().putString("user", gson.toJson(user)).apply()
        _user.value = user
    }

    fun clearSession() {
        prefs.edit().remove("token").remove("user").apply()
        _user.value = null
    }
}
