package id.sadarbudget.mobile.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 180_000
    private const val KEY_LENGTH = 256

    fun create(password: String): Pair<String, String> {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(salt, Base64.NO_WRAP) to hash(password, salt)
    }

    fun verify(password: String, saltBase64: String, expectedHash: String): Boolean {
        val salt = runCatching { Base64.decode(saltBase64, Base64.NO_WRAP) }.getOrNull() ?: return false
        val actual = hash(password, salt)
        if (actual.length != expectedHash.length) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].code xor expectedHash[i].code)
        return diff == 0
    }

    private fun hash(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
    }
}
