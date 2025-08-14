package com.example.monitorapp.utils

import android.content.Context
import android.content.SharedPreferences

object SharedPrefsHelper {
    private const val PREFS_NAME = "monitor_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_DEVICE_NAME = "device_name"
    private const val KEY_IS_REGISTERED = "is_registered"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveDeviceId(context: Context, deviceId: String) {
        getPrefs(context).edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    fun getDeviceId(context: Context): String? {
        return getPrefs(context).getString(KEY_DEVICE_ID, null)
    }

    fun clearDeviceId(context: Context) {
        getPrefs(context).edit().remove(KEY_DEVICE_ID).apply()
    }

    fun saveDeviceName(context: Context, deviceName: String) {
        getPrefs(context).edit().putString(KEY_DEVICE_NAME, deviceName).apply()
    }

    fun getDeviceName(context: Context): String? {
        return getPrefs(context).getString(KEY_DEVICE_NAME, null)
    }

    fun setDeviceRegistered(context: Context, isRegistered: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_REGISTERED, isRegistered).apply()
    }

    fun isDeviceRegistered(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_REGISTERED, false)
    }

    fun clearRegistrationData(context: Context) {
        getPrefs(context).edit().apply {
            remove(KEY_DEVICE_ID)
            remove(KEY_DEVICE_NAME)
            remove(KEY_IS_REGISTERED)
        }.apply()
    }
} 