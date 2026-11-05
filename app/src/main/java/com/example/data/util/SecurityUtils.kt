package com.example.data.util

import java.security.MessageDigest
import kotlin.random.Random

object SecurityUtils {
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        return hashPassword(password) == storedHash
    }

    fun generateSixDigitId(): String {
        val number = Random.nextInt(100000, 999999)
        return number.toString()
    }
}
