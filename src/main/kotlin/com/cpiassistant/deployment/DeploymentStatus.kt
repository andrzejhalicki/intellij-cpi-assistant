package com.cpiassistant.deployment

import java.time.Instant
import java.time.Duration

enum class DeploymentStatus {
    QUEUED,
    VALIDATING,
    BUILDING,
    DEPLOYING,
    SUCCESS,
    FAILED,
    CANCELLED;

    fun isTerminal(): Boolean = this in setOf(SUCCESS, FAILED, CANCELLED)
    fun isActive(): Boolean = this in setOf(VALIDATING, BUILDING, DEPLOYING)
}

enum class DeploymentPhase(val displayName: String, val weight: Int) {
    VALIDATION("Validating artifact", 20),
    BUILD("Building deployment package", 30),
    DEPLOYMENT("Deploying to runtime", 40),
    VERIFICATION("Verifying deployment", 10);

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

data class DeploymentTask(
    val taskId: String,
    val artifactId: String,
    val artifactName: String,
    val artifactType: String,
    val tenantName: String,
    val status: DeploymentStatus,
    val currentPhase: DeploymentPhase?,
    val startTime: Instant,
    val endTime: Instant? = null,
    val progress: Int = 0,
    val logs: List<DeploymentLog> = emptyList(),
    val errorMessage: String? = null,
) {
    val duration: Duration?
        get() = if (endTime != null) Duration.between(startTime, endTime) else null


    val isCompleted: Boolean
        get() = status.isTerminal()

    fun withStatus(newStatus: DeploymentStatus, phase: DeploymentPhase? = null): DeploymentTask =
        copy(
            status = newStatus,
            currentPhase = phase,
            endTime = if (newStatus.isTerminal()) Instant.now() else null,
            progress = when (newStatus) {
                DeploymentStatus.SUCCESS -> 100
                DeploymentStatus.FAILED, DeploymentStatus.CANCELLED -> progress
                else -> calculateProgress(phase)
            }
        )

    fun withError(error: String): DeploymentTask =
        copy(status = DeploymentStatus.FAILED, errorMessage = error, endTime = Instant.now())

    fun withLog(log: DeploymentLog): DeploymentTask =
        copy(logs = logs + log)


    private fun calculateProgress(phase: DeploymentPhase?): Int {
        if (phase == null) return progress

        val phaseValues = DeploymentPhase.values()
        val currentIndex = phaseValues.indexOf(phase)
        val completedWeight = phaseValues.take(currentIndex).sumOf { it.weight }
        val currentPhaseProgress = phase.weight / 2 // Assume halfway through current phase

        return completedWeight + currentPhaseProgress
    }
}

data class DeploymentLog(
    val timestamp: Instant,
    val level: LogLevel,
    val message: String,
    val details: String? = null
) {
    companion object {
        fun info(message: String, details: String? = null) =
            DeploymentLog(Instant.now(), LogLevel.INFO, message, details)

        fun warning(message: String, details: String? = null) =
            DeploymentLog(Instant.now(), LogLevel.WARNING, message, details)

        fun error(message: String, details: String? = null) =
            DeploymentLog(Instant.now(), LogLevel.ERROR, message, details)
    }
}

enum class LogLevel {
    INFO,
    WARNING,
    ERROR
}