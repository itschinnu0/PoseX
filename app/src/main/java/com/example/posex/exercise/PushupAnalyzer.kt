package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.sqrt

object PushupAnalyzer {

    private const val MIN_CONFIDENCE = 0.5f
    private val repCounter = RepCounter()

    fun resetRepCounter() {
        repCounter.reset()
    }

    fun analyze(pose: Pose): SquatAnalysisResult {
        val feedback = mutableListOf<String>()

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val keyLandmarks = listOf(
            leftShoulder, leftElbow, leftWrist,
            rightShoulder, rightElbow, rightWrist,
            leftHip, rightHip
        )

        if (keyLandmarks.any { it == null || it.inFrameLikelihood < MIN_CONFIDENCE }) {
            feedback.add("Move into frame so your upper body is fully visible")
            return SquatAnalysisResult(feedback, repCounter.getRepCount(), null)
        }

        // Elbow angle check
        val leftElbowAngle = calculateAngle(leftShoulder!!, leftElbow!!, leftWrist!!)
        val rightElbowAngle = calculateAngle(rightShoulder!!, rightElbow!!, rightWrist!!)
        val avgElbowAngle = (leftElbowAngle + rightElbowAngle) / 2

        // Update rep counter based on elbow angle
        val reps = repCounter.updateReps(avgElbowAngle)

        when {
            avgElbowAngle > 160 -> feedback.add("Lower your chest, bend your elbows more")
            avgElbowAngle < 45 -> feedback.add("Push up, do not collapse your arms")
        }

        // Elbow symmetry
        if (abs(leftElbowAngle - rightElbowAngle) > 15) {
            feedback.add("Keep both arms even, one side is lower than the other")
        }

        // Body alignment check — hips should not sag or pike
        val shoulderMidY = (leftShoulder.position.y + rightShoulder.position.y) / 2
        val hipMidY = (leftHip!!.position.y + rightHip!!.position.y) / 2

        if (leftAnkle != null && rightAnkle != null &&
            leftAnkle.inFrameLikelihood > MIN_CONFIDENCE &&
            rightAnkle.inFrameLikelihood > MIN_CONFIDENCE
        ) {
            val ankleMidY = (leftAnkle.position.y + rightAnkle.position.y) / 2

            val bodyLineDeviation = hipMidY - ((shoulderMidY + ankleMidY) / 2)

            when {
                bodyLineDeviation > 40 -> feedback.add("Lift your hips, your body is sagging")
                bodyLineDeviation < -40 -> feedback.add("Lower your hips, your body is piking up")
            }
        }

        if (feedback.isEmpty()) {
            feedback.add("Good form, keep going")
        }

        return SquatAnalysisResult(feedback, reps, avgElbowAngle)
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