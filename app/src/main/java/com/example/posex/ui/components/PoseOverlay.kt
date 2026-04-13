package com.example.posex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

@Composable
fun PoseOverlay(
    pose: Pose?,
    modifier: Modifier = Modifier,
    imageWidth: Int,
    imageHeight: Int,
    screenWidth: Float,
    screenHeight: Float
) {
    Canvas(modifier = modifier) {
        if (pose == null) return@Canvas

        val scaleX = screenWidth / imageHeight // flipped because of portrait + front camera
        val scaleY = screenHeight / imageWidth

        fun landmarkToOffset(landmark: PoseLandmark): Offset {
            // Mirror X for front camera
            val x = screenWidth - (landmark.position.x * scaleX)
            val y = landmark.position.y * scaleY
            return Offset(x, y)
        }

        fun drawLine(startId: Int, endId: Int) {
            val start = pose.getPoseLandmark(startId)
            val end = pose.getPoseLandmark(endId)
            if (start != null && end != null &&
                start.inFrameLikelihood > 0.5f &&
                end.inFrameLikelihood > 0.5f
            ) {
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = landmarkToOffset(start),
                    end = landmarkToOffset(end),
                    strokeWidth = 4f
                )
            }
        }

        // Draw skeleton lines
        // Torso
        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawLine(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawLine(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)

        // Left arm
        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawLine(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)

        // Right arm
        drawLine(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawLine(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)

        // Left leg
        drawLine(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawLine(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)

        // Right leg
        drawLine(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawLine(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)

        // Draw landmark dots
        pose.allPoseLandmarks.forEach { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = landmarkToOffset(landmark)
                )
            }
        }
    }
}