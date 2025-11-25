package com.cpiassistant.deployment

import com.cpiassistant.services.CpiService
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

@Service(Service.Level.PROJECT)
class DeploymentManager(private val project: Project) : Disposable {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _deploymentTasks = MutableStateFlow<Map<String, DeploymentTask>>(emptyMap())
    val deploymentTasks: StateFlow<Map<String, DeploymentTask>> = _deploymentTasks.asStateFlow()

    private val activeDeployments = ConcurrentHashMap<String, Job>()
    private val maxConcurrentDeployments = 3
    private val activeCount = AtomicInteger(0)

    fun startDeployment(
        artifactId: String,
        artifactName: String,
        artifactType: String,
        tenantName: String,
        service: CpiService
    ): String {
        val taskId = generateTaskId()
        val task = DeploymentTask(
            taskId = taskId,
            artifactId = artifactId,
            artifactName = artifactName,
            artifactType = artifactType,
            tenantName = tenantName,
            status = DeploymentStatus.QUEUED,
            currentPhase = null,
            startTime = Instant.now()
        )

        updateTask(task)

        val job = scope.launch {
            try {
                executeDeployment(task, service)
            } catch (e: CancellationException) {
                updateTask(task.withStatus(DeploymentStatus.CANCELLED))
                throw e
            } catch (e: Exception) {
                updateTask(task.withError("Deployment failed: ${e.message}"))
            } finally {
                activeDeployments.remove(taskId)
                activeCount.decrementAndGet()
            }
        }

        activeDeployments[taskId] = job
        return taskId
    }

    fun cancelDeployment(taskId: String): Boolean {
        val job = activeDeployments[taskId]
        return if (job != null && job.isActive) {
            job.cancel()
            val task = _deploymentTasks.value[taskId]
            if (task != null) {
                updateTask(task.withStatus(DeploymentStatus.CANCELLED))
            }
            true
        } else {
            false
        }
    }

    private suspend fun executeDeployment(initialTask: DeploymentTask, service: CpiService) {
        // Wait for available slot
        while (activeCount.get() >= maxConcurrentDeployments) {
            delay(500)
        }
        activeCount.incrementAndGet()

        var task = initialTask.withStatus(DeploymentStatus.VALIDATING, DeploymentPhase.VALIDATION)
        updateTask(task.withLog(DeploymentLog.info("Starting deployment validation")))

        try {
            // Start actual deployment
            val cpiTaskId = deployArtifact(task, service)
            task = task.withLog(DeploymentLog.info("Deployment initiated", "CPI Task ID: $cpiTaskId"))

            // Poll for status with exponential backoff
            var pollInterval = 2000L // Start with 2 seconds
            val maxPollInterval = 10000L // Max 10 seconds
            var retryCount = 0
            val maxRetries = 30 // 5 minutes max with exponential backoff

            while (retryCount < maxRetries) {
                delay(pollInterval)

                try {
                    val (cpiStatus, success) = checkDeploymentStatus(cpiTaskId, service)

                    if (success) {
                        task = when (cpiStatus) {
                            "SUCCESS" -> {
                                task.withStatus(DeploymentStatus.SUCCESS)
                                    .withLog(DeploymentLog.info("Deployment completed successfully"))
                            }
                            "FAILED" -> {
                                task.withError("Deployment failed in CPI")
                                    .withLog(DeploymentLog.error("Deployment failed in CPI"))
                            }
                            else -> {
                                // Map CPI status to our phases
                                val phase = DeploymentPhase.fromCpiStatus(cpiStatus) ?: DeploymentPhase.DEPLOYMENT
                                val status = if (phase == DeploymentPhase.DEPLOYMENT) DeploymentStatus.DEPLOYING else DeploymentStatus.BUILDING
                                task.withStatus(status, phase)
                                    .withLog(DeploymentLog.info("Deployment phase: ${phase.displayName}"))
                            }
                        }
                        updateTask(task)

                        if (task.isCompleted) {
                            break
                        }
                    } else {
                        // API error - retry with exponential backoff
                        task = task.withLog(DeploymentLog.warning("Status check failed, retrying...", cpiStatus))
                        updateTask(task)
                    }

                    // Exponential backoff
                    pollInterval = min(maxPollInterval, (pollInterval * 1.5).toLong())
                    retryCount++

                } catch (e: Exception) {
                    task = task.withLog(DeploymentLog.error("Status check error: ${e.message}"))
                    updateTask(task)
                    retryCount++

                    if (retryCount >= maxRetries) {
                        task = task.withError("Deployment status check timeout")
                        updateTask(task)
                        break
                    }
                }
            }

            if (retryCount >= maxRetries && !task.isCompleted) {
                task = task.withError("Deployment timeout - status unknown")
                updateTask(task)
            }

        } catch (e: Exception) {
            task = task.withError("Deployment failed: ${e.message}")
                .withLog(DeploymentLog.error("Deployment error", e.message))
            updateTask(task)
        } finally {
            // Task completed - no history storage needed

            // Show completion notification using notification provider
            val notificationProvider = com.cpiassistant.notifications.DeploymentNotificationProvider.getInstance(project)
            notificationProvider.showDeploymentCompleted(task)
        }
    }

    private suspend fun deployArtifact(task: DeploymentTask, service: CpiService): String {
        return withContext(Dispatchers.IO) {
            when (task.artifactType.lowercase()) {
                "scriptcollection" -> {
                    suspendCancellableCoroutine { continuation ->
                        service.deployScriptCollection(task.artifactId) { taskId ->
                            continuation.resumeWith(Result.success(taskId))
                        }
                    }
                }
                else -> {
                    suspendCancellableCoroutine { continuation ->
                        service.deployArtifact(task.artifactId) { taskId ->
                            continuation.resumeWith(Result.success(taskId))
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkDeploymentStatus(taskId: String, service: CpiService): Pair<String, Boolean> {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                service.checkDeploymentStatus(taskId) { status, success ->
                    continuation.resumeWith(Result.success(Pair(status, success)))
                }
            }
        }
    }

    private fun updateTask(task: DeploymentTask) {
        _deploymentTasks.value = _deploymentTasks.value + (task.taskId to task)
    }

    private fun generateTaskId(): String {
        return "deploy_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    override fun dispose() {
        // Cancel all active deployment jobs
        activeDeployments.values.forEach { job ->
            job.cancel()
        }
        activeDeployments.clear()

        // Cancel the coroutine scope
        scope.cancel()
    }
}