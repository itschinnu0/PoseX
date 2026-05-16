package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs

object PlankAnalyzer {

    private const val MIN_CONFIDENCE = 0.5f
    private var holdStartTime: Long? = null
    private var totalHoldSeconds = 0

    fun resetRepCounter() {
        holdStartTime = null
        totalHoldSeconds = 0
    }

    fun analyze(pose: Pose): ExerciseAnalysisResult {
        val cues = mutableListOf<FormCue>()

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)

        val keyLandmarks = listOf(
            leftShoulder, rightShoulder,
            leftHip, rightHip,
            leftAnkle, rightAnkle
        )

        if (keyLandmarks.any { it == null || it.inFrameLikelihood < MIN_CONFIDENCE }) {
            holdStartTime = null
            cues.add(FormCue(
                "Move into frame so your full body is visible",
                FormCue.Severity.INFO
            ))
            return ExerciseAnalysisResult(cues, 0, null, totalHoldSeconds)
        }

        val shoulderMidY = (leftShoulder!!.position.y + rightShoulder!!.position.y) / 2
        val shoulderMidX = (leftShoulder.position.x + rightShoulder.position.x) / 2
        val hipMidY = (leftHip!!.position.y + rightHip!!.position.y) / 2
        val hipMidX = (leftHip.position.x + rightHip.position.x) / 2
        val ankleMidY = (leftAnkle!!.position.y + rightAnkle!!.position.y) / 2
        val ankleMidX = (leftAnkle.position.x + rightAnkle.position.x) / 2

        val expectedHipY = shoulderMidY + (ankleMidY - shoulderMidY) *
                ((hipMidX - shoulderMidX) / (ankleMidX - shoulderMidX + 0.001f))

        val hipDeviation = (hipMidY - expectedHipY)
        val isGoodForm = hipDeviation in -40.0..40.0

        if (isGoodForm) {
            if (holdStartTime == null) holdStartTime = System.currentTimeMillis()
            totalHoldSeconds = ((System.currentTimeMillis() - holdStartTime!!) / 1000).toInt()
        } else {
            holdStartTime = null
        }

        // Hip sag/pike — CRITICAL: spinal compression/strain risk
        when {
            hipDeviation > 40 -> cues.add(FormCue(
                "Lift your hips, your body is sagging down",
                FormCue.Severity.CRITICAL
            ))
            hipDeviation < -40 -> cues.add(FormCue(
                "Lower your hips, your body is too high",
                FormCue.Severity.CRITICAL
            ))
        }

        // Elbow position — WARNING: shoulder impingement risk over a long hold
        if (leftElbow != null && rightElbow != null &&
            leftElbow.inFrameLikelihood > MIN_CONFIDENCE &&
            rightElbow.inFrameLikelihood > MIN_CONFIDENCE
        ) {
            val leftShoulderElbowDiff = abs(leftShoulder.position.x - leftElbow.position.x)
            val rightShoulderElbowDiff = abs(rightShoulder.position.x - rightElbow.position.x)

            if (leftShoulderElbowDiff > 50 || rightShoulderElbowDiff > 50) {
                cues.add(FormCue(
                    "Place your elbows directly under your shoulders",
                    FormCue.Severity.WARNING
                ))
            }
        }

        if (cues.isEmpty()) {
            cues.add(FormCue("Good form, hold steady", FormCue.Severity.SUCCESS))
        }

        return ExerciseAnalysisResult(cues, 0, hipDeviation.toDouble(), totalHoldSeconds)
    }
}