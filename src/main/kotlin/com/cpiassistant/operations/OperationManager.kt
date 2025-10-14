package com.cpiassistant.operations

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

/**
 * Generic operation manager service that tracks and executes operations
 * This replaces the deployment-specific DeploymentManager with a reusable solution
 */
@Service(Service.Level.PROJECT)
class OperationManager(private val project: Project) : Disposable {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _operationTasks = MutableStateFlow<Map<String, OperationTask<*>>>(emptyMap())
    val operationTasks: StateFlow<Map<String, OperationTask<*>>> = _operationTasks.asStateFlow()

    private val activeOperations = ConcurrentHashMap<String, Job>()
    private val maxConcurrentOperations = 3
    private val activeCount = AtomicInteger(0)

    /**
     * Start a new operation with the given executor
     */
    fun <T : OperationPhase> startOperation(
        targetName: String,
        tenantName: String,
        executor: OperationExecutor<T>,
        metadata: Map<String, Any> = emptyMap()
    ): String {
        val taskId = generateTaskId()
        val task = OperationTask<T>(
            taskId = taskId,
            operationType = executor.getOperationTypeName(),
            targetName = targetName,
            tenantName = tenantName,
            status = OperationStatus.QUEUED,
            currentPhase = null,
            startTime = Instant.now(),
            metadata = metadata
        )

        updateTask(task)

        val job = scope.launch {
            try {
                executeOperation(task, executor)
            } catch (e: CancellationException) {
                updateTask(task.withStatus(OperationStatus.CANCELLED))
                throw e
            } catch (e: Exception) {
                updateTask(task.withError("Operation failed: ${e.message}"))
            } finally {
                activeOperations.remove(taskId)
                activeCount.decrementAndGet()
            }
        }

        activeOperations[taskId] = job
        return taskId
    }

    /**
     * Cancel an operation by task ID
     */
    fun cancelOperation(taskId: String): Boolean {
        val job = activeOperations[taskId]
        return if (job != null && job.isActive) {
            job.cancel()
            val task = _operationTasks.value[taskId]
            if (task != null) {
                @Suppress("UNCHECKED_CAST")
                updateTask((task as OperationTask<OperationPhase>).withStatus(OperationStatus.CANCELLED))
            }
            true
        } else {
            false
        }
    }

    /**
     * Get an operation task by ID
     */
    fun getOperation(taskId: String): OperationTask<*>? {
        return _operationTasks.value[taskId]
    }

    /**
     * Execute an operation with the given executor
     */
    private suspend fun <T : OperationPhase> executeOperation(
        initialTask: OperationTask<T>,
        executor: OperationExecutor<T>
    ) {
        // Wait for available slot
        while (activeCount.get() >= maxConcurrentOperations) {
            delay(500)
        }
        activeCount.incrementAndGet()

        var task = initialTask.withStatus(OperationStatus.IN_PROGRESS)
            .withLog(OperationLog.info("Starting ${executor.getOperationTypeName()}"))
        updateTask(task)

        try {
            // Execute the operation - the executor will call onUpdate to report progress
            task = executor.execute(task) { updatedTask ->
                updateTask(updatedTask)
            }

            // Ensure final task state is updated
            if (!task.isCompleted) {
                task = task.withStatus(OperationStatus.SUCCESS)
                    .withLog(OperationLog.info("${executor.getOperationTypeName()} completed successfully"))
            }
            updateTask(task)

        } catch (e: Exception) {
            task = task.withError("${executor.getOperationTypeName()} failed: ${e.message}")
                .withLog(OperationLog.error("Operation error", e.message))
            updateTask(task)
        } finally {
            // Show completion notification
            val notificationProvider = com.cpiassistant.notifications.OperationNotificationProvider.getInstance(project)
            notificationProvider.showOperationCompleted(task)
        }
    }

    /**
     * Update a task in the state flow
     */
    fun <T : OperationPhase> updateTask(task: OperationTask<T>) {
        _operationTasks.value = _operationTasks.value + (task.taskId to task)
    }

    /**
     * Generate a unique task ID
     */
    private fun generateTaskId(): String {
        return "op_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    /**
     * Clean up completed tasks (optional, for memory management)
     */
    fun cleanupCompletedTasks() {
        _operationTasks.value = _operationTasks.value.filterValues { !it.isCompleted }
    }

    /**
     * Dispose of resources when the service is no longer needed
     */
    override fun dispose() {
        // Cancel all active operations
        activeOperations.values.forEach { it.cancel() }
        activeOperations.clear()

        // Cancel the coroutine scope
        scope.cancel()
    }
}
