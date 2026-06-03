package com.example.posex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.example.posex.ui.theme.PoseXAccent
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

        val scaleX = canvasWidth / imageHeight.toFloat()
        val scaleY = canvasHeight / imageWidth.toFloat()

        fun landmarkToOffset(landmark: PoseLandmark): Offset {
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
                    color = PoseXAccent,
                    start = landmarkToOffset(start),
                    end = landmarkToOffset(end),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw connections
        val connections = listOf(
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
        )

        connections.forEach { (s, e) -> drawBone(s, e) }

        // Points
        pose.allPoseLandmarks.forEach { landmark ->
            if (landmark.inFrameLikelihood > 0.6f) {
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = landmarkToOffset(landmark)
                )
            }
        }
    }
}
