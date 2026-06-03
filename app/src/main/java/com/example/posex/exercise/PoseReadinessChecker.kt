package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Checks whether the pose landmarks required for a given exercise are
 * visible with sufficient confidence.
 *
 * Called every frame during WaitingForPose state. Returns a [ReadinessResult]
 * indicating whether the user is correctly positioned, and a hint string
 * to display if they are not.
 *
 * Required landmarks per exercise:
 *   SQUAT  — shoulders, hips, knees, ankles (full body side view)
 *   PUSHUP — shoulders, elbows, wrists, hips (upper body)
 *   PLANK  — shoulders, hips, ankles, elbows (full body side view)
 *
 * Consecutive pass counter: readiness is only confirmed after
 * REQUIRED_CONSECUTIVE_READY_FRAMES clean frames in a row. This prevents
 * a single good frame from triggering the countdown while the user is
 * still walking into position.
 */
object PoseReadinessChecker {

    private const val MIN_CONFIDENCE = 0.5f

    // Require 3 consecutive clean frames (~300ms at 100ms interval)
    // before confirming readiness. Prevents false triggers while
    // the user is still walking into position.
    private const val REQUIRED_CONSECUTIVE_READY_FRAMES = 3

    private var consecutiveReadyFrames = 0

    data class ReadinessResult(
        val isReady: Boolean,
        val hint: String
    )

    /**
     * Call once per frame during WaitingForPose.
     * Returns isReady=true only after REQUIRED_CONSECUTIVE_READY_FRAMES
     * consecutive clean frames.
     */
    fun check(pose: Pose, exerciseType: ExerciseType): ReadinessResult {
        val result = checkOnce(pose, exerciseType)
        if (result.isReady) {
            consecutiveReadyFrames++
        } else {
            consecutiveReadyFrames = 0
        }
        val confirmed = consecutiveReadyFrames >= REQUIRED_CONSECUTIVE_READY_FRAMES
        return ReadinessResult(
            isReady = confirmed,
            hint = if (confirmed) "Great, get ready!" else result.hint
        )
    }

    /** Resets the consecutive frame counter. Call when leaving WaitingForPose. */
    fun reset() {
        consecutiveReadyFrames = 0
    }

    // ── Private ───────────────────────────────────────────────────────────

    private fun checkOnce(pose: Pose, exerciseType: ExerciseType): ReadinessResult {
        return when (exerciseType) {
            ExerciseType.SQUAT  -> checkSquat(pose)
            ExerciseType.PUSHUP -> checkPushup(pose)
            ExerciseType.PLANK  -> checkPlank(pose)
            ExerciseType.BICEPS_CURL -> checkBicepsCurl(pose)
        }
    }

    private fun checkSquat(pose: Pose): ReadinessResult {
        // Need at least one full side visible: shoulder + hip + knee + ankle
        val leftReady = isVisible(pose, PoseLandmark.LEFT_SHOULDER) &&
                isVisible(pose, PoseLandmark.LEFT_HIP) &&
                isVisible(pose, PoseLandmark.LEFT_KNEE) &&
                isVisible(pose, PoseLandmark.LEFT_ANKLE)

        val rightReady = isVisible(pose, PoseLandmark.RIGHT_SHOULDER) &&
                isVisible(pose, PoseLandmark.RIGHT_HIP) &&
                isVisible(pose, PoseLandmark.RIGHT_KNEE) &&
                isVisible(pose, PoseLandmark.RIGHT_ANKLE)

        if (leftReady || rightReady) {
            return ReadinessResult(true, "Great, get ready!")
        }

        // Diagnose what's missing to give a useful hint
        val hipVisible = isVisible(pose, PoseLandmark.LEFT_HIP) ||
                isVisible(pose, PoseLandmark.RIGHT_HIP)
        val kneeVisible = isVisible(pose, PoseLandmark.LEFT_KNEE) ||
                isVisible(pose, PoseLandmark.RIGHT_KNEE)
        val ankleVisible = isVisible(pose, PoseLandmark.LEFT_ANKLE) ||
                isVisible(pose, PoseLandmark.RIGHT_ANKLE)

        return when {
            !ankleVisible -> ReadinessResult(false, "Step back — your feet are not visible")
            !kneeVisible  -> ReadinessResult(false, "Step back — your knees are not visible")
            !hipVisible   -> ReadinessResult(false, "Step back — your hips are not visible")
            else          -> ReadinessResult(false, "Stand sideways so your full body is visible")
        }
    }

    private fun checkPushup(pose: Pose): ReadinessResult {
        // Need both shoulders, elbows, wrists, and hips
        val ready = isVisible(pose, PoseLandmark.LEFT_SHOULDER) &&
                isVisible(pose, PoseLandmark.RIGHT_SHOULDER) &&
                isVisible(pose, PoseLandmark.LEFT_ELBOW) &&
                isVisible(pose, PoseLandmark.RIGHT_ELBOW) &&
                isVisible(pose, PoseLandmark.LEFT_WRIST) &&
                isVisible(pose, PoseLandmark.RIGHT_WRIST) &&
                isVisible(pose, PoseLandmark.LEFT_HIP) &&
                isVisible(pose, PoseLandmark.RIGHT_HIP)

        if (ready) return ReadinessResult(true, "Great, get ready!")

        val shouldersVisible = isVisible(pose, PoseLandmark.LEFT_SHOULDER) &&
                isVisible(pose, PoseLandmark.RIGHT_SHOULDER)
        val wristsVisible = isVisible(pose, PoseLandmark.LEFT_WRIST) &&
                isVisible(pose, PoseLandmark.RIGHT_WRIST)
        val hipsVisible = isVisible(pose, PoseLandmark.LEFT_HIP) &&
                isVisible(pose, PoseLandmark.RIGHT_HIP)

        return when {
            !shouldersVisible -> ReadinessResult(false, "Move back — your shoulders are not visible")
            !wristsVisible    -> ReadinessResult(false, "Move back — your hands are not visible")
            !hipsVisible      -> ReadinessResult(false, "Move back — your hips are not visible")
            else              -> ReadinessResult(false, "Move back so your full upper body is visible")
        }
    }

    private fun checkPlank(pose: Pose): ReadinessResult {
        // Need shoulders, hips, ankles, and elbows
        val leftReady = isVisible(pose, PoseLandmark.LEFT_SHOULDER) &&
                isVisible(pose, PoseLandmark.LEFT_HIP) &&
                isVisible(pose, PoseLandmark.LEFT_ANKLE) &&
                isVisible(pose, PoseLandmark.LEFT_ELBOW)

        val rightReady = isVisible(pose, PoseLandmark.RIGHT_SHOULDER) &&
                isVisible(pose, PoseLandmark.RIGHT_HIP) &&
                isVisible(pose, PoseLandmark.RIGHT_ANKLE) &&
                isVisible(pose, PoseLandmark.RIGHT_ELBOW)

        if (leftReady || rightReady) return ReadinessResult(true, "Great, get ready!")

        val ankleVisible = isVisible(pose, PoseLandmark.LEFT_ANKLE) ||
                isVisible(pose, PoseLandmark.RIGHT_ANKLE)
        val elbowVisible = isVisible(pose, PoseLandmark.LEFT_ELBOW) ||
                isVisible(pose, PoseLandmark.RIGHT_ELBOW)

        return when {
            !ankleVisible -> ReadinessResult(false, "Move back — your feet are not visible")
            !elbowVisible -> ReadinessResult(false, "Move back — your elbows are not visible")
            else          -> ReadinessResult(false, "Move back so your full body is visible from the side")
        }
    }

    private fun checkBicepsCurl(pose: Pose): ReadinessResult {
        val leftVisible = isVisible(pose, PoseLandmark.LEFT_SHOULDER) &&
                isVisible(pose, PoseLandmark.LEFT_ELBOW) &&
                isVisible(pose, PoseLandmark.LEFT_WRIST) &&
                isVisible(pose, PoseLandmark.LEFT_HIP)

        val rightVisible = isVisible(pose, PoseLandmark.RIGHT_SHOULDER) &&
                isVisible(pose, PoseLandmark.RIGHT_ELBOW) &&
                isVisible(pose, PoseLandmark.RIGHT_WRIST) &&
                isVisible(pose, PoseLandmark.RIGHT_HIP)

        return if (leftVisible || rightVisible) {
            ReadinessResult(true, "Ready! Starting curls...")
        } else {
            ReadinessResult(false, "Show your full side profile (Shoulder to Hip)")
        }
    }

    private fun isVisible(pose: Pose, landmarkType: Int): Boolean {
        val landmark = pose.getPoseLandmark(landmarkType) ?: return false
        return landmark.inFrameLikelihood >= MIN_CONFIDENCE
    }
}