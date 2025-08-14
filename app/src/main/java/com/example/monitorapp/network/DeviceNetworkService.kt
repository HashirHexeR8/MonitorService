package com.example.monitorapp.network

import android.content.Context
import android.os.Build
import com.example.monitorapp.model.DeviceRegistrationRequest
import com.example.monitorapp.model.NetworkResponseDTO
import com.example.monitorapp.utils.SharedPrefsHelper
import com.example.monitorapp.utils.Utilities

object DeviceNetworkService {

    suspend fun registerDevice(context: Context): NetworkResponseDTO<String> {
        val deviceName = SharedPrefsHelper.getDeviceName(context) ?: "Unknown"
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val deviceId = Utilities.generateShortId()

        // Same algorithm as server for secret key
        val serverSecret = "pQ8!xR5zL2@vN7" // stored securely in your build config or fetched from Keystore
        val secretKey = Utilities.generateClientSecretKey(serverSecret)

        val request = DeviceRegistrationRequest(
            userDeviceId = deviceId,
            deviceName = deviceName,
            deviceModel = deviceModel,
            clientSecretKey = secretKey
        )

        val response = NetworkClient.post(
            url = "${NetworkConstants.BASE_URL}${NetworkConstants.REGISTER_DEVICE_ENDPOINT}",
            bodyObj = request
        ) as NetworkResponseDTO<String>?
        if (response?.statusCode == 200) {
            SharedPrefsHelper.saveDeviceId(context, deviceId)
        }
        else {
            return NetworkResponseDTO(404, "Request failed", null)
        }
        // Call your API
        return response
    }

}