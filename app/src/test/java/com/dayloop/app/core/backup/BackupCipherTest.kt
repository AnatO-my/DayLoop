package com.dayloop.app.core.backup

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import javax.crypto.AEADBadTagException

class BackupCipherTest {
    private val password = "a long test password".toCharArray()

    @Test fun roundTripUsesFreshRandomness() {
        val first = BackupCipher.encrypt("wardrobe", password)
        val second = BackupCipher.encrypt("wardrobe", password)
        assertNotEquals(first, second)
        assertEquals("wardrobe", BackupCipher.decrypt(first, password))
    }

    @Test fun wrongPasswordFailsAuthentication() {
        val file = BackupCipher.encrypt("wardrobe", password)
        assertThrows(AEADBadTagException::class.java) {
            BackupCipher.decrypt(file, "another long password".toCharArray())
        }
    }

    @Test fun alteredCiphertextFailsAuthentication() {
        val file = BackupCipher.encrypt("wardrobe", password)
        val header = file.substringBefore(':') + ":"
        val bytes = Base64.getDecoder().decode(file.substringAfter(':'))
        bytes[28] = (bytes[28].toInt() xor 1).toByte()
        assertThrows(AEADBadTagException::class.java) {
            BackupCipher.decrypt(header + Base64.getEncoder().encodeToString(bytes), password)
        }
    }

    @Test fun legacyAndOversizedFilesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { BackupCipher.decrypt("{\"salt\":\"old\"}", password) }
        assertThrows(IllegalArgumentException::class.java) {
            BackupCipher.decrypt("DAYLOOP-WARDROBE-1:" + "A".repeat(BackupCipher.MAX_FILE_BYTES), password)
        }
        assertThrows(IllegalArgumentException::class.java) { BackupCipher.encrypt("data", "short".toCharArray()) }
    }
}
