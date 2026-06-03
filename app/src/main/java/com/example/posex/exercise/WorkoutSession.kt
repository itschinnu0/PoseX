package com.example.posex.exercise

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Owns workout state transitions, countdown timer, session duration
 * tracking, and per-session cue counters.
 *
 * State machine:
 *   IDLE → WAITING_FOR_POSE  (start())
 *   WAITING_FOR_POSE → COUNTDOWN  (onPoseReady())
 *   COUNTDOWN → ACTIVE       (countdown finishes)
 *   ACTIVE → PAUSED          (pause())
 *   PAUSED → WAITING_FOR_POSE (start() — re-checks pose before resuming)
 *   any → IDLE               (stop())
 */
class WorkoutSession(
    private val scope: CoroutineScope,
    private val targetReps: Int = 0,
    private val totalSets: Int = 1,
    private val restSeconds: Int = 60,
    private val onStateChanged: (WorkoutState) -> Unit
) {
    private var countdownJob: Job? = null

    var currentState: WorkoutState = WorkoutState.Idle
        private set

    private var activeStartTimeMs: Long = 0L
    private var criticalCueCount: Int = 0
    private var warningCueCount: Int = 0
    private var currentSet: Int = 1

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Called when user taps Start (from Idle) or Resume (from Paused).
     * Moves to WaitingForPose — countdown starts only after pose is confirmed.
     */
    fun start() {
        when (currentState) {
            is WorkoutState.Idle,
            is WorkoutState.Paused -> {
                countdownJob?.cancel()
                PoseReadinessChecker.reset()
                transition(WorkoutState.WaitingForPose("Get into position"))
            }
            else -> Unit
        }
    }

    /**
     * Called by WorkoutScreen every frame during WaitingForPose when
     * PoseReadinessChecker confirms the pose is ready.
     * Starts the countdown immediately.
     */
    fun onPoseReady() {
        if (currentState !is WorkoutState.WaitingForPose) return
        beginCountdown()
    }

    /**
     * Called by WorkoutScreen every frame during WaitingForPose to
     * update the hint message shown to the user.
     */
    fun updatePoseHint(hint: String) {
        if (currentState !is WorkoutState.WaitingForPose) return
        transition(WorkoutState.WaitingForPose(hint))
    }

    fun pause() {
        if (currentState !is WorkoutState.Active) return
        countdownJob?.cancel()
        val reps = (currentState as WorkoutState.Active).repCount
        transition(WorkoutState.Paused(reps))
    }

    fun stop() {
        countdownJob?.cancel()
        PoseReadinessChecker.reset()
        currentSet = 1
        transition(WorkoutState.Idle)
    }

    fun onRepUpdated(newRepCount: Int): Boolean {
        if (currentState !is WorkoutState.Active) return false
        
        transition(WorkoutState.Active(newRepCount))
        
        // Only check for completion if we're still in Active state (not transitioned to Rest)
        if (currentState is WorkoutState.Active && targetReps > 0 && newRepCount >= targetReps) {
            return onSetCompleted(finalRepCount = newRepCount)
        }
        return false
    }

    fun onHoldCompleted(): Boolean {
        if (currentState !is WorkoutState.Active) return false
        return onSetCompleted(finalRepCount = 0)
    }

    fun recordCue(severity: FormCue.Severity) {
        if (currentState !is WorkoutState.Active) return
        when (severity) {
            FormCue.Severity.CRITICAL -> criticalCueCount++
            FormCue.Severity.WARNING  -> warningCueCount++
            else                      -> Unit
        }
    }

    fun getSessionDurationMs(): Long {
        if (activeStartTimeMs == 0L) return 0L
        return System.currentTimeMillis() - activeStartTimeMs
    }

    fun getCriticalCueCount(): Int = criticalCueCount
    fun getWarningCueCount(): Int = warningCueCount

    fun isActive(): Boolean = currentState is WorkoutState.Active
    fun isWaitingForPose(): Boolean = currentState is WorkoutState.WaitingForPose

    // ── Private ───────────────────────────────────────────────────────────

    private fun beginCountdown() {
        countdownJob?.cancel()
        transition(WorkoutState.Countdown(3))

        countdownJob = scope.launch {
            for (secondsLeft in 2 downTo 1) {
                delay(1000L)
                if (currentState !is WorkoutState.Countdown) return@launch
                transition(WorkoutState.Countdown(secondsLeft))
            }
            delay(1000L)
            if (currentState !is WorkoutState.Countdown) return@launch

            val preservedReps = when (val s = currentState) {
                is WorkoutState.Paused -> s.repCount
                else -> 0
            }

            if (activeStartTimeMs == 0L) {
                activeStartTimeMs = System.currentTimeMillis()
            }

            transition(WorkoutState.Active(preservedReps))
        }
    }

    private fun beginRest() {
        if (restSeconds <= 0) {
            transition(WorkoutState.WaitingForPose("Get into position"))
            return
        }

        countdownJob?.cancel()
        transition(WorkoutState.Rest(restSeconds, currentSet, totalSets))

        countdownJob = scope.launch {
            for (secondsLeft in restSeconds - 1 downTo 1) {
                delay(1000L)
                if (currentState !is WorkoutState.Rest) return@launch
                transition(WorkoutState.Rest(secondsLeft, currentSet, totalSets))
            }
            delay(1000L)
            if (currentState !is WorkoutState.Rest) return@launch
            currentSet += 1
            PoseReadinessChecker.reset()
            transition(WorkoutState.WaitingForPose("Get into position"))
        }
    }

    private fun onSetCompleted(finalRepCount: Int): Boolean {
        if (currentSet >= totalSets) {
            transition(WorkoutState.Completed(finalRepCount))
            return true
        }
        beginRest()
        return false
    }

    private fun transition(newState: WorkoutState) {
        currentState = newState
        onStateChanged(newState)
    }
}