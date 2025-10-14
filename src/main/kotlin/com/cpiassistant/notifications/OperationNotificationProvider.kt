package com.cpiassistant.notifications

import com.cpiassistant.operations.OperationManager
import com.cpiassistant.operations.OperationPhase
import com.cpiassistant.operations.OperationStatus
import com.cpiassistant.operations.OperationTask
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Generic notification provider for all operation types
 */
@Service(Service.Level.PROJECT)
class OperationNotificationProvider(private val project: Project) {

    private val operationManager = project.service<OperationManager>()
    private val activeNotifications = mutableMapOf<String, Notification>()

    fun <T : OperationPhase> showOperationCompleted(task: OperationTask<T>) {
        ApplicationManager.getApplication().invokeLater {
            // Remove any active notification for this task
            activeNotifications[task.taskId]?.expire()
            activeNotifications.remove(task.taskId)

            val notificationType = when (task.status) {
                OperationStatus.SUCCESS -> NotificationType.INFORMATION
                OperationStatus.FAILED -> NotificationType.ERROR
                OperationStatus.CANCELLED -> NotificationType.WARNING
                else -> NotificationType.INFORMATION
            }

            val title = when (task.status) {
                OperationStatus.SUCCESS -> "${task.operationType} Successful"
                OperationStatus.FAILED -> "${task.operationType} Failed"
                OperationStatus.CANCELLED -> "${task.operationType} Cancelled"
                else -> "${task.operationType} Status"
            }

            val message = when (task.status) {
                OperationStatus.FAILED -> buildErrorMessage(task)
                else -> buildSuccessMessage(task)
            }

            val notification = Notification(
                "Custom Notification Group",
                title,
                message,
                notificationType
            )

            // Set longer display time for errors
            if (task.status == OperationStatus.FAILED) {
                notification.setImportant(true)
            }

            Notifications.Bus.notify(notification, project)
        }
    }

    private fun <T : OperationPhase> buildSuccessMessage(task: OperationTask<T>): String {
        return buildString {
            append(task.targetName)
            if (task.tenantName != "Unknown Tenant") {
                append(" → ${task.tenantName}")
            }
            task.duration?.let {
                append(" (${it.seconds}s)")
            }
        }
    }

    private fun <T : OperationPhase> buildErrorMessage(task: OperationTask<T>): String {
        return buildString {
            // First line: target and tenant
            append(task.targetName)
            if (task.tenantName != "Unknown Tenant") {
                append(" → ${task.tenantName}")
            }
            task.duration?.let {
                append(" (${it.seconds}s)")
            }

            // Error message
            if (task.errorMessage != null) {
                append("\nError: ${task.errorMessage}")
            }

            // Recent logs (last 2-3 entries, excluding verbose details)
            val relevantLogs = task.logs.takeLast(3).filter { log ->
                log.level != com.cpiassistant.operations.LogLevel.INFO ||
                log.message.contains("initiated") ||
                log.message.contains("failed") ||
                log.message.contains("error", ignoreCase = true) ||
                log.message.contains("timeout", ignoreCase = true)
            }

            if (relevantLogs.isNotEmpty()) {
                append("\n\nRecent activity:")
                relevantLogs.forEach { log ->
                    append("\n• ${log.message}")
                }
            }
        }
    }

    companion object {
        fun getInstance(project: Project): OperationNotificationProvider {
            return project.service<OperationNotificationProvider>()
        }
    }
}
