package com.example.monitorapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.graphics.Color
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.material3.Snackbar
import androidx.core.content.ContextCompat
import com.example.monitorapp.databinding.ActivityMainBinding
import com.example.monitorapp.network.DeviceNetworkService
import com.example.monitorapp.services.ForegroundService
import com.example.monitorapp.services.RemoteAccessibilityService
import com.example.monitorapp.utils.SharedPrefsHelper
import com.example.monitorapp.utils.Utilities
import com.example.monitorapp.network.SocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dispatcher

class MainActivity : Activity(), SocketManager.ConnectionListener {

    private lateinit var viewBinding: ActivityMainBinding

    enum class StatusType {
        ACCESSIBILITY, BATTERY, SERVICE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        showPermissionDialog()

        viewBinding.btnAccessService.setOnClickListener {
            promptAccessibilitySettings(this@MainActivity)
        }

        viewBinding.btnBatteryPermission.setOnClickListener {
            launchBatteryOptimizationPermission()
        }

        viewBinding.btnStartBGService.setOnClickListener {
            val serviceIntent = Intent(this, ForegroundService::class.java)
            startForegroundService(serviceIntent)
        }

        viewBinding.btnTest.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = DeviceNetworkService.registerDevice()
                    Log.d("MainActivity", "Response from server: $response")
                    Toast.makeText(this@MainActivity, "$response", Toast.LENGTH_LONG).show()
                    if (response.statusCode == 200) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "$response", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                catch (exception: Exception) {
                    Log.e("MainActivity", "Error during network call", exception)
                }
            }
            SocketManager.connect(this)
            updateSocketStatusUI()
            android.widget.Toast.makeText(this, "Connecting to server...", android.widget.Toast.LENGTH_SHORT).show()
        }

        viewBinding.btnGetDeviceID.setOnClickListener {
            val deviceID = Utilities.generateShortId()
            SharedPrefsHelper.saveDeviceId(this@MainActivity, deviceId = deviceID)
            "Your unique device ID is: $deviceID. Enter this ID on the other device to register and start the connection.".also { viewBinding.tvDeviceIDInfo.text = it }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAllStatuses()
        SocketManager.addConnectionListener(this)
        updateSocketStatusUI()
    }

    override fun onPause() {
        super.onPause()
        SocketManager.removeConnectionListener(this)
    }

    private fun refreshAllStatuses() {
        updateStatus(StatusType.ACCESSIBILITY, isAccessibilityServiceEnabled())
        updateStatus(StatusType.BATTERY, isIgnoringBatteryOptimizations())
        updateStatus(StatusType.SERVICE, ForegroundService.isRunning)
        val deviceID = SharedPrefsHelper.getDeviceId(this@MainActivity)
        if (!deviceID.isNullOrEmpty()) {
            viewBinding.btnGetDeviceID.isEnabled = false
            "Your unique device ID is: $deviceID. Enter this ID on the other device to register and start the connection.".also { viewBinding.tvDeviceIDInfo.text = it }
        }
        else {
            viewBinding.btnGetDeviceID.isEnabled = true
            viewBinding.tvDeviceIDInfo.text = ""
        }
    }

    private fun showPermissionDialog() {
        val dialogBuilder = AlertDialog.Builder(this@MainActivity)
        dialogBuilder.setTitle("Permissions Required")
        dialogBuilder.setMessage("In order for the app to work, please allow the required permissions.")
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            launchBatteryOptimizationPermission()
        }
        dialogBuilder.show()
    }

    private fun launchBatteryOptimizationPermission() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    fun promptAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(this, RemoteAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(expectedComponent.flattenToString()) == true
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun updateStatus(type: StatusType, enabled: Boolean) {
        val icon: ImageView = when (type) {
            StatusType.ACCESSIBILITY -> viewBinding.statusIconAccessibility
            StatusType.BATTERY -> viewBinding.statusIconBattery
            StatusType.SERVICE -> viewBinding.statusIconService
        }

        if (enabled) {
            icon.setImageResource(android.R.drawable.checkbox_on_background)
            icon.setColorFilter(Color.parseColor("#4CAF50"))
        } else {
            icon.setImageResource(android.R.drawable.checkbox_off_background)
            icon.setColorFilter(Color.GRAY)
        }

        enableDeviceIdInfo()
    }

    private fun enableDeviceIdInfo() {
        viewBinding.btnGetDeviceID.isEnabled = isAccessibilityServiceEnabled() && isIgnoringBatteryOptimizations() && ForegroundService.isRunning
    }

    private fun updateSocketStatusUI() {
        val isConnected = SocketManager.isConnected()
        viewBinding.socketStatusText.text = if (isConnected) "Socket: Connected" else "Socket: Disconnected"
        viewBinding.socketStatusIcon.setImageResource(
            if (isConnected) android.R.drawable.presence_online else android.R.drawable.presence_offline
        )
        viewBinding.socketStatusIcon.setColorFilter(
            if (isConnected) Color.parseColor("#4CAF50") else Color.GRAY
        )
    }

    override fun onConnectionStatusChanged(isConnected: Boolean) {
        runOnUiThread { updateSocketStatusUI() }
    }
}
