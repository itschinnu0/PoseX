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
import com.example.posex.exercise.ExerciseType
import com.example.posex.exercise.PlankAnalyzer
import com.example.posex.exercise.PushupAnalyzer
import com.example.posex.exercise.SquatsAnalyzer
import com.example.posex.exercise.SquatAnalysisResult
import com.example.posex.feedback.FeedbackEngine
import com.example.posex.ui.components.CameraPreview
import com.example.posex.ui.components.PoseOverlay
import com.google.mlkit.vision.pose.Pose

@Composable
fun WorkoutScreen(
    exerciseType: ExerciseType,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var currentPose by remember { mutableStateOf<Pose?>(null) }
    var feedbackText by remember { mutableStateOf("Get into position") }
    var feedbackColor by remember { mutableStateOf(Color(0xFFB0BEC5)) }
    var repCount by remember { mutableStateOf(0) }

    val feedbackEngine = remember { FeedbackEngine(context) }

    DisposableEffect(Unit) {
        onDispose {
            feedbackEngine.shutdown()
        }
    }

    fun processPose(pose: Pose) {
        val result = when (exerciseType) {
            ExerciseType.SQUAT -> SquatsAnalyzer.analyze(pose)
            ExerciseType.PUSHUP -> {
                val feedbackList = PushupAnalyzer.analyze(pose)
                SquatAnalysisResult(feedbackList, 0, null)
            }
            ExerciseType.PLANK -> {
                val feedbackList = PlankAnalyzer.analyze(pose)
                SquatAnalysisResult(feedbackList, 0, null)
            }
        }

        val primaryFeedback = result.feedback.firstOrNull() ?: return
        repCount = result.repCount

        feedbackText = primaryFeedback
        feedbackColor = if (primaryFeedback == "Good form, keep going" ||
            primaryFeedback == "Good form, hold steady"
        ) {
            Color(0xFF00E676)
        } else {
            Color(0xFFFF5252)
        }

        if (primaryFeedback != "Good form, keep going" &&
            primaryFeedback != "Good form, hold steady"
        ) {
            feedbackEngine.speak(primaryFeedback)
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

        // Feedback banner at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(
                    color = Color(0xCC000000),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = feedbackText,
                color = feedbackColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Exit button at top
        Button(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xCC000000)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = "Exit", color = Color.White)
        }

        // Exercise label at top center
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(
                    color = Color(0xCC000000),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = exerciseType.name,
                    color = Color(0xFF00E5FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                // Rep counter display for squat
                if (exerciseType == ExerciseType.SQUAT) {
                    Text(
                        text = "Reps: $repCount",
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