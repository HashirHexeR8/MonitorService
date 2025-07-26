package com.example.monitorapp.utils

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.URISyntaxException
import java.util.concurrent.Executors
import com.google.gson.Gson

object SocketManager {
    private const val TAG = "SocketManager"
    private val SERVER_HOST = NetworkConstants.SOCKET_HOST
    @Volatile private var socket: Socket? = null
    @Volatile private var isConnected = false
    private val executor = Executors.newSingleThreadExecutor()

    private const val ACTION_COMMAND = "com.example.monitorapp.ACTION_COMMAND"

    interface ConnectionListener {
        fun onConnectionStatusChanged(isConnected: Boolean)
    }

    private val listeners = mutableSetOf<WeakReference<ConnectionListener>>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun addConnectionListener(listener: ConnectionListener) {
        synchronized(listeners) {
            listeners.add(WeakReference(listener))
            Log.d(TAG, "Listener added: $listener. Total listeners: ${listeners.size}")
        }
    }

    fun removeConnectionListener(listener: ConnectionListener) {
        synchronized(listeners) {
            listeners.removeAll { it.get() == null || it.get() == listener }
            Log.d(TAG, "Listener removed: $listener. Total listeners: ${listeners.size}")
        }
    }

    private fun notifyConnectionStatusChanged() {
        val isConnectedNow = isConnected
        val toRemove = mutableListOf<WeakReference<ConnectionListener>>()
        synchronized(listeners) {
            listeners.forEach { ref ->
                val l = ref.get()
                if (l != null) {
                    mainHandler.post { l.onConnectionStatusChanged(isConnectedNow) }
                } else {
                    toRemove.add(ref)
                }
            }
            listeners.removeAll(toRemove)
        }
    }

    fun connect(context: Context) {
        if (isConnected) {
            Log.d(TAG, "Already connected. Skipping connect().")
            return
        }
        val deviceId = SharedPrefsHelper.getDeviceId(context)
        if (deviceId.isNullOrEmpty()) {
            Log.w(TAG, "Device ID is null or empty. Cannot connect to socket.")
            return
        }
        val appContext = context.applicationContext
        executor.execute {
            try {
                val url = SERVER_HOST
                val opts = IO.Options()
                opts.query = "deviceId=$deviceId"
                Log.d(TAG, "Connecting to $url with query: deviceId=$deviceId")
                socket = IO.socket(url, opts)
                socket?.on(Socket.EVENT_CONNECT) {
                    isConnected = true
                    Log.i(TAG, "Socket.IO connected to $url with deviceId=$deviceId")
                    notifyConnectionStatusChanged()
                }
                socket?.on(Socket.EVENT_DISCONNECT) {
                    isConnected = false
                    Log.i(TAG, "Socket.IO disconnected from $url")
                    notifyConnectionStatusChanged()
                }
                socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                    isConnected = false
                    Log.e(TAG, "Socket.IO connection error: ${args.joinToString()}")
                    notifyConnectionStatusChanged()
                }
                // Listen for 'command' events from server
                socket?.on("receiveCommand") { args ->
                    if (args.isNotEmpty()) {
                        val commandJson = args[0]?.toString()
                        Log.i(TAG, "Received command from socket: $commandJson")
                        if (!commandJson.isNullOrEmpty()) {
                            val intent = Intent(ACTION_COMMAND)
                            intent.putExtra("command_json", commandJson)
                            appContext.sendBroadcast(intent)
                        }
                    }
                }
                socket?.connect()
            } catch (e: URISyntaxException) {
                Log.e(TAG, "Socket.IO URI syntax error", e)
            } catch (e: Exception) {
                Log.e(TAG, "Socket.IO connection error", e)
            }
        }
    }

    fun send(event: String, data: JSONObject) {
        executor.execute {
            try {
                Log.d(TAG, "Emitting event '$event' with data: $data")
                socket?.emit(event, data)
            } catch (e: Exception) {
                Log.e(TAG, "Socket.IO send error for event '$event'", e)
            }
        }
    }

    fun disconnect() {
        executor.execute {
            try {
                Log.i(TAG, "Disconnecting Socket.IO and cleaning up resources.")
                socket?.disconnect()
                socket?.off()
                socket = null
            } catch (e: Exception) {
                Log.e(TAG, "Socket.IO disconnect error", e)
            } finally {
                isConnected = false
                Log.i(TAG, "Socket.IO disconnected (disconnect() called)")
                notifyConnectionStatusChanged()
            }
        }
    }

    fun isConnected(): Boolean {
        return isConnected
    }
} 