package com.cpiassistant.operations

import com.cpiassistant.services.CpiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * Deployment-specific phases
 */
enum class DeploymentPhase(
    override val displayName: String,
    override val weight: Int
) : OperationPhase {
    VALIDATION("Validating artifact configuration", 20),
    BUILD("Building deployment package", 30),
    DEPLOYMENT("Deploying to SAP CPI runtime", 40),
    VERIFICATION("Verifying deployment success", 10);

    companion object {
        fun fromCpiStatus(status: String): DeploymentPhase? = when (status.uppercase()) {
            "VALIDATING" -> VALIDATION
            "BUILDING" -> BUILD
            "DEPLOYING" -> DEPLOYMENT
            "VERIFYING" -> VERIFICATION
            else -> null
        }
    }
}

/**
 * Executor for deployment operations (integration flows and script collections)
 */
class DeploymentOperationExecutor(
    private val artifactId: String,
    private val artifactType: String,
    private val service: CpiService
) : OperationExecutor<DeploymentPhase> {

    override suspend fun execute(
        task: OperationTask<DeploymentPhase>,
        onUpdate: (OperationTask<DeploymentPhase>) -> Unit
    ): OperationTask<DeploymentPhase> {
        var currentTask = task.withStatus(OperationStatus.IN_PROGRESS, DeploymentPhase.VALIDATION)
            .withLog(OperationLog.info("Starting deployment validation"))
        onUpdate(currentTask)

        try {
            // Start actual deployment
            val cpiTaskId = deployArtifact(artifactType, artifactId, service)
            currentTask = currentTask.withLog(OperationLog.info("Deployment initiated", "CPI Task ID: $cpiTaskId"))
                .withMetadata("cpiTaskId", cpiTaskId)
            onUpdate(currentTask)

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
                        currentTask = when (cpiStatus) {
                            "SUCCESS" -> {
                                currentTask.withStatus(OperationStatus.SUCCESS)
                                    .withLog(OperationLog.info("Deployment completed successfully"))
                            }
                            "FAILED" -> {
                                currentTask.withError("Deployment failed in CPI")
                                    .withLog(OperationLog.error("Deployment failed in CPI"))
                            }
                            else -> {
                                // Map CPI status to our phases
                                val phase = DeploymentPhase.fromCpiStatus(cpiStatus) ?: DeploymentPhase.DEPLOYMENT
                                currentTask.withStatus(OperationStatus.IN_PROGRESS, phase)
                                    .withLog(OperationLog.info("Deployment phase: ${phase.displayName}"))
                            }
                        }
                        onUpdate(currentTask)

                        if (currentTask.isCompleted) {
                            break
                        }
                    } else {
                        // API error - retry with exponential backoff
                        currentTask = currentTask.withLog(OperationLog.warning("Status check failed, retrying...", cpiStatus))
                        onUpdate(currentTask)
                    }

                    // Exponential backoff
                    pollInterval = min(maxPollInterval, (pollInterval * 1.5).toLong())
                    retryCount++

                } catch (e: Exception) {
                    currentTask = currentTask.withLog(OperationLog.error("Status check error: ${e.message}"))
                    onUpdate(currentTask)
                    retryCount++

                    if (retryCount >= maxRetries) {
                        currentTask = currentTask.withError("Deployment status check timeout")
                        onUpdate(currentTask)
                        break
                    }
                }
            }

            if (retryCount >= maxRetries && !currentTask.isCompleted) {
                currentTask = currentTask.withError("Deployment timeout - status unknown")
                onUpdate(currentTask)
            }

        } catch (e: Exception) {
            currentTask = currentTask.withError("Deployment failed: ${e.message}")
                .withLog(OperationLog.error("Deployment error", e.message))
            onUpdate(currentTask)
        }

        return currentTask
    }

    override fun getPhases(): List<DeploymentPhase> = DeploymentPhase.values().toList()

    override fun getOperationTypeName(): String = "Deployment"

    private suspend fun deployArtifact(artifactType: String, artifactId: String, service: CpiService): String {
        return withContext(Dispatchers.IO) {
            when (artifactType.lowercase()) {
                "scriptcollection" -> {
                    suspendCancellableCoroutine { continuation ->
                        service.deployScriptCollection(artifactId) { taskId ->
                            continuation.resumeWith(Result.success(taskId))
                        }
                    }
                }
                else -> {
                    suspendCancellableCoroutine { continuation ->
                        service.deployArtifact(artifactId) { taskId ->
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
}
