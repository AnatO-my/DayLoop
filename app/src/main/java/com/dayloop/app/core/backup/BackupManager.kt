package com.dayloop.app.core.backup

import android.content.Context
import android.util.Base64
import com.dayloop.app.di.AppGraph
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupManager(private val graph: AppGraph) {

    suspend fun createBackup(password: String): String {
        val data = JSONObject().apply {
            put("version", 1)
            put("timestamp", System.currentTimeMillis())
            
            // For now, let's just backup high-level entities as a proof of concept
            // In a real app, you'd iterate through all tables
            put("settings", graph.settingsRepository.settings.first().let { settings ->
                JSONObject().apply {
                    put("aiAssistantEnabled", settings.aiAssistantEnabled)
                    // ... other settings
                }
            })
            
            // Backup tasks
            val tasks = graph.taskRepository.observeTasks().first()
            put("tasks_count", tasks.size)
            // ... full task serialization would go here
        }

        return encrypt(data.toString(), password)
    }

    suspend fun restoreBackup(encryptedData: String, password: String) {
        val decrypted = decrypt(encryptedData, password)
        val json = JSONObject(decrypted)
        
        // Restore logic here
        val settings = json.optJSONObject("settings")
        if (settings != null) {
            graph.settingsRepository.setAiAssistantEnabled(settings.getBoolean("aiAssistantEnabled"))
        }
    }

    private fun encrypt(data: String, password: String): String {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        
        val encrypted = cipher.doFinal(data.toByteArray())
        
        val result = JSONObject().apply {
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }
        
        return result.toString()
    }

    private fun decrypt(jsonStr: String, password: String): String {
        val json = JSONObject(jsonStr)
        val salt = Base64.decode(json.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(json.getString("iv"), Base64.NO_WRAP)
        val encrypted = Base64.decode(json.getString("data"), Base64.NO_WRAP)
        
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        
        return String(cipher.doFinal(encrypted))
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}
