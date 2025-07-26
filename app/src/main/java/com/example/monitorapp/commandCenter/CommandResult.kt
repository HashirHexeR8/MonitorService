package com.example.monitorapp.commandCenter

data class CommandResult(
    val success: Boolean,
    val message: String,
    val commandType: String,
    val timestamp: Long = System.currentTimeMillis()
)