package com.example.monitorapp.utils

enum class AppActionEnum(val value: String) {
    appActionHome("performHomeAction"),
    appActionBack("performBackAction"),
    appActionRecents("performRecentsAction"),
    appActionLockScreen("performLockScreenAction");

    companion object {
        fun fromValue(value: String): AppActionEnum? {
            return AppActionEnum.entries.find { it.value == value }
        }
    }
}