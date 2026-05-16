package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs

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
            cues.add(FormCue(
                "Move into frame so your upper body is fully visible",
                FormCue.Severity.INFO
            ))
            return ExerciseAnalysisResult(
                cues = cues,
                repCount = repCounter.getRepCount(),
                metricValue = null,
                isCalibrating = repCounter.isCalibrating()
            )
        }

        val leftElbowAngle = PoseUtils.calculateAngle(leftShoulder!!, leftElbow!!, leftWrist!!)
        val rightElbowAngle = PoseUtils.calculateAngle(rightShoulder!!, rightElbow!!, rightWrist!!)
        val avgElbowAngle = (leftElbowAngle + rightElbowAngle) / 2

        // ── Form checks before updateReps ─────────────────────────────────

        // Body sag/pike — CRITICAL
        val shoulderMidY = (leftShoulder.position.y + rightShoulder.position.y) / 2
        val hipMidY = (leftHip!!.position.y + rightHip!!.position.y) / 2

        if (leftAnkle != null && rightAnkle != null &&
            leftAnkle.inFrameLikelihood > MIN_CONFIDENCE &&
            rightAnkle.inFrameLikelihood > MIN_CONFIDENCE
        ) {
            val ankleMidY = (leftAnkle.position.y + rightAnkle.position.y) / 2
            val bodyLineDeviation = hipMidY - ((shoulderMidY + ankleMidY) / 2)
            when {
                bodyLineDeviation > 40 -> cues.add(FormCue(
                    "Lift your hips, your body is sagging",
                    FormCue.Severity.CRITICAL
                ))
                bodyLineDeviation < -40 -> cues.add(FormCue(
                    "Lower your hips, your body is piking up",
                    FormCue.Severity.CRITICAL
                ))
            }
        }

        // Range of motion — WARNING
        when {
            avgElbowAngle > 160 -> cues.add(FormCue(
                "Lower your chest, bend your elbows more",
                FormCue.Severity.WARNING
            ))
            avgElbowAngle < 45 -> cues.add(FormCue(
                "Push up, do not collapse your arms",
                FormCue.Severity.WARNING
            ))
        }

        // Asymmetry — WARNING
        if (abs(leftElbowAngle - rightElbowAngle) > 15) {
            cues.add(FormCue(
                "Keep both arms even, one side is lower than the other",
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

        val reps = repCounter.updateReps(avgElbowAngle, formValid)

        if (cues.none { it.severity != FormCue.Severity.INFO } && !repCounter.lastRepRejected) {
            cues.add(FormCue("Good form, keep going", FormCue.Severity.SUCCESS))
        }

        return ExerciseAnalysisResult(
            cues = cues,
            repCount = reps,
            metricValue = avgElbowAngle,
            isCalibrating = repCounter.isCalibrating(),
            repRejected = repCounter.lastRepRejected,
            rejectionReason = repCounter.rejectionReason
        )
    }
}