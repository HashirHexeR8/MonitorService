package com.example.monitorapp.model

data class DeviceRegistrationRequest (
    val userDeviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val clientSecretKey: String
)