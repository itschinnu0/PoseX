package com.example.posex.exercise

/**
 * All possible states of a workout session.
 *
 * Transitions:
 *   IDLE → COUNTDOWN  (user taps Start)
 *   COUNTDOWN → ACTIVE (countdown reaches zero)
 *   ACTIVE → PAUSED   (user taps Pause)
 *   PAUSED → COUNTDOWN (user taps Resume — gives a fresh 3s countdown)
 *   ACTIVE → COMPLETED (target reps reached, or user taps Stop while active)
 *   any → IDLE         (user taps Stop)
 *
 * REST is intentionally omitted until set-based targets are added to HomeScreen.
 * Insert it between ACTIVE → REST → COUNTDOWN when that work is done.
 */
sealed class WorkoutState {

    /** Camera running, pose tracking active, waiting for user to tap Start. */
    object Idle : WorkoutState()

    /**
     * 3-second countdown before reps begin.
     * [secondsRemaining] counts down from 3 to 1 then transitions to Active.
     * Rep counting and form feedback are suppressed during this state.
     */
    data class Countdown(val secondsRemaining: Int) : WorkoutState()

    /**
     * Live workout. Rep counting and form feedback are fully active.
     * [repCount] is the current rep count for display.
     */
    data class Active(val repCount: Int) : WorkoutState()

    /**
     * User-initiated pause. Rep counting and TTS are frozen.
     * [repCount] preserved for display.
     */
    data class Paused(val repCount: Int) : WorkoutState()

    /**
     * Session finished — either target reps hit or user stopped.
     * [finalRepCount] is the count to display on the summary.
     */
    data class Completed(val finalRepCount: Int) : WorkoutState()
}