package id.sadarbudget.mobile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import id.sadarbudget.mobile.data.AppRepository
import id.sadarbudget.mobile.data.SessionStore
import id.sadarbudget.mobile.data.SecurityStore
import id.sadarbudget.mobile.data.User

class AppState(context: Context) {
    val session = SessionStore(context)
    val security = SecurityStore(context)
    val repository = AppRepository(context, session)

    var loggedIn by mutableStateOf(session.activeUserId > 0)
        private set
    var user by mutableStateOf<User?>(null)
    var darkTheme by mutableStateOf(session.darkTheme)
        private set
    var checkingSession by mutableStateOf(session.activeUserId > 0)
    var locked by mutableStateOf(security.hasPin(session.activeUserId))
        private set
    var pinEnabled by mutableStateOf(security.hasPin(session.activeUserId))
        private set
    var biometricEnabled by mutableStateOf(security.biometrics(session.activeUserId))
        private set
    var hideAmounts by mutableStateOf(security.hideAmounts(session.activeUserId))
        private set
    var secureWindow by mutableStateOf(security.secureWindow(session.activeUserId))
        private set

    private fun refreshSecurity() {
        val uid = session.activeUserId
        pinEnabled = security.hasPin(uid)
        biometricEnabled = security.biometrics(uid)
        hideAmounts = security.hideAmounts(uid)
        secureWindow = security.secureWindow(uid)
    }
    fun lockIfEnabled() { if (loggedIn && pinEnabled) locked = true }
    fun unlock() { locked = false }
    fun remainingPinWaitMs(): Long = security.remainingLockMs(session.activeUserId)
    fun unlockWithPin(pin: String): Boolean = security.tryUnlock(session.activeUserId, pin).also { if (it) unlock() }
    suspend fun resetPinWithAccountPassword(password: String) {
        repository.verifyAccountPassword(password)
        security.resetPinAfterAccountVerification(session.activeUserId)
        locked = false
        refreshSecurity()
    }
    fun updatePin(pin: String, oldPin: String) {
        security.setPin(session.activeUserId, pin, oldPin)
        refreshSecurity()
    }
    fun removePin(pin: String) {
        security.removePin(session.activeUserId, pin)
        locked = false
        refreshSecurity()
    }
    fun setBiometric(value: Boolean) { security.setBiometrics(session.activeUserId, value); refreshSecurity() }
    fun updateHideAmounts(value: Boolean) { security.setHideAmounts(session.activeUserId, value); refreshSecurity() }
    fun updateSecureWindow(value: Boolean) { security.setSecureWindow(session.activeUserId, value); refreshSecurity() }

    fun updateDarkTheme(value: Boolean) {
        darkTheme = value
        session.darkTheme = value
    }

    suspend fun verifySession() {
        if (session.activeUserId <= 0) {
            checkingSession = false
            loggedIn = false
            user = null
            return
        }
        runCatching { repository.me() }
            .onSuccess { user = it; loggedIn = true }
            .onFailure { session.clearSession(); loggedIn = false; user = null }
        refreshSecurity()
        checkingSession = false
    }

    suspend fun login(email: String, password: String) {
        val auth = repository.login(email, password)
        loggedIn = true
        user = auth.user
        locked = false
        refreshSecurity()
    }

    suspend fun register(name: String, email: String, password: String, confirm: String) {
        val auth = repository.register(name, email, password, confirm)
        loggedIn = true
        user = auth.user
        locked = false
        refreshSecurity()
    }

    suspend fun logout() {
        repository.logout()
        loggedIn = false
        user = null
        locked = false
        refreshSecurity()
    }
}
