package com.example.monitorapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.monitorapp.services.ForegroundService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device reboot detected, starting service...")
            val serviceIntent = Intent(context, ForegroundService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
