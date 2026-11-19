package snd.sketchpilot.core.crypto

import java.io.File
import java.io.FileInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Verified Sketchware Pro file crypto (E6/E8): AES/CBC/PKCS5Padding,
 * key and IV are the same 16-byte literal "sketchwaresecure".
 */
class SketchwareCrypto(private val keyLiteral: String = KEY_LITERAL) {
    private val key: ByteArray = keyLiteral.toByteArray(Charsets.UTF_8).also {
        require(it.size == 16) { "AES-128 requires a 16-byte key/IV" }
    }

    fun encrypt(plain: String): ByteArray = cipher(Cipher.ENCRYPT_MODE).doFinal(plain.toByteArray(Charsets.UTF_8))

    fun decrypt(cipherText: ByteArray): String = String(cipher(Cipher.DECRYPT_MODE).doFinal(cipherText), Charsets.UTF_8)

    fun looksEncrypted(data: ByteArray): Boolean {
        if (data.isEmpty() || data.size % 16 != 0) return false
        return try {
            val text = decrypt(data)
            text.contains("@") || text.startsWith("{") || text.startsWith("[")
        } catch (t: Throwable) {
            false
        }
    }

    private fun cipher(mode: Int): Cipher {
        val result = Cipher.getInstance("AES/CBC/PKCS5Padding")
        result.init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(key))
        return result
    }

    companion object {
        const val KEY_LITERAL = "sketchwaresecure"
        fun readAll(file: File): ByteArray = FileInputStream(file).use { it.readBytes() }
    }
}
