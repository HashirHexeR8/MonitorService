package com.example.monitorapp.utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

object Utilities {
    fun generateShortId(length: Int = 8): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    fun generateClientSecretKey(serverSecretKey: String): String {
        return try {
            val salt = "G7tkQ2mP1!zW9bX"
            val combinedString = serverSecretKey + salt

            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(combinedString.toByteArray(StandardCharsets.UTF_8))

            // Convert to hex string and take first 16 characters
            val hexString = hashBytes.joinToString("") { "%02x".format(it) }
            hexString.substring(0, 16)

        } catch (e: Exception) {
            throw RuntimeException("Failed to generate secret key", e)
        }
    }
}