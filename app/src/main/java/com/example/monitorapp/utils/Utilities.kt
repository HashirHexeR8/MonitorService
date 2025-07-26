package com.example.monitorapp.utils

import java.util.UUID

object Utilities {

    fun generateUniqueDeviceID(): String {
        return UUID.randomUUID().toString()
    }
}