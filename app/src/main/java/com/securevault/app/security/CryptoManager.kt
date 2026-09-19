package com.securevault.app.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * مدير التشفير - يستعمل AES-256-GCM مع PBKDF2
 * هذا هو المعيار اللي تستعملو التطبيقات الاحترافية
 */
object CryptoManager {

    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH = 128
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGO = "PBKDF2WithHmacSHA256"

    /**
     * توليد Salt عشوائي آمن
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * اشتقاق مفتاح من كلمة السر الرئيسية (Master Password)
     */
    fun deriveKey(masterPassword: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(masterPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_ALGO)
        return factory.generateSecret(spec).encoded
    }

    /**
     * تشفير نص
     */
    fun encrypt(plainText: String, key: ByteArray): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), spec)
        
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // نجمعو: IV + Ciphertext
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * فك التشفير
     */
    fun decrypt(encryptedData: String, key: ByteArray): String {
        val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val cipherText = combined.copyOfRange(IV_LENGTH, combined.size)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), spec)
        
        val decrypted = cipher.doFinal(cipherText)
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * توليد كلمة سر قوية عشوائية
     */
    fun generatePassword(
        length: Int = 20,
        useUpper: Boolean = true,
        useLower: Boolean = true,
        useDigits: Boolean = true,
        useSymbols: Boolean = true
    ): String {
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val symbols = "!@#\$%^&*()_+-=[]{}|;:,.<>?"
        
        val pool = buildString {
            if (useUpper) append(upper)
            if (useLower) append(lower)
            if (useDigits) append(digits)
            if (useSymbols) append(symbols)
        }
        
        if (pool.isEmpty()) return ""
        
        val random = SecureRandom()
        return (1..length).map { pool[random.nextInt(pool.length)] }.joinToString("")
    }

    /**
     * حساب قوة كلمة السر
     */
    fun getPasswordStrength(password: String): Int {
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.length >= 16) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return score.coerceIn(0, 7)
    }
}
