package github.leavesczy.compose_chat.open.session

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object OpenSessionManager {

    private const val KEY_GROUP = "OpenSession"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "userId"
    private const val KEY_DISPLAY_NAME = "displayName"
    private const val KEY_PERMISSIONS = "permissions"

    private lateinit var preferences: SharedPreferences

    val token: String
        get() = preferences.getString(KEY_TOKEN, "") ?: ""

    val userId: String
        get() = preferences.getString(KEY_USER_ID, "") ?: ""

    val displayName: String
        get() = preferences.getString(KEY_DISPLAY_NAME, "") ?: ""

    val permissions: Set<String>
        get() {
            val rawValue = preferences.getString(KEY_PERMISSIONS, "") ?: ""
            return rawValue.split(separator).filter { it.isNotBlank() }.toSet()
        }

    val isLoggedIn: Boolean
        get() = token.isNotBlank()

    fun init(application: Application) {
        preferences = application.getSharedPreferences(KEY_GROUP, Context.MODE_PRIVATE)
    }

    fun saveSession(
        token: String,
        userId: String?,
        displayName: String,
        permissions: List<String>
    ) {
        preferences.edit {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, userId.orEmpty())
            putString(KEY_DISPLAY_NAME, displayName)
            putString(KEY_PERMISSIONS, permissions.joinToString(separator = separator))
        }
    }

    fun clear() {
        preferences.edit {
            clear()
        }
    }

    private const val separator = "|"

}
