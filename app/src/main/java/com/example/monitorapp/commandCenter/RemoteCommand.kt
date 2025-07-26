package com.example.monitorapp.commandCenter

import com.example.monitorapp.utils.AppActionEnum

sealed class RemoteCommand {
    abstract val type: String

    data class Tap(val x: Float, val y: Float) : RemoteCommand() {
        override val type = "tap"
    }

    data class Swipe(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val duration: Long = 300
    ) : RemoteCommand() {
        override val type = "swipe"
    }

    data class InputText(val text: String) : RemoteCommand() {
        override val type = "input_text"
    }

    data class AppGlobalActions(val appAction: AppActionEnum) : RemoteCommand() {
        override val type = "launch_app_drawer"
    }
}
