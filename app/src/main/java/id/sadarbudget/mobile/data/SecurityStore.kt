package id.sadarbudget.mobile.data

import android.content.Context

/** Pengaturan keamanan lokal per akun. PIN tidak pernah disimpan sebagai teks biasa. */
class SecurityStore(context: Context) {
    private val prefs = context.getSharedPreferences("sadarbudget_security_v1", Context.MODE_PRIVATE)
    private fun key(userId: Int, name: String) = "u${userId}_$name"

    fun hasPin(userId: Int): Boolean = userId > 0 && prefs.contains(key(userId, "pin_hash"))
    fun verifyPin(userId: Int, pin: String): Boolean {
        if (!hasPin(userId) || !pin.matches(Regex("\\d{6}"))) return false
        val salt = prefs.getString(key(userId, "pin_salt"), null) ?: return false
        val hash = prefs.getString(key(userId, "pin_hash"), null) ?: return false
        return PasswordHasher.verify(pin, salt, hash)
    }

    fun remainingLockMs(userId: Int): Long = (prefs.getLong(key(userId, "unlock_wait_until"), 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
    fun tryUnlock(userId: Int, pin: String): Boolean {
        if (remainingLockMs(userId) > 0) return false
        if (verifyPin(userId, pin)) {
            prefs.edit().remove(key(userId, "unlock_fails")).remove(key(userId, "unlock_wait_until")).apply()
            return true
        }
        val count = prefs.getInt(key(userId, "unlock_fails"), 0) + 1
        if (count >= 5) {
            prefs.edit().putInt(key(userId, "unlock_fails"), 0)
                .putLong(key(userId, "unlock_wait_until"), System.currentTimeMillis() + 30_000L).apply()
        } else prefs.edit().putInt(key(userId, "unlock_fails"), count).apply()
        return false
    }

    fun setPin(userId: Int, pin: String, oldPin: String = "") {
        require(userId > 0) { "Akun tidak ditemukan." }
        require(pin.matches(Regex("\\d{6}"))) { "PIN harus berisi tepat 6 angka." }
        if (hasPin(userId) && !verifyPin(userId, oldPin)) throw ApiFailure("PIN lama salah.")
        val (salt, hash) = PasswordHasher.create(pin)
        prefs.edit().putString(key(userId, "pin_salt"), salt)
            .putString(key(userId, "pin_hash"), hash).apply()
    }

    fun removePin(userId: Int, pin: String) {
        if (!verifyPin(userId, pin)) throw ApiFailure("PIN tidak cocok.")
        resetPinAfterAccountVerification(userId)
    }
    /** Hanya dipanggil setelah kata sandi akun diverifikasi oleh repository. */
    fun resetPinAfterAccountVerification(userId: Int) {
        prefs.edit().remove(key(userId, "pin_salt")).remove(key(userId, "pin_hash"))
            .remove(key(userId, "unlock_fails")).remove(key(userId, "unlock_wait_until"))
            .putBoolean(key(userId, "biometric"), false).apply()
    }

    fun biometrics(userId: Int) = hasPin(userId) && prefs.getBoolean(key(userId, "biometric"), false)
    fun setBiometrics(userId: Int, value: Boolean) {
        require(!value || hasPin(userId)) { "Buat PIN terlebih dahulu." }
        prefs.edit().putBoolean(key(userId, "biometric"), value).apply()
    }
    fun hideAmounts(userId: Int) = prefs.getBoolean(key(userId, "hide_amounts"), false)
    fun setHideAmounts(userId: Int, value: Boolean) = prefs.edit().putBoolean(key(userId, "hide_amounts"), value).apply()
    fun secureWindow(userId: Int) = prefs.getBoolean(key(userId, "secure_window"), false)
    fun setSecureWindow(userId: Int, value: Boolean) = prefs.edit().putBoolean(key(userId, "secure_window"), value).apply()
}
