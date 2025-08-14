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
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.monitorapp.databinding.ActivityMainBinding
import com.example.monitorapp.network.DeviceNetworkService
import com.example.monitorapp.services.ForegroundService
import com.example.monitorapp.services.RemoteAccessibilityService
import com.example.monitorapp.utils.SharedPrefsHelper
import com.example.monitorapp.utils.Utilities
import com.example.monitorapp.utils.NetworkUtils
import com.example.monitorapp.network.SocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : Activity(), SocketManager.ConnectionListener {

    private lateinit var viewBinding: ActivityMainBinding

    enum class StatusType {
        ACCESSIBILITY, BATTERY, SERVICE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Check registration status first
        if (!SharedPrefsHelper.isDeviceRegistered(this)) {
            showRegistrationFlow()
        } else {
            // Device is registered, proceed with normal app flow
            initializeApp()
        }
    }

    private fun showRegistrationFlow() {
        // Check network connectivity first
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNetworkErrorDialog()
            return
        }

        // Show device name input dialog
        showDeviceNameDialog()
    }

    private fun showNetworkErrorDialog() {
        AlertDialog.Builder(this)
            .setTitle("Network Required")
            .setMessage("Please connect to the internet to register your device.")
            .setPositiveButton("Retry") { _, _ ->
                showRegistrationFlow()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showDeviceNameDialog() {
        val input = EditText(this)
        input.hint = "Enter device name"

        AlertDialog.Builder(this)
            .setTitle("Device Registration")
            .setMessage("Please enter a name for this device:")
            .setView(input)
            .setPositiveButton("Register") { _, _ ->
                val deviceName = input.text.toString().trim()
                if (deviceName.isNotEmpty()) {
                    SharedPrefsHelper.saveDeviceName(this, deviceName)
                    registerDeviceWithServer()
                } else {
                    Toast.makeText(this, "Please enter a device name", Toast.LENGTH_SHORT).show()
                    showDeviceNameDialog()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun registerDeviceWithServer() {
        // Show loading state
        viewBinding.root.alpha = 0.5f
        Toast.makeText(this, "Registering device...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = DeviceNetworkService.registerDevice(this@MainActivity)
                
                runOnUiThread {
                    viewBinding.root.alpha = 1.0f
                    
                    if (response.statusCode == 200) {
                        // Registration successful
                        SharedPrefsHelper.setDeviceRegistered(this@MainActivity, true)
                        Toast.makeText(this@MainActivity, "Device registered successfully!", Toast.LENGTH_LONG).show()
                        initializeApp()
                    } else {
                        // Registration failed
                        Toast.makeText(this@MainActivity, "Registration failed: ${response}", Toast.LENGTH_LONG).show()
                        showRegistrationFlow()
                    }
                }
            } catch (exception: Exception) {
                runOnUiThread {
                    viewBinding.root.alpha = 1.0f
                    Log.e("MainActivity", "Registration error", exception)
                    Toast.makeText(this@MainActivity, "Registration failed: ${exception.message}", Toast.LENGTH_LONG).show()
                    showRegistrationFlow()
                }
            }
        }
    }

    private fun initializeApp() {
        // Request permissions
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        showPermissionDialog()

        // Set up UI
        setupUI()
        updateRegistrationStatusUI()
        
        // Check if all requirements are met for socket connection
        if (areAllRequirementsMet()) {
            connectToSocket()
        }
    }

    private fun setupUI() {
        viewBinding.btnAccessService.setOnClickListener {
            promptAccessibilitySettings(this@MainActivity)
        }

        viewBinding.btnBatteryPermission.setOnClickListener {
            launchBatteryOptimizationPermission()
        }

        viewBinding.btnStartBGService.setOnClickListener {
            if (areAllRequirementsMet()) {
                val serviceIntent = Intent(this, ForegroundService::class.java)
                startForegroundService(serviceIntent)
                connectToSocket()
            } else {
                Toast.makeText(this, "Please enable all required permissions first", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun areAllRequirementsMet(): Boolean {
        return isAccessibilityServiceEnabled() && 
               isIgnoringBatteryOptimizations() && 
               NetworkUtils.isNetworkAvailable(this)
    }

    private fun connectToSocket() {
        if (areAllRequirementsMet()) {
            SocketManager.connect(this)
            updateSocketStatusUI()
        }
    }

    override fun onResume() {
        super.onResume()
        if (SharedPrefsHelper.isDeviceRegistered(this)) {
            refreshAllStatuses()
            SocketManager.addConnectionListener(this)
            updateSocketStatusUI()
        }
        updateRegistrationStatusUI()
    }

    override fun onPause() {
        super.onPause()
        SocketManager.removeConnectionListener(this)
    }

    private fun refreshAllStatuses() {
        updateStatus(StatusType.ACCESSIBILITY, isAccessibilityServiceEnabled())
        updateStatus(StatusType.BATTERY, isIgnoringBatteryOptimizations())
        updateStatus(StatusType.SERVICE, ForegroundService.isRunning)
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

    private fun updateRegistrationStatusUI() {
        val isRegistered = SharedPrefsHelper.isDeviceRegistered(this)
        viewBinding.registrationStatusText.text = if (isRegistered) "Registration: Complete" else "Registration: Pending"
        viewBinding.registrationStatusIcon.setImageResource(
            if (isRegistered) android.R.drawable.presence_online else android.R.drawable.presence_offline
        )
        viewBinding.registrationStatusIcon.setColorFilter(
            if (isRegistered) Color.parseColor("#4CAF50") else Color.GRAY
        )
    }

    override fun onConnectionStatusChanged(isConnected: Boolean) {
        runOnUiThread { updateSocketStatusUI() }
    }
}
