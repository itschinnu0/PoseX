package com.example.posex.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.posex.data.SessionRecord
import com.example.posex.data.StorageService
import com.example.posex.exercise.CuePrioritizer
import com.example.posex.exercise.ExerciseAnalysisResult
import com.example.posex.exercise.ExerciseType
import com.example.posex.exercise.FormCue
import com.example.posex.exercise.PlankAnalyzer
import com.example.posex.exercise.PoseReadinessChecker
import com.example.posex.exercise.PushupAnalyzer
import com.example.posex.exercise.SquatsAnalyzer
import com.example.posex.exercise.WorkoutConfig
import com.example.posex.exercise.WorkoutSession
import com.example.posex.exercise.WorkoutState
import com.example.posex.audio.PoseXTtsManager
import com.example.posex.ui.components.CameraPreview
import com.example.posex.ui.components.PoseOverlay
import com.google.mlkit.vision.pose.Pose
import java.util.UUID

@Composable
fun WorkoutScreen(
    exerciseType: ExerciseType,
    config: WorkoutConfig,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var imageWidth by remember { mutableStateOf(480) }
    var imageHeight by remember { mutableStateOf(640) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
        if (exerciseType == ExerciseType.PLANK) {
            PlankAnalyzer.setTargetHoldSeconds(config.holdSeconds)
        }
    }

    val targetHoldSeconds = if (exerciseType == ExerciseType.PLANK) {
        config.holdSeconds
    } else {
        0
    }

    var lastState by remember { mutableStateOf<WorkoutState?>(null) }
    var currentSetIndex by remember { mutableStateOf(1) }

    var currentPose by remember { mutableStateOf<Pose?>(null) }
    var feedbackText by remember { mutableStateOf("Get into position") }
    var feedbackColor by remember { mutableStateOf(Color(0xFFB0BEC5)) }
    var lastResult by remember { mutableStateOf<ExerciseAnalysisResult?>(null) }
    var workoutState by remember { mutableStateOf<WorkoutState>(WorkoutState.Idle) }

    // Shown briefly after a rep is rejected — cleared on next successful rep
    var rejectionText by remember { mutableStateOf("") }

    val ttsManager = remember { PoseXTtsManager(context) }
    val storageService = remember { StorageService(context) }
    val sessionId = remember { UUID.randomUUID().toString() }
    val sessionStartDate = remember { System.currentTimeMillis() }

    val warningFrameCounts = remember { mutableMapOf<String, Int>() }
    val warningFrameThreshold = 5

    val session = remember {
        WorkoutSession(
            scope = coroutineScope,
            targetReps = if (exerciseType == ExerciseType.PLANK) 0 else config.repsPerSet,
            totalSets = config.sets,
            restSeconds = config.restSeconds,
            onStateChanged = { newState ->
                workoutState = newState
                ttsManager.setWorkoutState(newState)
                if (newState is WorkoutState.Countdown) {
                    ttsManager.speakCountdown(newState.secondsRemaining)
                }
                if (newState !is WorkoutState.Active) {
                    warningFrameCounts.clear()
                    rejectionText = ""
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    fun resetAnalyzers() {
        when (exerciseType) {
            ExerciseType.SQUAT  -> SquatsAnalyzer.resetRepCounter()
            ExerciseType.PUSHUP -> PushupAnalyzer.resetRepCounter()
            ExerciseType.PLANK  -> PlankAnalyzer.resetRepCounter()
        }
    }

    LaunchedEffect(workoutState) {
        val previousState = lastState
        if (workoutState is WorkoutState.Rest) {
            resetAnalyzers()
            currentSetIndex = (workoutState as WorkoutState.Rest).currentSet
        } else if (workoutState is WorkoutState.WaitingForPose && previousState is WorkoutState.Rest) {
            currentSetIndex = previousState.currentSet + 1
        }
        lastState = workoutState
    }

    fun saveAndExit() {
        val durationMs = session.getSessionDurationMs()
        if (durationMs > 0L) {
            storageService.saveSession(
                SessionRecord(
                    id           = sessionId,
                    exerciseType = exerciseType.name,
                    date         = sessionStartDate,
                    repCount     = lastResult?.repCount ?: 0,
                    holdSeconds  = lastResult?.holdDurationSeconds ?: 0,
                    durationMs   = durationMs,
                    criticalCues = session.getCriticalCueCount(),
                    warningCues  = session.getWarningCueCount()
                )
            )
        }
        resetAnalyzers()
        session.stop()
        onExit()
    }

    fun handlePoseFrame(pose: Pose) {
        // ── WaitingForPose ────────────────────────────────────────────────
        if (session.isWaitingForPose()) {
            val readiness = PoseReadinessChecker.check(pose, exerciseType)
            session.updatePoseHint(readiness.hint)
            if (readiness.isReady) session.onPoseReady()
            return
        }

        if (!session.isActive()) return

        // ── Active ────────────────────────────────────────────────────────
        val result = when (exerciseType) {
            ExerciseType.SQUAT  -> SquatsAnalyzer.analyze(pose)
            ExerciseType.PUSHUP -> PushupAnalyzer.analyze(pose)
            ExerciseType.PLANK  -> PlankAnalyzer.analyze(pose)
        }

        lastResult = result

        if (result.exerciseCompleted) {
            if (exerciseType == ExerciseType.PLANK &&
                targetHoldSeconds > 0 &&
                result.holdDurationSeconds >= targetHoldSeconds
            ) {
                val completed = session.onHoldCompleted()
                if (completed) {
                    saveAndExit()
                }
                return
            }
            saveAndExit()
            return
        }

        // ── Rep rejected — show rejection reason, speak it, skip normal feedback
        if (result.repRejected) {
            rejectionText = result.rejectionReason
            feedbackText = result.rejectionReason
            feedbackColor = Color(0xFFFF5252)
            ttsManager.speakCue(result.rejectionReason)
            session.onRepUpdated(result.repCount)
            return
        }

        // ── Calibration hint — override feedback during first rep ─────────
        if (result.isCalibrating) {
            val calibrationHint = when (exerciseType) {
                ExerciseType.SQUAT  -> "Do one full squat to calibrate"
                ExerciseType.PUSHUP -> "Do one full push-up to calibrate"
                ExerciseType.PLANK  -> ""
            }
            if (calibrationHint.isNotEmpty()) {
                feedbackText = calibrationHint
                feedbackColor = Color(0xFFB0BEC5)
                // Don't speak calibration hint — it would be too noisy
                session.onRepUpdated(result.repCount)
                return
            }
        }

        // ── Normal feedback path ──────────────────────────────────────────
        rejectionText = "" // clear any previous rejection on a clean frame

        val topCue = CuePrioritizer.topCue(result.cues) ?: return

        feedbackText = topCue.message
        feedbackColor = when (topCue.severity) {
            FormCue.Severity.CRITICAL -> Color(0xFFFF5252)
            FormCue.Severity.WARNING  -> Color(0xFFFFB300)
            FormCue.Severity.INFO     -> Color(0xFFB0BEC5)
            FormCue.Severity.SUCCESS  -> Color(0xFF00E676)
        }

        when (topCue.severity) {
            FormCue.Severity.CRITICAL -> {
                warningFrameCounts.clear()
                session.recordCue(topCue.severity)
                ttsManager.speakCue(topCue.message)
            }
            FormCue.Severity.WARNING -> {
                val currentCount = (warningFrameCounts[topCue.message] ?: 0) + 1
                warningFrameCounts.clear()
                warningFrameCounts[topCue.message] = currentCount
                if (currentCount >= warningFrameThreshold) {
                    session.recordCue(topCue.severity)
                    ttsManager.speakCue(topCue.message)
                    warningFrameCounts[topCue.message] = 0
                }
            }
            else -> warningFrameCounts.clear()
        }

        val completed = session.onRepUpdated(result.repCount)
        if (completed) saveAndExit()
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0F1E)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Camera permission is required for PoseX to work.",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {

        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            lifecycleOwner = lifecycleOwner,
            onPoseDetected = { pose, width, height ->
                currentPose = pose
                imageWidth = width
                imageHeight = height
                handlePoseFrame(pose)
            },
            onError = { error ->
                feedbackText = "Camera error: ${error.message}"
            }
        )

        PoseOverlay(
            pose = currentPose,
            modifier = Modifier.fillMaxSize(),
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )

        // ── Countdown overlay ─────────────────────────────────────────────
        if (workoutState is WorkoutState.Countdown) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (workoutState as WorkoutState.Countdown).secondsRemaining.toString(),
                    color = Color(0xFF00E5FF),
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (workoutState is WorkoutState.Rest) {
            val restState = workoutState as WorkoutState.Rest
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Rest",
                        color = Color(0xFF00E5FF),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${restState.secondsRemaining}s",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set ${restState.currentSet} of ${restState.totalSets} complete",
                        color = Color(0xFFB0BEC5),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // ── Feedback banner ───────────────────────────────────────────────
        if (workoutState !is WorkoutState.Countdown) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 80.dp)
                    .background(Color(0xCC000000), shape = RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = when (val s = workoutState) {
                        is WorkoutState.Idle           -> "Tap Start when ready"
                        is WorkoutState.WaitingForPose -> s.hint
                        is WorkoutState.Paused         -> "Paused — tap Resume to continue"
                        is WorkoutState.Rest           -> "Rest — next set in ${s.secondsRemaining}s"
                        else                           -> feedbackText
                    },
                    color = when (workoutState) {
                        is WorkoutState.Idle,
                        is WorkoutState.WaitingForPose,
                        is WorkoutState.Paused,
                        is WorkoutState.Rest -> Color(0xFFB0BEC5)
                        else                 -> feedbackColor
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── Stop button ───────────────────────────────────────────────────
        Button(
            onClick = { saveAndExit() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = "Stop", color = Color.White)
        }

        // ── Start / Pause / Resume ────────────────────────────────────────
        val actionLabel = when (workoutState) {
            is WorkoutState.Idle   -> "Start"
            is WorkoutState.Paused -> "Resume"
            is WorkoutState.Active -> "Pause"
            else                   -> null
        }

        if (actionLabel != null) {
            Button(
                onClick = {
                    when (workoutState) {
                        is WorkoutState.Active -> session.pause()
                        else                   -> session.start()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = actionLabel,
                    color = Color(0xFF0A0F1E),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Exercise label + rep/hold counter ─────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color(0xCC000000), shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = exerciseType.name,
                    color = Color(0xFF00E5FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Show "Calibrating..." label during first rep
                val isCalibrating = lastResult?.isCalibrating == true &&
                        workoutState is WorkoutState.Active

                if (isCalibrating) {
                    Text(
                        text = "Calibrating...",
                        color = Color(0xFFFFB300),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (exerciseType == ExerciseType.PLANK) {
                    Text(
                        text = "Hold: ${lastResult?.holdDurationSeconds ?: 0}s",
                        color = Color(0xFF00E676),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    val displayReps = when (val s = workoutState) {
                        is WorkoutState.Active    -> s.repCount
                        is WorkoutState.Paused    -> s.repCount
                        is WorkoutState.Completed -> s.finalRepCount
                        else                      -> 0
                    }
                    Text(
                        text = "Reps: $displayReps",
                        color = Color(0xFF00E676),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (config.sets > 1) {
                    Text(
                        text = "Set $currentSetIndex of ${config.sets}",
                        color = Color(0xFFB0BEC5),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}