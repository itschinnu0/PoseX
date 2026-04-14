package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object SquatsAnalyzer {

    private const val MIN_CONFIDENCE = 0.5f

    fun analyze(pose: Pose): List<String> {
        val feedback = mutableListOf<String>()

        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)

        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        // Detect which side is more visible
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

        // Confidence check on selected side
        if (hip == null || knee == null || ankle == null ||
            hip.inFrameLikelihood < MIN_CONFIDENCE ||
            knee.inFrameLikelihood < MIN_CONFIDENCE ||
            ankle.inFrameLikelihood < MIN_CONFIDENCE
        ) {
            feedback.add("Move into frame so your full body is visible from the side")
            return feedback
        }

        // Knee angle — primary squat metric
        val kneeAngle = calculateAngle(hip, knee, ankle)

        when {
            kneeAngle > 160 -> feedback.add("Go lower, bend your knees more")
            kneeAngle < 60 -> feedback.add("You are too low, come up slightly")
        }

        // Torso upright check — shoulder should be above hip vertically
        if (shoulder != null && shoulder.inFrameLikelihood > MIN_CONFIDENCE) {
            val torsoAngle = Math.toDegrees(
                atan2(
                    (hip.position.y - shoulder.position.y).toDouble(),
                    (hip.position.x - shoulder.position.x).toDouble()
                )
            )
            // From side view, torso should be roughly vertical (angle near 90 degrees)
            if (abs(torsoAngle) < 60) {
                feedback.add("Keep your chest up, do not lean forward")
            }
        }

        // Knee over toe check — knee should not go too far past ankle horizontally
        val kneeAnkleHorizontalDiff = abs(knee.position.x - ankle.position.x)
        if (kneeAnkleHorizontalDiff > 80) {
            feedback.add("Do not let your knees go past your toes")
        }

        if (feedback.isEmpty()) {
            feedback.add("Good form, keep going")
        }

        return feedback
    }

    private fun calculateAngle(
        first: PoseLandmark,
        mid: PoseLandmark,
        last: PoseLandmark
    ): Double {
        val ax = first.position.x - mid.position.x
        val ay = first.position.y - mid.position.y
        val bx = last.position.x - mid.position.x
        val by = last.position.y - mid.position.y

        val dot = ax * bx + ay * by
        val magA = sqrt((ax * ax + ay * ay).toDouble())
        val magB = sqrt((bx * bx + by * by).toDouble())

        if (magA == 0.0 || magB == 0.0) return 0.0

        val cosAngle = (dot / (magA * magB)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(Math.acos(cosAngle))
    }
}