package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs

object SquatsAnalyzer {

    private const val MIN_CONFIDENCE = 0.5f

    // Fallback thresholds used if calibration range is too small.
    // Also used as the calibration detection anchors.
    private val repCounter = CalibratingRepCounter(
        fallbackBottom = 100.0,
        fallbackTop = 140.0
    )

    fun resetRepCounter() {
        repCounter.reset()
    }

    fun analyze(pose: Pose): ExerciseAnalysisResult {
        val cues = mutableListOf<FormCue>()
        var kneeAngle: Double? = null

        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)

        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val leftConfidence = listOf(leftHip, leftKnee, leftAnkle, leftShoulder)
            .mapNotNull { it?.inFrameLikelihood }.average()
        val rightConfidence = listOf(rightHip, rightKnee, rightAnkle, rightShoulder)
            .mapNotNull { it?.inFrameLikelihood }.average()

        val usLeft = leftConfidence >= rightConfidence

        val hip = if (usLeft) leftHip else rightHip
        val knee = if (usLeft) leftKnee else rightKnee
        val ankle = if (usLeft) leftAnkle else rightAnkle
        val shoulder = if (usLeft) leftShoulder else rightShoulder

        if (hip == null || knee == null || ankle == null ||
            hip.inFrameLikelihood < MIN_CONFIDENCE ||
            knee.inFrameLikelihood < MIN_CONFIDENCE ||
            ankle.inFrameLikelihood < MIN_CONFIDENCE
        ) {
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

        kneeAngle = PoseUtils.calculateAngle(hip, knee, ankle)

        // ── Form checks — done BEFORE updateReps so formValid reflects ────
        // the current frame accurately when passed to the counter

        // Torso lean — CRITICAL
        if (shoulder != null && shoulder.inFrameLikelihood > MIN_CONFIDENCE) {
            val diff = shoulder.position.x - hip.position.x
            if (abs(diff) > 60) {
                cues.add(FormCue(
                    "Keep your chest up, do not lean forward",
                    FormCue.Severity.CRITICAL
                ))
            } else if (abs(diff) < 20 && abs(diff) > 5) {
                cues.add(FormCue(
                    "Keep your chest up, do not lean backward",
                    FormCue.Severity.WARNING
                ))
            }
        }

        // Knee over toe — CRITICAL
        val kneeAnkleDiff = abs(knee.position.x - ankle.position.x)
        if (kneeAnkleDiff > 80) {
            cues.add(FormCue(
                "Do not let your knees go past your toes",
                FormCue.Severity.CRITICAL
            ))
        }

        // Depth — WARNING (not a safety issue, a quality issue)
        when {
            kneeAngle > 160 -> cues.add(FormCue(
                "Go lower, bend your knees more",
                FormCue.Severity.WARNING
            ))
            kneeAngle < 60 -> cues.add(FormCue(
                "You are too low, come up slightly",
                FormCue.Severity.WARNING
            ))
        }

        // formValid = no CRITICAL cues this frame
        val formValid = cues.none { it.severity == FormCue.Severity.CRITICAL }

        // Show calibration hint during first rep
        if (repCounter.isCalibrating()) {
            cues.add(FormCue(
                "Perform one full squat to calibrate",
                FormCue.Severity.INFO
            ))
        }

        val reps = repCounter.updateReps(kneeAngle, formValid)

        if (cues.none { it.severity != FormCue.Severity.INFO } && !repCounter.lastRepRejected) {
            cues.add(FormCue("Good form, keep going", FormCue.Severity.SUCCESS))
        }

        return ExerciseAnalysisResult(
            cues = cues,
            repCount = reps,
            metricValue = kneeAngle,
            isCalibrating = repCounter.isCalibrating(),
            repRejected = repCounter.lastRepRejected,
            rejectionReason = repCounter.rejectionReason
        )
    }
}