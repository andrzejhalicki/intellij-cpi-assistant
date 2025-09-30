package com.cpiassistant.notifications

import com.cpiassistant.deployment.DeploymentManager
import com.cpiassistant.deployment.DeploymentStatus
import com.cpiassistant.deployment.DeploymentTask
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class DeploymentNotificationProvider(private val project: Project) {

    private val deploymentManager = project.service<DeploymentManager>()
    private val activeNotifications = mutableMapOf<String, Notification>()

    fun showDeploymentCompleted(task: DeploymentTask) {
        ApplicationManager.getApplication().invokeLater {
            // Remove any active notification for this task
            activeNotifications[task.taskId]?.expire()
            activeNotifications.remove(task.taskId)

            val notificationType = when (task.status) {
                DeploymentStatus.SUCCESS -> NotificationType.INFORMATION
                DeploymentStatus.FAILED -> NotificationType.ERROR
                DeploymentStatus.CANCELLED -> NotificationType.WARNING
                else -> NotificationType.INFORMATION
            }

            val title = when (task.status) {
                DeploymentStatus.SUCCESS -> "✅ Deployment Successful"
                DeploymentStatus.FAILED -> "❌ Deployment Failed"
                DeploymentStatus.CANCELLED -> "⚠️ Deployment Cancelled"
                else -> "Deployment Status"
            }

            val message = buildString {
                append(task.artifactName)
                if (task.tenantName != "Unknown Tenant") {
                    append(" → ${task.tenantName}")
                }
                task.duration?.let {
                    append(" (${it.seconds}s)")
                }
            }

            val content = task.errorMessage ?: when (task.status) {
                DeploymentStatus.SUCCESS -> "Artifact deployed successfully and is now active"
                DeploymentStatus.CANCELLED -> "Deployment was cancelled by user"
                else -> ""
            }

            val notification = Notification(
                "Custom Notification Group",
                title,
                if (content.isNotEmpty()) "$message\n$content" else message,
                notificationType
            )

            // Add actions based on status
            when (task.status) {
                DeploymentStatus.FAILED -> {
                    notification.addAction(RetryDeploymentAction(task, deploymentManager))
                    notification.addAction(ViewErrorDetailsAction(task))
                }
                DeploymentStatus.SUCCESS -> {
                    // No additional actions needed for successful deployments
                }
                DeploymentStatus.CANCELLED -> {
                    notification.addAction(RestartDeploymentAction(task, deploymentManager))
                }
                else -> {}
            }

            // Set longer display time for important notifications
            when (task.status) {
                DeploymentStatus.FAILED -> {
                    // Keep error notifications visible longer
                    notification.setImportant(true)
                }
                DeploymentStatus.SUCCESS -> {
                    // Success notifications can disappear automatically
                    notification.setImportant(false)
                }
                else -> {}
            }

            Notifications.Bus.notify(notification, project)
        }
    }

    // Action classes for notifications
    private class CancelDeploymentAction(
        private val taskId: String,
        private val deploymentManager: DeploymentManager
    ) : NotificationAction("Cancel") {

        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            val cancelled = deploymentManager.cancelDeployment(taskId)
            notification.expire()

            if (!cancelled) {
                val errorNotification = Notification(
                    "Custom Notification Group",
                    "Cannot Cancel",
                    "Deployment cannot be cancelled at this stage",
                    NotificationType.WARNING
                )
                Notifications.Bus.notify(errorNotification, e.project)
            }
        }
    }

    private class RetryDeploymentAction(
        private val task: DeploymentTask,
        private val deploymentManager: DeploymentManager
    ) : NotificationAction("Retry") {

        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            notification.expire()

            // Note: This would need access to the original CpiService
            // For now, we'll show a message to use the manual retry
            val retryNotification = Notification(
                "Custom Notification Group",
                "Retry Deployment",
                "To retry ${task.artifactName}, please use the Deploy action again from the CPI Explorer",
                NotificationType.INFORMATION
            )
            Notifications.Bus.notify(retryNotification, e.project)
        }
    }

    private class RestartDeploymentAction(
        private val task: DeploymentTask,
        private val deploymentManager: DeploymentManager
    ) : NotificationAction("Restart") {

        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            notification.expire()

            val restartNotification = Notification(
                "Custom Notification Group",
                "Restart Deployment",
                "To restart ${task.artifactName}, please use the Deploy action again from the CPI Explorer",
                NotificationType.INFORMATION
            )
            Notifications.Bus.notify(restartNotification, e.project)
        }
    }

    private class ViewErrorDetailsAction(
        private val task: DeploymentTask
    ) : NotificationAction("View Details") {

        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            notification.expire()

            val details = buildString {
                append("Deployment Error Details\n\n")
                append("Artifact: ${task.artifactName}\n")
                append("Tenant: ${task.tenantName}\n")
                append("Error: ${task.errorMessage ?: "Unknown error"}\n\n")
                append("Logs:\n")
                task.logs.takeLast(5).forEach { log ->
                    append("${log.timestamp}: ${log.message}\n")
                }
            }

            val detailsNotification = Notification(
                "Custom Notification Group",
                "Deployment Error Details",
                details,
                NotificationType.ERROR
            )
            detailsNotification.setImportant(true)
            Notifications.Bus.notify(detailsNotification, e.project)
        }
    }


    companion object {
        fun getInstance(project: Project): DeploymentNotificationProvider {
            return project.service<DeploymentNotificationProvider>()
        }
    }
}