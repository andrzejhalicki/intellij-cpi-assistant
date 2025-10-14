package com.cpiassistant.operations

import java.time.Duration
import java.time.Instant

/**
 * Common statuses for all operations
 */
enum class OperationStatus {
    QUEUED,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    CANCELLED;

    fun isTerminal(): Boolean = this in setOf(SUCCESS, FAILED, CANCELLED)
    fun isActive(): Boolean = this == IN_PROGRESS
}

/**
 * Interface for operation-specific phases
 */
interface OperationPhase {
    val displayName: String
    val weight: Int
}

/**
 * Log entry for operation tracking
 */
data class OperationLog(
    val timestamp: Instant,
    val level: LogLevel,
    val message: String,
    val details: String? = null
) {
    companion object {
        fun info(message: String, details: String? = null) =
            OperationLog(Instant.now(), LogLevel.INFO, message, details)

        fun warning(message: String, details: String? = null) =
            OperationLog(Instant.now(), LogLevel.WARNING, message, details)

        fun error(message: String, details: String? = null) =
            OperationLog(Instant.now(), LogLevel.ERROR, message, details)
    }
}

enum class LogLevel {
    INFO,
    WARNING,
    ERROR
}

/**
 * Generic operation task that tracks progress and status
 * @param T The type of operation phase (deployment, script update, etc.)
 */
data class OperationTask<T : OperationPhase>(
    val taskId: String,
    val operationType: String,
    val targetName: String,
    val tenantName: String,
    val status: OperationStatus,
    val currentPhase: T?,
    val startTime: Instant,
    val endTime: Instant? = null,
    val progress: Int = 0,
    val logs: List<OperationLog> = emptyList(),
    val errorMessage: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    val duration: Duration?
        get() = if (endTime != null) Duration.between(startTime, endTime) else null

    val isCompleted: Boolean
        get() = status.isTerminal()

    fun withStatus(newStatus: OperationStatus, phase: T? = null): OperationTask<T> =
        copy(
            status = newStatus,
            currentPhase = phase,
            endTime = if (newStatus.isTerminal()) Instant.now() else null,
            progress = when (newStatus) {
                OperationStatus.SUCCESS -> 100
                OperationStatus.FAILED, OperationStatus.CANCELLED -> progress
                else -> calculateProgress(phase)
            }
        )

    fun withError(error: String): OperationTask<T> =
        copy(status = OperationStatus.FAILED, errorMessage = error, endTime = Instant.now())

    fun withLog(log: OperationLog): OperationTask<T> =
        copy(logs = logs + log)

    fun withProgress(newProgress: Int): OperationTask<T> =
        copy(progress = newProgress.coerceIn(0, 100))

    fun withMetadata(key: String, value: Any): OperationTask<T> =
        copy(metadata = metadata + (key to value))

    /**
     * Calculate progress based on the current phase and its weight
     * Progress is calculated as: sum of completed phase weights + half of current phase weight
     */
    private fun calculateProgress(phase: T?): Int {
        if (phase == null) return progress

        // Get all enum values using reflection (T is guaranteed to be an enum)
        @Suppress("UNCHECKED_CAST")
        val allPhases = try {
            (phase::class.java.enumConstants as? Array<T>)?.toList() ?: return progress
        } catch (e: Exception) {
            return progress
        }

        val currentIndex = allPhases.indexOf(phase)
        if (currentIndex == -1) return progress

        // Calculate completed weight from previous phases
        val completedWeight = allPhases.take(currentIndex).sumOf { it.weight }

        // Assume halfway through current phase
        val currentPhaseProgress = phase.weight / 2

        return (completedWeight + currentPhaseProgress).coerceIn(0, 100)
    }
}

/**
 * Interface for operation executors
 * Each operation type (deployment, script update, etc.) should implement this
 */
interface OperationExecutor<T : OperationPhase> {
    /**
     * Execute the operation and return the final task state
     */
    suspend fun execute(task: OperationTask<T>, onUpdate: (OperationTask<T>) -> Unit): OperationTask<T>

    /**
     * Get ordered list of phases for this operation type
     */
    fun getPhases(): List<T>

    /**
     * Get display name for the operation type
     */
    fun getOperationTypeName(): String
}
