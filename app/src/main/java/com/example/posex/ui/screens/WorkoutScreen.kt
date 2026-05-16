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
import com.example.posex.exercise.PushupAnalyzer
import com.example.posex.exercise.SquatsAnalyzer
import com.example.posex.exercise.WorkoutSession
import com.example.posex.exercise.WorkoutState
import com.example.posex.feedback.FeedbackEngine
import com.example.posex.ui.components.CameraPreview
import com.example.posex.ui.components.PoseOverlay
import com.google.mlkit.vision.pose.Pose
import java.util.UUID

@Composable
fun WorkoutScreen(
    exerciseType: ExerciseType,
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
    }

    var currentPose by remember { mutableStateOf<Pose?>(null) }
    var feedbackText by remember { mutableStateOf("Get into position") }
    var feedbackColor by remember { mutableStateOf(Color(0xFFB0BEC5)) }
    var lastResult by remember { mutableStateOf<ExerciseAnalysisResult?>(null) }
    var workoutState by remember { mutableStateOf<WorkoutState>(WorkoutState.Idle) }

    val feedbackEngine = remember { FeedbackEngine(context) }
    val storageService = remember { StorageService(context) }

    // Session ID generated once per WorkoutScreen instance.
    // Using remember ensures it doesn't regenerate on recomposition.
    val sessionId = remember { UUID.randomUUID().toString() }
    val sessionStartDate = remember { System.currentTimeMillis() }

    val session = remember {
        WorkoutSession(
            scope = coroutineScope,
            targetReps = 0,
            onStateChanged = { newState ->
                workoutState = newState
                feedbackEngine.setWorkoutState(newState)
                if (newState is WorkoutState.Countdown) {
                    feedbackEngine.speakCountdown(newState.secondsRemaining)
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { feedbackEngine.shutdown() }
    }

    fun resetAnalyzers() {
        when (exerciseType) {
            ExerciseType.SQUAT  -> SquatsAnalyzer.resetRepCounter()
            ExerciseType.PUSHUP -> PushupAnalyzer.resetRepCounter()
            ExerciseType.PLANK  -> PlankAnalyzer.resetRepCounter()
        }
    }

    /**
     * Builds a SessionRecord from current session state and persists it.
     * Safe to call from Stop or on target completion.
     * Only saves if the session was ever Active (duration > 0) — avoids
     * saving empty sessions when the user opens and immediately stops.
     */
    fun saveAndExit() {
        val durationMs = session.getSessionDurationMs()
        if (durationMs > 0L) {
            val result = lastResult
            storageService.saveSession(
                SessionRecord(
                    id           = sessionId,
                    exerciseType = exerciseType.name,
                    date         = sessionStartDate,
                    repCount     = result?.repCount ?: 0,
                    holdSeconds  = result?.holdDurationSeconds ?: 0,
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

    fun processPose(pose: Pose) {
        if (!session.isActive()) return

        val result = when (exerciseType) {
            ExerciseType.SQUAT  -> SquatsAnalyzer.analyze(pose)
            ExerciseType.PUSHUP -> PushupAnalyzer.analyze(pose)
            ExerciseType.PLANK  -> PlankAnalyzer.analyze(pose)
        }

        lastResult = result

        val topCue = CuePrioritizer.topCue(result.cues) ?: return

        feedbackText = topCue.message
        feedbackColor = when (topCue.severity) {
            FormCue.Severity.CRITICAL -> Color(0xFFFF5252)
            FormCue.Severity.WARNING  -> Color(0xFFFFB300)
            FormCue.Severity.INFO     -> Color(0xFFB0BEC5)
            FormCue.Severity.SUCCESS  -> Color(0xFF00E676)
        }

        // Record this cue in the session counters before speaking
        session.recordCue(topCue.severity)

        if (topCue.severity == FormCue.Severity.CRITICAL ||
            topCue.severity == FormCue.Severity.WARNING
        ) {
            feedbackEngine.speak(topCue.message)
        }

        val completed = session.onRepUpdated(result.repCount)
        if (completed) {
            saveAndExit()
        }
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
                processPose(pose)
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

        // ── Feedback banner — hidden during Countdown ─────────────────────
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
                    text = when (workoutState) {
                        is WorkoutState.Idle   -> "Tap Start when ready"
                        is WorkoutState.Paused -> "Paused — tap Resume to continue"
                        else                   -> feedbackText
                    },
                    color = when (workoutState) {
                        is WorkoutState.Idle,
                        is WorkoutState.Paused -> Color(0xFFB0BEC5)
                        else                   -> feedbackColor
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── Stop button — always visible ──────────────────────────────────
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

        // ── Start / Pause / Resume — bottom center ────────────────────────
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

        // ── Exercise label + rep/hold counter — top center ────────────────
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
            }
        }
    }
}