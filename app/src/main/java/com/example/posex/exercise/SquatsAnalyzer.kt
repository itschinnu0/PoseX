package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs

object SquatsAnalyzer {

    private const val MIN_CONFIDENCE = 0.5f
    private val repCounter = RepCounter(bottomThreshold = 100.0, topThreshold = 140.0)

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
            .mapNotNull { it?.inFrameLikelihood }
            .average()

        val rightConfidence = listOf(rightHip, rightKnee, rightAnkle, rightShoulder)
            .mapNotNull { it?.inFrameLikelihood }
            .average()

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
            return ExerciseAnalysisResult(cues, repCounter.getRepCount(), null)
        }

        kneeAngle = PoseUtils.calculateAngle(hip, knee, ankle)
        val reps = repCounter.updateReps(kneeAngle)

        // Depth issues — WARNING: directly affect rep validity
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

        // Torso lean — CRITICAL: forward lean under load risks lower back injury
        if (shoulder != null && shoulder.inFrameLikelihood > MIN_CONFIDENCE) {
            val shoulderHipHorizontalDiff = shoulder.position.x - hip.position.x

            if (abs(shoulderHipHorizontalDiff) > 60) {
                cues.add(FormCue(
                    "Keep your chest up, do not lean forward",
                    FormCue.Severity.CRITICAL
                ))
            } else if (abs(shoulderHipHorizontalDiff) < 20 && abs(shoulderHipHorizontalDiff) > 5) {
                cues.add(FormCue(
                    "Keep your chest up, do not lean backward",
                    FormCue.Severity.WARNING
                ))
            }
        }

        // Knee over toe — CRITICAL: knee past toe under load risks knee injury
        val kneeAnkleHorizontalDiff = abs(knee.position.x - ankle.position.x)
        if (kneeAnkleHorizontalDiff > 80) {
            cues.add(FormCue(
                "Do not let your knees go past your toes",
                FormCue.Severity.CRITICAL
            ))
        }

        if (cues.isEmpty()) {
            cues.add(FormCue("Good form, keep going", FormCue.Severity.SUCCESS))
        }

        return ExerciseAnalysisResult(cues, reps, kneeAngle)
    }
}