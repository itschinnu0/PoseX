package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.hypot

object PushupAnalyzer {

    private const val MIN_CONFIDENCE = 0.5f

    private val repCounter = CalibratingRepCounter(
        fallbackBottom = 60.0,
        fallbackTop = 140.0
    )

    fun resetRepCounter() {
        repCounter.reset()
    }

    fun analyze(pose: Pose): ExerciseAnalysisResult {
        val cues = mutableListOf<FormCue>()

        // 1. Extract all landmarks
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        // 2. Side Selection (Handle Occlusion)
        val leftConfidence = listOf(leftShoulder, leftElbow, leftWrist, leftHip, leftKnee, leftAnkle)
            .mapNotNull { it?.inFrameLikelihood }.average()
        val rightConfidence = listOf(rightShoulder, rightElbow, rightWrist, rightHip, rightKnee, rightAnkle)
            .mapNotNull { it?.inFrameLikelihood }.average()

        val isLeftVisible = leftConfidence >= rightConfidence

        val shoulder = if (isLeftVisible) leftShoulder else rightShoulder
        val elbow = if (isLeftVisible) leftElbow else rightElbow
        val wrist = if (isLeftVisible) leftWrist else rightWrist
        val hip = if (isLeftVisible) leftHip else rightHip
        val knee = if (isLeftVisible) leftKnee else rightKnee
        val ankle = if (isLeftVisible) leftAnkle else rightAnkle

        // 3. Safe Screen & Confidence Check
        if (shoulder == null || elbow == null || wrist == null || hip == null || knee == null || ankle == null ||
            shoulder.inFrameLikelihood < MIN_CONFIDENCE ||
            elbow.inFrameLikelihood < MIN_CONFIDENCE ||
            wrist.inFrameLikelihood < MIN_CONFIDENCE ||
            knee.inFrameLikelihood < MIN_CONFIDENCE
        ) {
            repCounter.abortCurrentRep()
            cues.add(FormCue(
                "Move into frame so your full body is visible from the side",
                FormCue.Severity.INFO
            ))
            return ExerciseAnalysisResult(
                cues = cues,
                repCount = repCounter.getRepCount(),
                metricValue = null,
                isCalibrating = repCounter.isCalibrating()
            )
        }

        // Calculate Torso Length for dynamic scaling
        val dx = shoulder.position.x - hip.position.x
        val dy = shoulder.position.y - hip.position.y
        val torsoLength = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val safeTorsoLength = torsoLength.coerceAtLeast(100f)

        // 4. Core Angle Calculations
        val elbowAngle = PoseUtils.calculateAngle(shoulder, elbow, wrist)

        // Detect Knee Push-up vs Standard Push-up
        val kneeToAnkleVerticalDistance = ankle.position.y - knee.position.y
        val isKneePushup = kneeToAnkleVerticalDistance < (safeTorsoLength * 0.2f)

        val bodyAngle = if (isKneePushup) {
            PoseUtils.calculateAngle(shoulder, hip, knee)
        } else {
            PoseUtils.calculateAngle(shoulder, hip, ankle)
        }

        // 5. Form Checks (Dynamic Math, no hardcoded pixels)

        // Body sag/pike — CRITICAL
        bodyLineCue(bodyAngle)?.let { cues.add(it) }

        // Hand Placement (Forearm verticality) — WARNING
        if (elbowAngle > 150) {
            val wristToShoulderOffset = abs(wrist.position.x - shoulder.position.x)
            val maxOffsetThreshold = safeTorsoLength * 0.35f
            if (wristToShoulderOffset > maxOffsetThreshold) {
                cues.add(FormCue(
                    "Move your hands back, place them directly under your shoulders",
                    FormCue.Severity.WARNING
                ))
            }
        }

        // Range of motion — WARNING
        when {
            elbowAngle > 165 && !repCounter.isCalibrating() -> cues.add(FormCue(
                "Lower your chest more",
                FormCue.Severity.WARNING
            ))
            elbowAngle < 45 -> cues.add(FormCue(
                "Push all the way up",
                FormCue.Severity.WARNING
            ))
        }

        val formValid = cues.none { it.severity == FormCue.Severity.CRITICAL }

        if (repCounter.isCalibrating()) {
            cues.add(FormCue(
                "Perform one full push-up to calibrate",
                FormCue.Severity.INFO
            ))
        }

        // 6. Update State
        val reps = repCounter.updateReps(elbowAngle, formValid)

        if (repCounter.lastRepRejected) {
            cues.add(FormCue(repCounter.rejectionReason, FormCue.Severity.CRITICAL))
        }

        if (cues.none { it.severity != FormCue.Severity.INFO } && !repCounter.lastRepRejected) {
            cues.add(FormCue("Good form, keep going", FormCue.Severity.SUCCESS))
        }

        return ExerciseAnalysisResult(
            cues = cues,
            repCount = reps,
            metricValue = elbowAngle,
            isCalibrating = repCounter.isCalibrating(),
            repRejected = repCounter.lastRepRejected,
            rejectionReason = repCounter.rejectionReason
        )
    }

    internal fun bodyLineCue(bodyAngle: Double): FormCue? {
        return when {
            bodyAngle < 150 -> FormCue(
                "Lower your hips, your body is piking up",
                FormCue.Severity.CRITICAL
            )
            bodyAngle > 185 || bodyAngle < 90 -> FormCue(
                "Lift your hips, your core is sagging",
                FormCue.Severity.CRITICAL
            )
            else -> null
        }
    }
}