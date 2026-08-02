package com.live.notesapp.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "notesapp_auth_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_AUTH_TOKEN = "auth_token"
    }

    fun saveSession(
        userId: String,
        email: String,
        displayName: String? = null,
        authToken: String? = null
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_EMAIL, email)
            putString(KEY_DISPLAY_NAME, displayName ?: "")
            putString(KEY_AUTH_TOKEN, authToken ?: "")
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    fun getDisplayName(): String? {
        return prefs.getString(KEY_DISPLAY_NAME, null)
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
