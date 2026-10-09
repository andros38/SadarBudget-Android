package id.sadarbudget.mobile.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupTest {
    @Test
    fun roundTripRestoresOriginalBytes() {
        val original = "-- SADARBUDGET_PAYLOAD_BEGIN\\ncontoh-transaksi-123".toByteArray()
        val encrypted = EncryptedBackup.encrypt(original, "contoh-password-kuat-123")
        assertFalse(original.contentEquals(encrypted))
        assertTrue(encrypted.copyOfRange(0, 4).contentEquals("SBB1".toByteArray()))
        assertArrayEquals(original, EncryptedBackup.decrypt(encrypted, "contoh-password-kuat-123"))
    }

    @Test(expected = ApiFailure::class)
    fun wrongPasswordIsRejected() {
        val encrypted = EncryptedBackup.encrypt("data-dummy".toByteArray(), "password-untuk-test")
        EncryptedBackup.decrypt(encrypted, "password-salah-123")
    }

    @Test(expected = ApiFailure::class)
    fun tamperedBackupIsRejected() {
        val encrypted = EncryptedBackup.encrypt("data-dummy".toByteArray(), "password-untuk-test")
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        EncryptedBackup.decrypt(encrypted, "password-untuk-test")
    }

    @Test(expected = ApiFailure::class)
    fun invalidHeaderIsRejected() {
        EncryptedBackup.decrypt("bukan-backup".toByteArray(), "password-untuk-test")
    }
}
