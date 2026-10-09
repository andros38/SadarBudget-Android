package id.sadarbudget.mobile.data

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("sadarbudget_session", Context.MODE_PRIVATE)

    var activeUserId: Int
        get() = prefs.getInt("active_user_id", 0)
        set(value) = prefs.edit().putInt("active_user_id", value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", false)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    fun clearSession() {
        prefs.edit().remove("active_user_id").apply()
    }
}
