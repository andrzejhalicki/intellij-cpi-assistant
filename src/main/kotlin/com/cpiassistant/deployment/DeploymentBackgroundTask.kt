package com.cpiassistant.deployment

import com.cpiassistant.services.CpiService
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DeploymentBackgroundTask(
    project: Project,
    private val artifactId: String,
    private val artifactName: String,
    private val artifactType: String,
    private val tenantName: String,
    private val service: CpiService
) : Task.Backgroundable(project, "Deploying $artifactName", true) {

    private val deploymentManager = project.service<DeploymentManager>()
    private lateinit var taskId: String
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun run(indicator: ProgressIndicator) {
        try {
            // Initialize progress
            indicator.text = "Initializing deployment of $artifactName"
            indicator.fraction = 0.0
            indicator.isIndeterminate = false

            // Start deployment
            taskId = deploymentManager.startDeployment(
                artifactId = artifactId,
                artifactName = artifactName,
                artifactType = artifactType,
                tenantName = tenantName,
                service = service
            )

            // Monitor deployment progress
            runBlocking {
                monitorDeployment(indicator)
            }

        } catch (e: Exception) {
            indicator.text = "Deployment failed: ${e.message}"
            indicator.fraction = 1.0
        }
    }

    private suspend fun monitorDeployment(indicator: ProgressIndicator) {
        var isCompleted = false

        deploymentManager.deploymentTasks
            .onEach { tasks ->
                val task = tasks[taskId]
                if (task != null) {
                    updateProgressIndicator(indicator, task)

                    if (task.isCompleted && !isCompleted) {
                        isCompleted = true

                        // Give a brief moment for the final progress update to be visible
                        delay(1500) // 1.5 second delay to show final status

                        handleCompletion(task)

                        // Cancel the monitoring coroutine to end the background task
                        scope.cancel()
                    }
                }
            }
            .launchIn(scope)
            .join()
    }

    private fun updateProgressIndicator(indicator: ProgressIndicator, task: DeploymentTask) {
        // Check for cancellation
        if (indicator.isCanceled) {
            deploymentManager.cancelDeployment(taskId)
            return
        }

        // Update progress text based on current phase
        val phaseText = when (task.currentPhase) {
            DeploymentPhase.VALIDATION -> "Validating artifact configuration"
            DeploymentPhase.BUILD -> "Building deployment package"
            DeploymentPhase.DEPLOYMENT -> "Deploying to SAP CPI runtime"
            DeploymentPhase.VERIFICATION -> "Verifying deployment success"
            null -> when (task.status) {
                DeploymentStatus.QUEUED -> "Queued for deployment"
                DeploymentStatus.SUCCESS -> "Deployment completed successfully"
                DeploymentStatus.FAILED -> "Deployment failed"
                DeploymentStatus.CANCELLED -> "Deployment cancelled"
                else -> "Processing deployment"
            }
        }

        indicator.text = phaseText

        // Update secondary text with more details
        val secondaryText = buildString {
            append(artifactName)
            if (tenantName != "Unknown Tenant") {
                append(" → $tenantName")
            }
            if (task.status.isActive()) {
                val elapsed = java.time.Duration.between(task.startTime, java.time.Instant.now()).seconds
                append(" (${elapsed}s)")
            }
        }
        indicator.text2 = secondaryText

        // Update progress fraction
        indicator.fraction = task.progress / 100.0

        // Update indeterminate state
        indicator.isIndeterminate = task.status == DeploymentStatus.QUEUED
    }

    private fun handleCompletion(@Suppress("UNUSED_PARAMETER") task: DeploymentTask) {
        // Task has been completed, final status was displayed for a moment
        // The progress bar will now disappear as the background task ends
    }

    override fun onCancel() {
        super.onCancel()
        deploymentManager.cancelDeployment(taskId)
        scope.cancel()
    }

    override fun onFinished() {
        super.onFinished()
        scope.cancel()
    }

    override fun onSuccess() {
        super.onSuccess()
        scope.cancel()
    }

    override fun onThrowable(error: Throwable) {
        super.onThrowable(error)
        scope.cancel()
    }
}