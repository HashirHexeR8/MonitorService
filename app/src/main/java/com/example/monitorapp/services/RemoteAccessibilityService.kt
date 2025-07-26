package com.example.monitorapp.services

import RemoteCommandAdapter
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.monitorapp.commandCenter.CommandResult
import com.example.monitorapp.commandCenter.RemoteCommand
import com.example.monitorapp.utils.AppActionEnum
import com.google.gson.Gson
import com.google.gson.GsonBuilder


class RemoteAccessibilityService : AccessibilityService() {

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra("command_json")?.let { json ->
                val command = parseRemoteCommand(json)
                executeRemoteCommand(command)
            }
        }
    }

    private val commandResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra("command_result_json")?.let { json ->
                val result = Gson().fromJson(json, CommandResult::class.java)
                Log.i("CommandResult", "Command: ${result.commandType}, Success: ${result.success}, Message: ${result.message}")
                // Optionally update UI or log list
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("RemoteService", "Accessibility Service Connected")
        val filter = IntentFilter("com.example.monitorapp.ACTION_COMMAND")
        registerReceiver(commandReceiver, filter, RECEIVER_EXPORTED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Optional: You can handle events if needed
    }

    override fun onInterrupt() {
        Log.i("RemoteService", "Accessibility Service Interrupted")
    }

    fun parseRemoteCommand(json: String): RemoteCommand {
        val gson = GsonBuilder()
            .registerTypeAdapter(RemoteCommand::class.java, RemoteCommandAdapter())
            .create()

        return gson.fromJson(json, RemoteCommand::class.java)
    }

    fun executeRemoteCommand(command: RemoteCommand): Boolean {
        return when (command) {
            is RemoteCommand.Tap -> dispatchTapGesture(command.x, command.y)
            is RemoteCommand.Swipe -> dispatchSwipeGesture(command.startX, command.startY, command.endX, command.endY, command.duration)
            is RemoteCommand.InputText -> performTextInput(command.text)
            is RemoteCommand.AppGlobalActions -> performGlobalActions(command)
            else -> false
        }
    }

    fun performGlobalActions(action: RemoteCommand.AppGlobalActions): Boolean {
        when (action.appAction) {
            AppActionEnum.appActionHome -> performGlobalAction(GLOBAL_ACTION_HOME)
            AppActionEnum.appActionBack -> performGlobalAction(GLOBAL_ACTION_BACK)
            AppActionEnum.appActionRecents -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            AppActionEnum.appActionLockScreen -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
        return true
    }

    fun dispatchTapGesture(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        return dispatchGesture(gesture, object: AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i("RemoteService", "Tap gesture completed at ($x, $y)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w("RemoteService", "Tap gesture cancelled at ($x, $y)")
            }
        }, null)
    }

    fun dispatchSwipeGesture(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        return dispatchGesture(gesture, object: AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i("RemoteService", "Swipe gesture completed from ($startX, $startY) to ($endX, $endY)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w("RemoteService", "Swipe gesture cancelled from ($startX, $startY) to ($endX, $endY)")
            }
        }, null)
    }

    fun performTextInput(textInput: String): Boolean {
        try {
            val rootNode = rootInActiveWindow
            val editTextNode = findFirstEditableNode(rootNode)

            if (editTextNode != null) {
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textInput)
                val success = editTextNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                Log.i("AccessibilityService", "Set text success: $success")
            } else {
                throw Exception("Editable field not found")
            }

        } catch (exception: Exception) {
            Log.i("AccessibilityService", "Unable to perform set text action, Copying to clipboard instead ${exception.message}")
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", textInput)
            clipboard.setPrimaryClip(clip)
        }

        return true
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable && node.className == "android.widget.EditText") return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findFirstEditableNode(child)
            if (result != null) return result
        }
        return null
    }
}
