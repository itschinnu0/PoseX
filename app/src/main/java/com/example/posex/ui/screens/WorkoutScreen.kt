package com.example.posex.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.posex.audio.PoseXSfxManager
import com.example.posex.audio.PoseXTtsManager
import com.example.posex.data.SessionRecord
import com.example.posex.data.StorageService
import com.example.posex.exercise.*
import com.example.posex.ui.components.CameraPreview
import com.example.posex.ui.components.PhoneOrientation
import com.example.posex.ui.components.PhoneTiltWarning
import com.example.posex.ui.components.PoseOverlay
import com.example.posex.ui.theme.*
import com.google.mlkit.vision.pose.Pose
import java.util.UUID

@Composable
fun WorkoutScreen(
    exerciseType: ExerciseType,
    config: WorkoutConfig,
    activeProfileId: String?,
    onExit: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var imageWidth by remember { mutableIntStateOf(480) }
    var imageHeight by remember { mutableIntStateOf(640) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
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

    val targetHoldSeconds = if (exerciseType == ExerciseType.PLANK) config.holdSeconds else 0

    var lastState by remember { mutableStateOf<WorkoutState?>(null) }
    var currentSetIndex by remember { mutableIntStateOf(1) }
    var lastRepCount by remember { mutableIntStateOf(0) }
    
    var currentPose by remember { mutableStateOf<Pose?>(null) }
    var feedbackText by remember { mutableStateOf("GET INTO POSITION") }
    var feedbackColor by remember { mutableStateOf(PoseXOnSurface) }
    var lastResult by remember { mutableStateOf<ExerciseAnalysisResult?>(null) }
    var workoutState by remember { mutableStateOf<WorkoutState>(WorkoutState.Idle) }

    val ttsManager = remember { PoseXTtsManager(context) }
    val sfxManager = remember { PoseXSfxManager(context) }
    val storageService = remember { StorageService(context) }
    val sessionId = remember { UUID.randomUUID().toString() }
    val sessionStartDate = remember { System.currentTimeMillis() }
    val activity = context as? Activity

    val warningFrameCounts = remember { mutableMapOf<String, Int>() }
    val warningFrameThreshold = 5

    val session = remember {
        WorkoutSession(
            scope = coroutineScope,
            targetReps = if (exerciseType == ExerciseType.PLANK) 0 else config.repsPerSet,
            totalSets = config.sets,
            restSeconds = config.restSeconds,
            onStateChanged = { newState ->
                val prev = workoutState
                workoutState = newState
                ttsManager.setWorkoutState(newState)

                if (newState is WorkoutState.Rest && prev !is WorkoutState.Rest) {
                    ttsManager.speakRestStart(newState.secondsRemaining)
                }
                if (prev is WorkoutState.Rest && newState is WorkoutState.WaitingForPose) {
                    ttsManager.speakRestComplete()
                }

                when (newState) {
                    is WorkoutState.Countdown, is WorkoutState.Rest -> sfxManager.playTick()
                    else -> Unit
                }

                if (newState is WorkoutState.Rest) {
                    currentSetIndex = newState.currentSet
                }

                if (newState is WorkoutState.Countdown) ttsManager.speakCountdown(newState.secondsRemaining)
                if (newState !is WorkoutState.Active) sfxManager.stopTimerLoop()
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
            sfxManager.release()
        }
    }

    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    fun saveAndExit() {
        val durationMs = session.getSessionDurationMs()
        if (durationMs > 0L) {
            storageService.saveSession(
                SessionRecord(
                    id = sessionId,
                    exerciseType = exerciseType.name,
                    date = sessionStartDate,
                    repCount = lastResult?.repCount ?: 0,
                    holdSeconds = lastResult?.holdDurationSeconds ?: 0,
                    durationMs = durationMs,
                    criticalCues = session.getCriticalCueCount(),
                    warningCues = session.getWarningCueCount(),
                    profileId = activeProfileId ?: ""
                )
            )
        }
        SquatsAnalyzer.resetRepCounter()
        PushupAnalyzer.resetRepCounter()
        PlankAnalyzer.resetRepCounter()
        BicepsAnalyzer.resetRepCounter()
        session.stop()
        onExit(sessionId)
    }

    fun handlePoseFrame(pose: Pose) {
        if (session.isWaitingForPose()) {
            val readiness = PoseReadinessChecker.check(pose, exerciseType)
            session.updatePoseHint(readiness.hint)
            if (readiness.isReady) session.onPoseReady()
            return
        }

        if (!session.isActive()) return

        val result = when (exerciseType) {
            ExerciseType.SQUAT -> SquatsAnalyzer.analyze(pose)
            ExerciseType.PUSHUP -> PushupAnalyzer.analyze(pose)
            ExerciseType.PLANK -> PlankAnalyzer.analyze(pose)
            ExerciseType.BICEPS_CURL -> BicepsAnalyzer.analyze(pose)
        }
        lastResult = result

        if (exerciseType == ExerciseType.PLANK) {
            if (result.isTimerRunning) sfxManager.startTimerLoop() else sfxManager.stopTimerLoop()
        } else if (result.repCount > lastRepCount) {
            sfxManager.playRepIncrement()
            lastRepCount = result.repCount
        }

        if (result.exerciseCompleted) {
            if (exerciseType == ExerciseType.PLANK && targetHoldSeconds > 0 && result.holdDurationSeconds >= targetHoldSeconds) {
                if (session.onHoldCompleted()) saveAndExit()
                return
            }
            saveAndExit()
            return
        }

        if (result.repRejected) {
            feedbackText = result.rejectionReason.uppercase()
            feedbackColor = PoseXError
            ttsManager.speakCue(result.rejectionReason)
            session.onRepUpdated(result.repCount)
            return
        }

        if (result.isCalibrating) {
            feedbackText = "CALIBRATING FORM..."
            feedbackColor = PoseXAccent
            session.onRepUpdated(result.repCount)
            return
        }

        val topCue = CuePrioritizer.topCue(result.cues)
        if (topCue != null) {
            feedbackText = topCue.message.uppercase()
            feedbackColor = when (topCue.severity) {
                FormCue.Severity.CRITICAL -> PoseXError
                FormCue.Severity.WARNING -> Color(0xFFFFB300)
                FormCue.Severity.SUCCESS -> PoseXSuccess
                else -> PoseXOnSurface
            }

            if (topCue.severity == FormCue.Severity.CRITICAL) {
                session.recordCue(topCue.severity)
                ttsManager.speakCue(topCue.message)
            } else if (topCue.severity == FormCue.Severity.WARNING) {
                val count = (warningFrameCounts[topCue.message] ?: 0) + 1
                if (count >= warningFrameThreshold) {
                    session.recordCue(topCue.severity)
                    ttsManager.speakCue(topCue.message)
                    warningFrameCounts[topCue.message] = 0
                } else {
                    warningFrameCounts[topCue.message] = count
                }
            }
        } else {
            feedbackText = "PERFECT FORM"
            feedbackColor = PoseXSuccess
        }

        if (session.onRepUpdated(result.repCount)) saveAndExit()
    }

    Scaffold(
        containerColor = Color.Black // Deepest black for contrast
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Layer
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                lifecycleOwner = lifecycleOwner,
                isActive = workoutState !is WorkoutState.Rest,
                onPoseDetected = { pose, w, h ->
                    currentPose = pose
                    imageWidth = w
                    imageHeight = h
                    handlePoseFrame(pose)
                },
                onSnapshotCaptured = {},
                onError = {}
            )

            // Tech Overlay Layer
            PoseOverlay(
                pose = currentPose,
                modifier = Modifier.fillMaxSize(),
                imageWidth = imageWidth,
                imageHeight = imageHeight
            )

            // UI Layer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Stats Bar
                WorkoutTopBar(
                    exerciseName = exerciseType.name,
                    reps = if (exerciseType == ExerciseType.PLANK) 0 else lastRepCount,
                    holdSeconds = if (exerciseType == ExerciseType.PLANK) lastResult?.holdDurationSeconds ?: 0 else 0,
                    isPlank = exerciseType == ExerciseType.PLANK,
                    isCalibrating = lastResult?.isCalibrating == true,
                    onClose = { saveAndExit() }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Real-time Feedback Banner
                AnimatedVisibility(
                    visible = workoutState is WorkoutState.Active || workoutState is WorkoutState.WaitingForPose,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    FeedbackBanner(text = feedbackText, color = feedbackColor)
                }

                // Controls
                WorkoutControls(
                    state = workoutState,
                    onAction = {
                        if (workoutState is WorkoutState.Active) session.pause() else session.start()
                    }
                )
            }

            // High-Alert Overlays
            if (workoutState is WorkoutState.Countdown) {
                CountdownOverlay(seconds = (workoutState as WorkoutState.Countdown).secondsRemaining)
            }

            if (workoutState is WorkoutState.Rest) {
                RestOverlay(
                    seconds = (workoutState as WorkoutState.Rest).secondsRemaining,
                    setInfo = "SET ${currentSetIndex} COMPLETE"
                )
            }

            PhoneTiltWarning(
                context = context,
                modifier = Modifier.align(Alignment.Center),
                expectedOrientation = when (exerciseType) {
                    ExerciseType.SQUAT, ExerciseType.BICEPS_CURL -> PhoneOrientation.PORTRAIT
                    else -> PhoneOrientation.LANDSCAPE
                }
            )
        }
    }
}

@Composable
fun WorkoutTopBar(
    exerciseName: String,
    reps: Int,
    holdSeconds: Int,
    isPlank: Boolean,
    isCalibrating: Boolean,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Metric Card
        Surface(
            color = PoseXSurface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(160.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    exerciseName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = PoseXAccent
                )
                Text(
                    text = if (isPlank) "${holdSeconds}s" else reps.toString(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = PoseXSuccess
                    )
                )
                if (isCalibrating) {
                    Text(
                        "CALIBRATING",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFB300)
                    )
                }
            }
        }

        // Exit Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .background(PoseXError.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Exit", tint = PoseXError)
        }
    }
}

@Composable
fun FeedbackBanner(text: String, color: Color) {
    Surface(
        color = PoseXSurface.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
fun WorkoutControls(state: WorkoutState, onAction: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is WorkoutState.Active, is WorkoutState.Paused, is WorkoutState.Idle -> {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state is WorkoutState.Active) PoseXSurface else PoseXAccent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(64.dp)
                        .width(200.dp)
                ) {
                    Icon(
                        if (state is WorkoutState.Active) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (state is WorkoutState.Active) PoseXAccent else PoseXBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state is WorkoutState.Active) "PAUSE" else if (state is WorkoutState.Paused) "RESUME" else "START",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (state is WorkoutState.Active) PoseXAccent else PoseXBackground
                        )
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
fun CountdownOverlay(seconds: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = seconds.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 160.sp,
                fontWeight = FontWeight.Black,
                color = PoseXAccent
            )
        )
    }
}

@Composable
fun RestOverlay(seconds: Int, setInfo: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PoseXBackground.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(setInfo, color = PoseXAccent, style = MaterialTheme.typography.labelLarge)
            Text(
                "REST: ${seconds}S",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
        }
    }
}
