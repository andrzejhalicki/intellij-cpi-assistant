package com.cpiassistant.operations

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Generic background task that monitors operation progress and updates the progress indicator
 * This replaces the deployment-specific DeploymentBackgroundTask
 *
 * @param T The type of operation phase
 */
class OperationBackgroundTask<T : OperationPhase>(
    project: Project,
    private val taskId: String,
    private val taskTitle: String,
    private val phaseFormatter: (T?, OperationStatus) -> String = { phase, status ->
        phase?.displayName ?: when (status) {
            OperationStatus.QUEUED -> "Queued"
            OperationStatus.SUCCESS -> "Completed successfully"
            OperationStatus.FAILED -> "Failed"
            OperationStatus.CANCELLED -> "Cancelled"
            else -> "Processing"
        }
    }
) : Task.Backgroundable(project, taskTitle, true) {

    private val operationManager = project.service<OperationManager>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun run(indicator: ProgressIndicator) {
        try {
            // Initialize progress
            indicator.text = "Initializing $taskTitle"
            indicator.fraction = 0.0
            indicator.isIndeterminate = false

            // Monitor operation progress
            runBlocking {
                monitorOperation(indicator)
            }

        } catch (e: Exception) {
            indicator.text = "Operation failed: ${e.message}"
            indicator.fraction = 1.0
        }
    }

    private suspend fun monitorOperation(indicator: ProgressIndicator) {
        var isCompleted = false

        operationManager.operationTasks
            .onEach { tasks ->
                @Suppress("UNCHECKED_CAST")
                val task = tasks[taskId] as? OperationTask<T>
                if (task != null) {
                    updateProgressIndicator(indicator, task)

                    if (task.isCompleted && !isCompleted) {
                        isCompleted = true

                        // Give a brief moment for the final progress update to be visible
                        delay(1500) // 1.5 second delay to show final status

                        // Cancel the monitoring coroutine to end the background task
                        scope.cancel()
                    }
                }
            }
            .launchIn(scope)
            .join()
    }

    private fun updateProgressIndicator(indicator: ProgressIndicator, task: OperationTask<T>) {
        // Check for cancellation
        if (indicator.isCanceled) {
            operationManager.cancelOperation(taskId)
            return
        }

        // Update progress text based on current phase
        val phaseText = phaseFormatter(task.currentPhase, task.status)
        indicator.text = phaseText

        // Update secondary text with more details
        val secondaryText = buildString {
            append(task.targetName)
            if (task.tenantName != "Unknown Tenant") {
                append(" → ${task.tenantName}")
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
        indicator.isIndeterminate = task.status == OperationStatus.QUEUED
    }

    override fun onCancel() {
        super.onCancel()
        operationManager.cancelOperation(taskId)
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
