    package com.example.posex.exercise

    import com.google.mlkit.vision.pose.Pose
    import com.google.mlkit.vision.pose.PoseLandmark
    import kotlin.math.abs
    import kotlin.math.hypot

    object PlankAnalyzer {

        private const val MIN_CONFIDENCE = 0.5f

        private const val MIN_GOOD_ANGLE = 155.0
        private const val MAX_GOOD_ANGLE = 195.0

        private var targetHoldSeconds: Int = 30

        private val timer = ActiveHoldTimer()
        private var isTerminated: Boolean = false
        private var consecutiveKneeDropFrames: Int = 0

        fun setTargetHoldSeconds(seconds: Int) {
            targetHoldSeconds = seconds
        }

        fun resetRepCounter() {
            timer.reset()
            isTerminated = false
            consecutiveKneeDropFrames = 0
        }

        fun analyze(pose: Pose): ExerciseAnalysisResult {
            val cues = mutableListOf<FormCue>()
            val holdSeconds = (timer.accumulatedTimeMillis.coerceAtLeast(0L) / 1000).toInt()

            val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
            val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
            val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
            val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)

            val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
            val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
            val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
            val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
            val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)

            val leftConfidence = listOf(leftShoulder, leftHip, leftKnee, leftAnkle)
                .mapNotNull { it?.inFrameLikelihood }.average()
            val rightConfidence = listOf(rightShoulder, rightHip, rightKnee, rightAnkle)
                .mapNotNull { it?.inFrameLikelihood }.average()

            val useLeft = leftConfidence >= rightConfidence

            val shoulder = if (useLeft) leftShoulder else rightShoulder
            val hip = if (useLeft) leftHip else rightHip
            val knee = if (useLeft) leftKnee else rightKnee
            val ankle = if (useLeft) leftAnkle else rightAnkle
            val elbow = if (useLeft) leftElbow else rightElbow

            if (isTerminated) {
                cues.add(FormCue(
                    "Knees dropped to the floor — exercise ended",
                    FormCue.Severity.CRITICAL
                ))
                return ExerciseAnalysisResult(
                    cues = cues,
                    repCount = 0,
                    metricValue = null,
                    holdDurationSeconds = holdSeconds,
                    exerciseCompleted = true
                )
            }

            if (shoulder == null || hip == null || knee == null || ankle == null ||
                shoulder.inFrameLikelihood < MIN_CONFIDENCE ||
                hip.inFrameLikelihood < MIN_CONFIDENCE ||
                knee.inFrameLikelihood < MIN_CONFIDENCE ||
                ankle.inFrameLikelihood < MIN_CONFIDENCE
            ) {
                timer.forcePause()
                cues.add(FormCue("Full body must be visible", FormCue.Severity.INFO))
                return ExerciseAnalysisResult(
                    cues = cues,
                    repCount = 0,
                    metricValue = null,
                    holdDurationSeconds = holdSeconds
                )
            }

            val dx = shoulder.position.x - hip.position.x
            val dy = shoulder.position.y - hip.position.y
            val torsoLength = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            val safeTorsoLength = torsoLength.coerceAtLeast(100f)

            val minElevationThreshold = safeTorsoLength * 0.15f
            val kneeDropThreshold = safeTorsoLength * 0.10f
            val maxElbowOffsetThreshold = safeTorsoLength * 0.25f

            if (elbow != null && elbow.inFrameLikelihood >= MIN_CONFIDENCE) {
                val armHeight = elbow.position.y - shoulder.position.y
                if (armHeight < minElevationThreshold) {
                    timer.forcePause()
                    cues.add(FormCue("Lift your body off the floor", FormCue.Severity.CRITICAL))
                    return ExerciseAnalysisResult(
                        cues = cues,
                        repCount = 0,
                        metricValue = null,
                        holdDurationSeconds = holdSeconds
                    )
                }

                val elbowOffset = abs(shoulder.position.x - elbow.position.x)
                if (elbowOffset > maxElbowOffsetThreshold) {
                    cues.add(FormCue(
                        "Keep your elbows directly under your shoulders",
                        FormCue.Severity.WARNING
                    ))
                }
            }

            val kneeToGround = ankle.position.y - knee.position.y
            val kneeNearAnkle = abs(knee.position.y - ankle.position.y) <
                    abs(knee.position.y - hip.position.y)
            val isKneeDropped = kneeToGround < kneeDropThreshold || kneeNearAnkle

            if (timer.isTimerRunning && isKneeDropped) {
                consecutiveKneeDropFrames++
            } else {
                consecutiveKneeDropFrames = 0
            }

            if (consecutiveKneeDropFrames >= 5) {
                timer.forcePause()
                isTerminated = true
                cues.add(FormCue(
                    "Knees dropped to the floor — exercise ended",
                    FormCue.Severity.CRITICAL
                ))
                return ExerciseAnalysisResult(
                    cues = cues,
                    repCount = 0,
                    metricValue = null,
                    holdDurationSeconds = holdSeconds,
                    exerciseCompleted = true
                )
            }

            val kneeAngle = PoseUtils.calculateAngle(shoulder, hip, ankle)

            val expectedHipY = shoulder.position.y +
                    (ankle.position.y - shoulder.position.y) *
                    ((hip.position.x - shoulder.position.x) /
                            (ankle.position.x - shoulder.position.x + 0.001f))
            val hipsSagging = hip.position.y > expectedHipY


            val hipsTooHigh = kneeAngle < MIN_GOOD_ANGLE
            val hipsTooLow = kneeAngle > MAX_GOOD_ANGLE || hipsSagging

            val isFormValid = !hipsTooHigh && !hipsTooLow
            timer.processFrame(isFormValid)

            if (!timer.isTimerRunning && !isFormValid) {
                when {
                    hipsTooHigh -> cues.add(FormCue(
                        "Lower your hips to a straight line",
                        FormCue.Severity.CRITICAL
                    ))
                    hipsTooLow -> cues.add(FormCue(
                        "Raise your hips, engage your core",
                        FormCue.Severity.CRITICAL
                    ))
                }
            }

            if (cues.isEmpty()) {
                cues.add(FormCue("Good form, hold steady", FormCue.Severity.SUCCESS))
            }

            if (holdSeconds >= targetHoldSeconds) {
                cues.add(FormCue("Hold complete — great job!", FormCue.Severity.SUCCESS))
                return ExerciseAnalysisResult(
                    cues = cues,
                    repCount = 0,
                    metricValue = kneeAngle,
                    holdDurationSeconds = holdSeconds,
                    exerciseCompleted = true
                )
            }

            return ExerciseAnalysisResult(
                cues = cues,
                repCount = 0,
                metricValue = kneeAngle,
                holdDurationSeconds = holdSeconds
            )
        }
    }

