package com.example.posex.exercise

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Owns workout state transitions, the countdown timer, session duration
 * tracking, and per-session cue counters.
 *
 * [scope]          — composable's rememberCoroutineScope(); countdown job
 *                    is cancelled automatically when screen leaves composition.
 * [targetReps]     — when > 0, ACTIVE → COMPLETED automatically when repCount
 *                    reaches target. Pass 0 for open-ended session.
 * [onStateChanged] — called on every state transition.
 *
 * Duration tracking:
 *   - Starts when state first enters ACTIVE.
 *   - [getSessionDurationMs] returns elapsed ms at the moment it's called.
 *   - Pause time IS included in duration (no subtraction of paused intervals).
 *     Document this if you later add a rest timer.
 *
 * Cue counters:
 *   - [recordCue] must be called by WorkoutScreen on every cue shown to the user.
 *   - Counters only increment when state is ACTIVE — cues during countdown
 *     or pause are ignored even if recordCue is called.
 */
class WorkoutSession(
    private val scope: CoroutineScope,
    private val targetReps: Int = 0,
    private val onStateChanged: (WorkoutState) -> Unit
) {
    private var countdownJob: Job? = null

    var currentState: WorkoutState = WorkoutState.Idle
        private set

    // Duration tracking
    private var activeStartTimeMs: Long = 0L

    // Cue counters
    private var criticalCueCount: Int = 0
    private var warningCueCount: Int = 0

    // ── Public API ────────────────────────────────────────────────────────

    fun start() {
        when (currentState) {
            is WorkoutState.Idle,
            is WorkoutState.Paused -> beginCountdown()
            else -> Unit
        }
    }

    fun pause() {
        if (currentState !is WorkoutState.Active) return
        countdownJob?.cancel()
        val reps = (currentState as WorkoutState.Active).repCount
        transition(WorkoutState.Paused(reps))
    }

    fun stop() {
        countdownJob?.cancel()
        transition(WorkoutState.Idle)
    }

    fun onRepUpdated(newRepCount: Int): Boolean {
        if (currentState !is WorkoutState.Active) return false
        transition(WorkoutState.Active(newRepCount))
        if (targetReps > 0 && newRepCount >= targetReps) {
            transition(WorkoutState.Completed(newRepCount))
            return true
        }
        return false
    }

    /**
     * Called by WorkoutScreen each time a cue is shown to the user.
     * Only counts when state is Active — safe to call unconditionally.
     */
    fun recordCue(severity: com.example.posex.exercise.FormCue.Severity) {
        if (currentState !is WorkoutState.Active) return
        when (severity) {
            FormCue.Severity.CRITICAL -> criticalCueCount++
            FormCue.Severity.WARNING  -> warningCueCount++
            else                      -> Unit
        }
    }

    /**
     * Wall-clock ms elapsed since the session first entered ACTIVE state.
     * Returns 0 if the session never became active.
     * Pause time is included.
     */
    fun getSessionDurationMs(): Long {
        if (activeStartTimeMs == 0L) return 0L
        return System.currentTimeMillis() - activeStartTimeMs
    }

    fun getCriticalCueCount(): Int = criticalCueCount
    fun getWarningCueCount(): Int = warningCueCount

    fun isActive(): Boolean = currentState is WorkoutState.Active

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

            // Start duration clock the first time we enter Active
            if (activeStartTimeMs == 0L) {
                activeStartTimeMs = System.currentTimeMillis()
            }

            transition(WorkoutState.Active(preservedReps))
        }
    }

    private fun transition(newState: WorkoutState) {
        currentState = newState
        onStateChanged(newState)
    }
}