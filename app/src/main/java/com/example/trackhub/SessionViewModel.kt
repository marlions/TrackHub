package com.example.trackhub

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionPrefs = application.getSharedPreferences(
        SESSION_PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var accessToken by mutableStateOf<String?>(
        sessionPrefs.getString(SESSION_ACCESS_TOKEN_KEY, null)
    )
        private set

    fun saveAccessToken(token: String) {
        sessionPrefs
            .edit()
            .putString(SESSION_ACCESS_TOKEN_KEY, token)
            .apply()

        accessToken = token
    }

    fun clearAccessToken() {
        sessionPrefs
            .edit()
            .remove(SESSION_ACCESS_TOKEN_KEY)
            .apply()

        accessToken = null
    }
}
