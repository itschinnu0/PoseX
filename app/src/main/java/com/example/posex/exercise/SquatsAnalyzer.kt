package com.example.posex.exercise

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs

object SquatsAnalyzer {

    private const val MIN_CONFIDENCE = 0.5f

    private val repCounter = CalibratingRepCounter(
        fallbackBottom = 100.0,
        fallbackTop = 140.0
    )

    // ── Baseline stabilisation constants ─────────────────────────────────

    // User must be this extended before we consider them "at top".
    // 160° is more conservative than 150° — prevents baseline capture
    // while the user is still mid-rise.
    private const val AT_TOP_ANGLE_THRESHOLD = 160.0

    // Baseline only updates after this many consecutive frames above
    // AT_TOP_ANGLE_THRESHOLD. Prevents a single noisy frame from corrupting
    // the baseline on the way up.
    private const val BASELINE_STABILITY_FRAMES = 3

    // Minimum absGap that is physically plausible when standing.
    // If absGap drops below this while kneeAngle > AT_TOP_ANGLE_THRESHOLD,
    // the frame is a bad ML Kit reading — discard it entirely.
    private const val MIN_PLAUSIBLE_STANDING_GAP = 35f

    // Minimum plausible knee angle. Anything below this while supposedly
    // standing is a garbage landmark — discard the frame.
    private const val MIN_PLAUSIBLE_KNEE_ANGLE = 20.0

    // ── Leg raise detection constants ─────────────────────────────────────
    private const val HIP_KNEE_SHRINK_THRESHOLD = 15f
    private const val HIP_DROP_THRESHOLD = 10f

    // ── State ─────────────────────────────────────────────────────────────

    // Confirmed stable baseline after BASELINE_STABILITY_FRAMES clean frames
    private var standingAbsGap: Float? = null
    private var hipYAtStanding: Float? = null
    private var hasValidBaseline: Boolean = false
    private var standingTorsoLength: Float? = null

    // Frames accumulated toward next baseline update
    private var consecutiveTopFrames: Int = 0
    private var accumulatedGap: Float = 0f
    private var accumulatedHipY: Float = 0f
    private var accumulatedTorsoLength: Float = 0f

    // Per-rep descent tracking — reset only inside validator
    private var minAbsGapThisRep: Float = Float.MAX_VALUE
    private var maxHipDisplacementThisRep: Float = 0f

    // Whether user was at top on previous frame
    private var wasAtTop: Boolean = false

    init {
        repCounter.movementValidator = ::validateNotLegRaise
    }

    fun resetRepCounter() {
        repCounter.reset()
        standingAbsGap = null
        hipYAtStanding = null
        hasValidBaseline = false
        standingTorsoLength = null
        consecutiveTopFrames = 0
        accumulatedGap = 0f
        accumulatedHipY = 0f
        accumulatedTorsoLength = 0f
        minAbsGapThisRep = Float.MAX_VALUE
        maxHipDisplacementThisRep = 0f
        wasAtTop = false
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
            // FIX: Abort the active rep state so hallucinated exit frames don't count
            repCounter.abortCurrentRep()
            minAbsGapThisRep = Float.MAX_VALUE
            maxHipDisplacementThisRep = 0f

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
        val currentAbsGap = abs(hip.position.y - knee.position.y)
        val currentHipY = hip.position.y

        // ── Sanity check — reject physically impossible ML Kit frames ─────
        // kneeAngle < MIN_PLAUSIBLE_KNEE_ANGLE while apparently standing =
        // garbage landmark. Skip all gap/baseline logic for this frame.
        val isAtTop = kneeAngle > AT_TOP_ANGLE_THRESHOLD

        val isPlausibleFrame = !(isAtTop &&
                (currentAbsGap < MIN_PLAUSIBLE_STANDING_GAP ||
                        kneeAngle < MIN_PLAUSIBLE_KNEE_ANGLE))

        if (isPlausibleFrame) {
            when {
                // ── At top: accumulate frames toward stable baseline ───────
                isAtTop -> {
                    consecutiveTopFrames++
                    accumulatedGap += currentAbsGap
                    accumulatedHipY += currentHipY
                    accumulatedTorsoLength += if (shoulder != null)
                        abs(shoulder.position.y - hip.position.y) else 100f

                    if (consecutiveTopFrames >= BASELINE_STABILITY_FRAMES) {
                        // Use average over the stability window — smooths noise
                        val avgGap = accumulatedGap / consecutiveTopFrames
                        val avgHipY = accumulatedHipY / consecutiveTopFrames
                        val avgTorso = accumulatedTorsoLength / consecutiveTopFrames

                        // Only update baseline on the !top → top transition
                        // (first time we accumulate enough frames after a descent)
                        if (!wasAtTop || standingAbsGap == null) {
                            standingAbsGap = avgGap
                            hipYAtStanding = avgHipY
                            standingTorsoLength = avgTorso
                            hasValidBaseline = true
                        }

                        // Reset accumulators so they don't keep growing
                        consecutiveTopFrames = 0
                        accumulatedGap = 0f
                        accumulatedHipY = 0f
                        accumulatedTorsoLength = 0f
                    }
                }

                // ── During descent: track minimum gap and hip displacement ─
                !isAtTop && hasValidBaseline -> {
                    consecutiveTopFrames = 0
                    accumulatedGap = 0f
                    accumulatedHipY = 0f

                    if (currentAbsGap < minAbsGapThisRep) {
                        minAbsGapThisRep = currentAbsGap
                    }
                    val hipDisplacement = abs(currentHipY - (hipYAtStanding ?: currentHipY))
                    if (hipDisplacement > maxHipDisplacementThisRep) {
                        maxHipDisplacementThisRep = hipDisplacement
                    }
                }
            }
            wasAtTop = isAtTop
        }

        // Debug log — remove after confirming consistent rep counting
        val baseline = standingAbsGap ?: -1f
        val shrinkage = if (baseline > 0 && minAbsGapThisRep != Float.MAX_VALUE)
            baseline - minAbsGapThisRep else 0f
        android.util.Log.d("SquatDebug",
            "kneeAngle=${"%.1f".format(kneeAngle)} " +
                    "absGap=${"%.1f".format(currentAbsGap)} " +
                    "baseline=${"%.1f".format(baseline)} " +
                    "shrinkage=${"%.1f".format(shrinkage)} " +
                    "hipDisp=${"%.1f".format(maxHipDisplacementThisRep)} " +
                    "topFrames=$consecutiveTopFrames " +
                    "plausible=$isPlausibleFrame " +
                    "phase=${repCounter.phase} " +
                    "reps=${repCounter.getRepCount()}"
        )

        // ── Form checks ───────────────────────────────────────────────────

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

        if (abs(knee.position.x - ankle.position.x) > 80) {
            cues.add(FormCue(
                "Do not let your knees go past your toes",
                FormCue.Severity.CRITICAL
            ))
        }

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

        val formValid = cues.none { it.severity == FormCue.Severity.CRITICAL }

        if (repCounter.isCalibrating()) {
            cues.add(FormCue(
                "Perform one full squat to calibrate",
                FormCue.Severity.INFO
            ))
        }

        val reps = repCounter.updateReps(kneeAngle, formValid)

        if (repCounter.lastRepRejected) {
            cues.add(FormCue(repCounter.rejectionReason, FormCue.Severity.CRITICAL))
        }

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

    // ── Movement validator ────────────────────────────────────────────────

    private fun validateNotLegRaise(): CalibratingRepCounter.ValidationResult {
        if (!hasValidBaseline) {
            return CalibratingRepCounter.ValidationResult(
                passed = false,
                reason = "Stand still at the top for a moment to calibrate form"
            )
        }

        val baseline = standingAbsGap ?: return CalibratingRepCounter.ValidationResult(true)
        val torsoLength = standingTorsoLength ?: 100f

        val gapShrinkage = if (minAbsGapThisRep != Float.MAX_VALUE)
            baseline - minAbsGapThisRep else 0f
        val hipDisp = maxHipDisplacementThisRep

        // Reset AFTER reading
        minAbsGapThisRep = Float.MAX_VALUE
        maxHipDisplacementThisRep = 0f

        // FIX: Dynamic drop threshold. Hips must drop by at least 35% of the user's torso length.
        val dynamicHipDropThreshold = torsoLength * 0.35f

        return when {
            gapShrinkage >= HIP_KNEE_SHRINK_THRESHOLD && hipDisp >= dynamicHipDropThreshold ->
                CalibratingRepCounter.ValidationResult(true)

            gapShrinkage >= HIP_KNEE_SHRINK_THRESHOLD && hipDisp < dynamicHipDropThreshold ->
                CalibratingRepCounter.ValidationResult(
                    passed = false,
                    reason = "Rep not counted — lower your hips, don't just raise your knee"
                )

            else ->
                CalibratingRepCounter.ValidationResult(
                    passed = false,
                    reason = "Rep not counted — bend your knees and lower your hips"
                )
        }
    }
}