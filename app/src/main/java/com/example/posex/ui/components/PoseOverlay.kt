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
    imageHeight: Int
) {
    Canvas(modifier = modifier) {
        if (pose == null) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        // CameraX in portrait delivers frames rotated 90 degrees
        // so image width maps to screen height and vice versa
        val scaleX = canvasWidth / imageHeight.toFloat()
        val scaleY = canvasHeight / imageWidth.toFloat()

        fun landmarkToOffset(landmark: PoseLandmark): Offset {
            // Mirror X axis for front camera
            val x = canvasWidth - (landmark.position.x * scaleX)
            val y = landmark.position.y * scaleY
            return Offset(x, y)
        }

        fun drawBone(startId: Int, endId: Int) {
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
                    strokeWidth = 6f
                )
            }
        }

        // Torso
        drawBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawBone(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawBone(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)

        // Left arm
        drawBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawBone(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)

        // Right arm
        drawBone(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawBone(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)

        // Left leg
        drawBone(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawBone(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)

        // Right leg
        drawBone(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawBone(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)

        // Dots
        pose.allPoseLandmarks.forEach { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = landmarkToOffset(landmark)
                )
            }
        }
    }
}