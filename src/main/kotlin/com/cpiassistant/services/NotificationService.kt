package com.cpiassistant.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class NotificationService {

    companion object {
        private const val GROUP_ID = "Custom Notification Group"

        fun getInstance(): NotificationService {
            return com.intellij.openapi.application.ApplicationManager.getApplication().getService(NotificationService::class.java)
        }
    }

    fun showSuccess(title: String, message: String = "") {
        Notifications.Bus.notify(
            Notification(GROUP_ID, title, message, NotificationType.INFORMATION)
        )
    }

    fun showError(title: String, message: String = "") {
        Notifications.Bus.notify(
            Notification(GROUP_ID, title, message, NotificationType.ERROR)
        )
    }

    fun showWarning(title: String, message: String = "") {
        Notifications.Bus.notify(
            Notification(GROUP_ID, title, message, NotificationType.WARNING)
        )
    }

    fun showInfo(title: String, message: String = "") {
        Notifications.Bus.notify(
            Notification(GROUP_ID, title, message, NotificationType.INFORMATION)
        )
    }
}