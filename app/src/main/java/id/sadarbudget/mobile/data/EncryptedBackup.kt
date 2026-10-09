package id.sadarbudget.mobile.data

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Format SBB1: magic(4), PBKDF2 salt(16), AES-GCM nonce(12), ciphertext+tag. */
object EncryptedBackup {
    private val magic = "SBB1".toByteArray(StandardCharsets.US_ASCII)
    private const val iterations = 210_000
    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        val input = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(input).encoded, "AES") }
        finally { input.clearPassword() }
    }
    fun encrypt(data: ByteArray, password: String): ByteArray {
        if (password.length < 8) throw ApiFailure("Kata sandi backup minimal 8 karakter.")
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        val ciphertext = cipher.doFinal(data)
        return ByteBuffer.allocate(4 + 16 + 12 + ciphertext.size).put(magic).put(salt).put(nonce).put(ciphertext).array()
    }
    fun decrypt(data: ByteArray, password: String): ByteArray {
        if (data.size < 4 + 16 + 12 + 16 || !data.copyOfRange(0,4).contentEquals(magic)) throw ApiFailure("Berkas bukan backup terenkripsi SadarBudget (.sbb).")
        val salt = data.copyOfRange(4, 20)
        val nonce = data.copyOfRange(20, 32)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, nonce))
            cipher.updateAAD(magic)
            cipher.doFinal(data.copyOfRange(32, data.size))
        } catch (_: AEADBadTagException) {
            throw ApiFailure("Kata sandi backup salah atau berkas telah berubah/rusak.")
        }
    }
}
