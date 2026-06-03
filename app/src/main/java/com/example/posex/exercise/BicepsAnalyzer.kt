package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs

object BicepsAnalyzer {

    private const val MIN_CONFIDENCE = 0.5f

    private val repCounter = CalibratingRepCounter(
        fallbackBottom = 35.0, // Flexed (Peak)
        fallbackTop = 160.0    // Extended (Rest)
    )

    // ── Calibration constants ──────────────────────────────────────────
    private const val AT_TOP_ANGLE_THRESHOLD = 155.0 // Arm extended
    private const val BASELINE_STABILITY_FRAMES = 5
    
    // ── Form correction constants ──────────────────────────────────────
    private const val MAX_ELBOW_SWING_THRESHOLD = 0.15f // Percentage of torso height

    // ── State ─────────────────────────────────────────────────────────────
    private var standingElbowX: Float? = null
    private var hasValidBaseline: Boolean = false
    private var standingTorsoHeight: Float? = null

    private var consecutiveTopFrames: Int = 0
    private var accumulatedElbowX: Float = 0f
    private var accumulatedTorsoHeight: Float = 0f

    private var maxElbowDisplacementThisRep: Float = 0f
    private var wasAtTop: Boolean = false

    init {
        repCounter.movementValidator = ::validateForm
    }

    fun resetRepCounter() {
        repCounter.reset()
        standingElbowX = null
        hasValidBaseline = false
        standingTorsoHeight = null
        consecutiveTopFrames = 0
        accumulatedElbowX = 0f
        accumulatedTorsoHeight = 0f
        maxElbowDisplacementThisRep = 0f
        wasAtTop = false
    }

    fun analyze(pose: Pose): ExerciseAnalysisResult {
        val cues = mutableListOf<FormCue>()

        // 1. Extract landmarks
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        // 2. Side Selection (Pick the side with better visibility)
        val leftConfidence = listOf(leftShoulder, leftElbow, leftWrist, leftHip)
            .mapNotNull { it?.inFrameLikelihood }.average()
        val rightConfidence = listOf(rightShoulder, rightElbow, rightWrist, rightHip)
            .mapNotNull { it?.inFrameLikelihood }.average()

        val isLeftVisible = leftConfidence >= rightConfidence

        val shoulder = if (isLeftVisible) leftShoulder else rightShoulder
        val elbow = if (isLeftVisible) leftElbow else rightElbow
        val wrist = if (isLeftVisible) leftWrist else rightWrist
        val hip = if (isLeftVisible) leftHip else rightHip

        // 3. Confidence Check
        if (shoulder == null || elbow == null || wrist == null || hip == null ||
            shoulder.inFrameLikelihood < MIN_CONFIDENCE ||
            elbow.inFrameLikelihood < MIN_CONFIDENCE ||
            wrist.inFrameLikelihood < MIN_CONFIDENCE ||
            hip.inFrameLikelihood < MIN_CONFIDENCE
        ) {
            repCounter.abortCurrentRep()
            cues.add(FormCue("Move into frame for side-view analysis", FormCue.Severity.INFO))
            return ExerciseAnalysisResult(
                cues = cues,
                repCount = repCounter.getRepCount(),
                metricValue = null,
                isCalibrating = repCounter.isCalibrating()
            )
        }

        // 4. Angle Calculation
        val angle = PoseUtils.calculateAngle(shoulder, elbow, wrist)
        val currentElbowX = elbow.position.x
        val currentTorsoHeight = abs(shoulder.position.y - hip.position.y)

        // 5. Calibration Logic
        val isAtTop = angle > AT_TOP_ANGLE_THRESHOLD

        if (isAtTop) {
            consecutiveTopFrames++
            accumulatedElbowX += currentElbowX
            accumulatedTorsoHeight += currentTorsoHeight

            if (consecutiveTopFrames >= BASELINE_STABILITY_FRAMES) {
                val avgX = accumulatedElbowX / consecutiveTopFrames
                val avgH = accumulatedTorsoHeight / consecutiveTopFrames

                if (!wasAtTop || standingElbowX == null) {
                    standingElbowX = avgX
                    standingTorsoHeight = avgH
                    hasValidBaseline = true
                }
                consecutiveTopFrames = 0
                accumulatedElbowX = 0f
                accumulatedTorsoHeight = 0f
            }
        } else if (hasValidBaseline) {
            consecutiveTopFrames = 0
            val displacement = abs(currentElbowX - (standingElbowX ?: currentElbowX))
            if (displacement > maxElbowDisplacementThisRep) {
                maxElbowDisplacementThisRep = displacement
            }
        }
        wasAtTop = isAtTop

        // 6. Form Corrections
        if (hasValidBaseline) {
            val threshold = (standingTorsoHeight ?: 100f) * MAX_ELBOW_SWING_THRESHOLD
            if (maxElbowDisplacementThisRep > threshold) {
                cues.add(FormCue("Keep your elbow pinned to your side", FormCue.Severity.WARNING))
            }
        }

        if (angle < 25) {
            cues.add(FormCue("Don't curl too high, keep tension on bicep", FormCue.Severity.WARNING))
        }

        if (repCounter.isCalibrating()) {
            cues.add(FormCue("Perform one full curl to calibrate", FormCue.Severity.INFO))
        }

        // 7. Rep Counting
        val formValid = cues.none { it.severity == FormCue.Severity.CRITICAL }
        val reps = repCounter.updateReps(angle, formValid)

        if (repCounter.lastRepRejected) {
            cues.add(FormCue(repCounter.rejectionReason, FormCue.Severity.CRITICAL))
        }

        if (cues.none { it.severity != FormCue.Severity.INFO } && !repCounter.lastRepRejected) {
            cues.add(FormCue("Good curl, keep it controlled", FormCue.Severity.SUCCESS))
        }

        return ExerciseAnalysisResult(
            cues = cues,
            repCount = reps,
            metricValue = angle,
            isCalibrating = repCounter.isCalibrating(),
            repRejected = repCounter.lastRepRejected,
            rejectionReason = repCounter.rejectionReason
        )
    }

    private fun validateForm(): CalibratingRepCounter.ValidationResult {
        if (!hasValidBaseline) {
            return CalibratingRepCounter.ValidationResult(false, "Hold arm straight down to calibrate")
        }

        val torsoH = standingTorsoHeight ?: 100f
        val swing = maxElbowDisplacementThisRep
        
        // Reset per-rep state
        maxElbowDisplacementThisRep = 0f

        return if (swing > torsoH * 0.25f) {
            CalibratingRepCounter.ValidationResult(false, "Rep not counted — stop swinging your elbow")
        } else {
            CalibratingRepCounter.ValidationResult(true)
        }
    }
}
