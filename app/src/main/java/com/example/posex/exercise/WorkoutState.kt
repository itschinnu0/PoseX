package com.example.posex.exercise

/**
 * All possible states of a workout session.
 *
 * Transitions:
 *   IDLE → WAITING_FOR_POSE  (user taps Start)
 *   WAITING_FOR_POSE → COUNTDOWN  (pose readiness check passes)
 *   COUNTDOWN → ACTIVE       (countdown reaches zero)
 *   ACTIVE → PAUSED          (user taps Pause)
 *   PAUSED → WAITING_FOR_POSE (user taps Resume — re-checks pose before counting)
 *   ACTIVE → COMPLETED       (target reps reached, or user taps Stop while active)
 *   any → IDLE               (user taps Stop)
 *
 * REST is intentionally omitted until set-based targets are added to HomeScreen.
 * Insert it between ACTIVE → REST → WAITING_FOR_POSE when that work is done.
 */
sealed class WorkoutState {

    /** Camera running, waiting for user to tap Start. */
    object Idle : WorkoutState()

    /**
     * User tapped Start. App is checking pose readiness every frame.
     * [hint] is the message shown to the user ("Step back so your full
     * body is visible", etc.). Updated every frame by PoseReadinessChecker.
     * Countdown starts automatically the moment readiness passes.
     */
    data class WaitingForPose(val hint: String) : WorkoutState()

    /**
     * Pose confirmed ready. 3-second countdown before reps begin.
     * [secondsRemaining] counts down 3 → 1 then transitions to Active.
     * Rep counting and form feedback suppressed during this state.
     */
    data class Countdown(val secondsRemaining: Int) : WorkoutState()

    /**
     * Live workout. Rep counting and form feedback fully active.
     * [repCount] is the current rep count for display.
     */
    data class Active(val repCount: Int) : WorkoutState()

    /**
     * User-initiated pause. Rep counting and TTS frozen.
     * [repCount] preserved for display.
     */
    data class Paused(val repCount: Int) : WorkoutState()

    /**
     * Session finished — target reps hit or user stopped.
     * [finalRepCount] is the count to show on summary.
     */
    data class Completed(val finalRepCount: Int) : WorkoutState()
}