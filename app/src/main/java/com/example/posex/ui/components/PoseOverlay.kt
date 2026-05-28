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

        fun drawBone(startId: Int, endId: Int, confidenceThreshold: Float = 0.5f) {
            val start = pose.getPoseLandmark(startId)
            val end = pose.getPoseLandmark(endId)
            if (start != null && end != null &&
                start.inFrameLikelihood > confidenceThreshold &&
                end.inFrameLikelihood > confidenceThreshold
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
        drawBone(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, 0.7f)
        drawBone(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, 0.7f)

        // Right arm
        drawBone(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, 0.7f)
        drawBone(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, 0.7f)

        // Left leg
        drawBone(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawBone(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)

        // Right leg
        drawBone(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawBone(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)

        // Dots
        pose.allPoseLandmarks.forEach { landmark ->
            val threshold = when (landmark.landmarkType) {
                PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
                PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST -> 0.7f
                else -> 0.5f
            }
            if (landmark.inFrameLikelihood > threshold) {
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = landmarkToOffset(landmark)
                )
            }
        }
    }
}