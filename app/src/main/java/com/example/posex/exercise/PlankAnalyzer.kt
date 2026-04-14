package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.sqrt

object PlankAnalyzer {

    private const val MIN_CONFIDENCE = 0.5f
    private var repCount = 0
    private var inGoodForm = false

    fun resetRepCounter() {
        repCount = 0
        inGoodForm = false
    }

    fun analyze(pose: Pose): SquatAnalysisResult {
        val feedback = mutableListOf<String>()

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
            feedback.add("Move into frame so your full body is visible")
            return SquatAnalysisResult(feedback, repCount, null)
        }

        val shoulderMidY = (leftShoulder!!.position.y + rightShoulder!!.position.y) / 2
        val shoulderMidX = (leftShoulder.position.x + rightShoulder.position.x) / 2
        val hipMidY = (leftHip!!.position.y + rightHip!!.position.y) / 2
        val hipMidX = (leftHip.position.x + rightHip.position.x) / 2
        val ankleMidY = (leftAnkle!!.position.y + rightAnkle!!.position.y) / 2
        val ankleMidX = (leftAnkle.position.x + rightAnkle.position.x) / 2

        // Body alignment — hip should sit on the line between shoulder and ankle
        val expectedHipY = shoulderMidY + (ankleMidY - shoulderMidY) *
                ((hipMidX - shoulderMidX) / (ankleMidX - shoulderMidX + 0.001f))

        val hipDeviation = hipMidY - expectedHipY

        // Track form changes: count when user returns to good form after bad form
        val isGoodForm = hipDeviation in -40.0..40.0
        if (isGoodForm && !inGoodForm) {
            repCount++
        }
        inGoodForm = isGoodForm

        when {
            hipDeviation > 40 -> feedback.add("Lift your hips, your body is sagging down")
            hipDeviation < -40 -> feedback.add("Lower your hips, your body is too high")
        }

        // Shoulder directly above elbow check
        if (leftElbow != null && rightElbow != null &&
            leftElbow.inFrameLikelihood > MIN_CONFIDENCE &&
            rightElbow.inFrameLikelihood > MIN_CONFIDENCE
        ) {
            val leftShoulderElbowDiff = abs(leftShoulder.position.x - leftElbow.position.x)
            val rightShoulderElbowDiff = abs(rightShoulder.position.x - rightElbow.position.x)

            if (leftShoulderElbowDiff > 50 || rightShoulderElbowDiff > 50) {
                feedback.add("Place your elbows directly under your shoulders")
            }
        }

        if (feedback.isEmpty()) {
            feedback.add("Good form, hold steady")
        }

        return SquatAnalysisResult(feedback, repCount, hipDeviation as Double?)
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