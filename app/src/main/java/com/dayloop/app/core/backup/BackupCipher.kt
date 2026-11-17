package com.dayloop.app.core.backup

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Fixed, versioned parameters prevent an imported file from choosing excessive KDF work. */
internal object BackupCipher {
    const val MAX_FILE_BYTES = 8 * 1024 * 1024
    private const val HEADER = "DAYLOOP-WARDROBE-1:"
    private const val ITERATIONS = 600_000

    fun encrypt(plainText: String, password: CharArray): String {
        require(password.size in 12..256) { "Use a password of 12 to 256 characters." }
        val plain = plainText.toByteArray(Charsets.UTF_8)
        require(plain.size <= 5 * 1024 * 1024) { "Wardrobe export is too large." }
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(HEADER.toByteArray(Charsets.US_ASCII))
        return try {
            HEADER + Base64.getEncoder().encodeToString(salt + nonce + cipher.doFinal(plain))
        } finally {
            plain.fill(0)
        }
    }

    fun decrypt(file: String, password: CharArray): String {
        require(password.size in 12..256) { "Invalid password length." }
        require(file.length <= MAX_FILE_BYTES && file.startsWith(HEADER)) {
            "Unsupported wardrobe file. Legacy backups cannot be imported."
        }
        val bytes = Base64.getDecoder().decode(file.substring(HEADER.length))
        require(bytes.size >= 44) { "Incomplete wardrobe file." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, bytes.copyOfRange(0, 16)),
            GCMParameterSpec(128, bytes.copyOfRange(16, 28)))
        cipher.updateAAD(HEADER.toByteArray(Charsets.US_ASCII))
        val plain = cipher.doFinal(bytes, 28, bytes.size - 28)
        return try { plain.toString(Charsets.UTF_8) } finally { plain.fill(0) }
    }

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try {
            val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            try { SecretKeySpec(encoded, "AES") } finally { encoded.fill(0) }
        } finally { spec.clearPassword() }
    }
}
