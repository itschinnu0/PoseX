package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object SquatAnalyzer {

    private const val MIN_CONFIDENCE = 0.5f

    fun analyze(pose: Pose): List<String> {
        val feedback = mutableListOf<String>()

        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        // Confidence check — if key landmarks are not visible, don't give feedback
        val keyLandmarks = listOf(leftHip, leftKnee, leftAnkle, rightHip, rightKnee, rightAnkle)
        if (keyLandmarks.any { it == null || it.inFrameLikelihood < MIN_CONFIDENCE }) {
            feedback.add("Move into frame so your full body is visible")
            return feedback
        }

        // Knee angle check
        val leftKneeAngle = calculateAngle(leftHip!!, leftKnee!!, leftAnkle!!)
        val rightKneeAngle = calculateAngle(rightHip!!, rightKnee!!, rightAnkle!!)
        val avgKneeAngle = (leftKneeAngle + rightKneeAngle) / 2

        when {
            avgKneeAngle > 160 -> feedback.add("Go lower, bend your knees more")
            avgKneeAngle < 60 -> feedback.add("You are too low, come up slightly")
        }

        // Knee symmetry check
        if (abs(leftKneeAngle - rightKneeAngle) > 15) {
            feedback.add("Balance your weight evenly on both legs")
        }

        // Torso upright check using hip and shoulder
        if (leftShoulder != null && rightShoulder != null &&
            leftShoulder.inFrameLikelihood > MIN_CONFIDENCE &&
            rightShoulder.inFrameLikelihood > MIN_CONFIDENCE
        ) {
            val hipMidY = (leftHip!!.position.y + rightHip!!.position.y) / 2
            val shoulderMidY = (leftShoulder.position.y + rightShoulder.position.y) / 2
            val hipMidX = (leftHip.position.x + rightHip.position.x) / 2
            val shoulderMidX = (leftShoulder.position.x + rightShoulder.position.x) / 2

            val torsoAngle = Math.toDegrees(
                atan2(
                    (hipMidY - shoulderMidY).toDouble(),
                    (hipMidX - shoulderMidX).toDouble()
                )
            )

            if (abs(torsoAngle) < 60) {
                feedback.add("Keep your chest up, do not lean forward")
            }
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
