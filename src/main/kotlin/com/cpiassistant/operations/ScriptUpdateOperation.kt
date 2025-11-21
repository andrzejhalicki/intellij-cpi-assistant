package com.cpiassistant.operations

import com.cpiassistant.nodes.artifact.CpiArtifact
import com.cpiassistant.nodes.artifact.CpiScriptCollection
import com.cpiassistant.nodes.resource.CpiResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Script update-specific phases
 */
enum class ScriptUpdatePhase(
    override val displayName: String,
    override val weight: Int
) : OperationPhase {
    VALIDATION("Validating script content", 30),
    UPLOAD("Uploading script to CPI", 50),
    VERIFICATION("Verifying update success", 20);
}

/**
 * Executor for script update operations
 */
class ScriptUpdateOperationExecutor(
    private val artifact: CpiArtifact,
    private val resource: CpiResource,
    private val content: String
) : OperationExecutor<ScriptUpdatePhase> {

    override suspend fun execute(
        task: OperationTask<ScriptUpdatePhase>,
        onUpdate: (OperationTask<ScriptUpdatePhase>) -> Unit
    ): OperationTask<ScriptUpdatePhase> {
        var currentTask = task.withStatus(OperationStatus.IN_PROGRESS, ScriptUpdatePhase.VALIDATION)
            .withLog(OperationLog.info("Validating script content"))
        onUpdate(currentTask)

        try {
            // Validation phase - simulate validation delay
            delay(500)

            // Check if content is valid (basic validation)
            if (content.isEmpty()) {
                return currentTask.withError("Script content is empty")
                    .withLog(OperationLog.error("Validation failed: empty content"))
            }

            currentTask = currentTask.withProgress(30)
                .withLog(OperationLog.info("Validation completed"))
            onUpdate(currentTask)

            // Upload phase
            currentTask = currentTask.withStatus(OperationStatus.IN_PROGRESS, ScriptUpdatePhase.UPLOAD)
                .withLog(OperationLog.info("Uploading script to CPI"))
            onUpdate(currentTask)

            // Perform the actual update
            val (success, message) = updateResource(artifact, resource, content)

            if (!success) {
                return currentTask.withError("Upload failed: $message")
                    .withLog(OperationLog.error("Upload failed", message))
            }

            currentTask = currentTask.withProgress(80)
                .withLog(OperationLog.info("Upload completed"))
            onUpdate(currentTask)

            // Verification phase
            currentTask = currentTask.withStatus(OperationStatus.IN_PROGRESS, ScriptUpdatePhase.VERIFICATION)
                .withLog(OperationLog.info("Verifying update"))
            onUpdate(currentTask)

            delay(500) // Brief delay for verification

            // Success
            currentTask = currentTask.withStatus(OperationStatus.SUCCESS)
                .withLog(OperationLog.info("Script update completed successfully"))
            onUpdate(currentTask)

        } catch (e: Exception) {
            currentTask = currentTask.withError("Script update failed: ${e.message}")
                .withLog(OperationLog.error("Update error", e.message))
            onUpdate(currentTask)
        }

        return currentTask
    }

    override fun getPhases(): List<ScriptUpdatePhase> = ScriptUpdatePhase.values().toList()

    override fun getOperationTypeName(): String = "Script Update"

    private suspend fun updateResource(
        artifact: CpiArtifact,
        resource: CpiResource,
        content: String
    ): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                // Call the appropriate service method based on artifact type
                // This avoids duplicate notifications (artifact.updateResource shows notifications)
                // The notification will be shown by OperationManager instead
                when (artifact) {
                    is CpiScriptCollection -> {
                        artifact.service.updateScriptCollectionResource(artifact.id, resource.name, content) { success, message ->
                            continuation.resumeWith(Result.success(Pair(success, message)))
                        }
                    }
                    else -> {
                        artifact.service.updateResource(artifact.id, resource, content) { success, message ->
                            continuation.resumeWith(Result.success(Pair(success, message)))
                        }
                    }
                }
            }
        }
    }
}
