package com.example.monitorapp.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.monitorapp.MainActivity
import com.example.monitorapp.R
import com.example.monitorapp.utils.SocketManager

class ForegroundService : Service() {

    companion object {
        @Volatile
        var isRunning: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
        isRunning = true
        Log.i("ForegroundService", "Service Started")
        SocketManager.connect(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.i("ForegroundService", "Service Stopped")
        SocketManager.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "MyServiceChannel")
            .setContentTitle("Service Running")
            .setContentText("Background service is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // Replace with your app icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "MyServiceChannel",
            "Background Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
