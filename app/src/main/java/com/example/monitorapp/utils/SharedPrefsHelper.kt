package com.example.monitorapp.utils

import android.content.Context
import android.content.SharedPreferences

object SharedPrefsHelper {
    private const val PREFS_NAME = "monitor_prefs"
    private const val KEY_DEVICE_ID = "device_id"

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
} 