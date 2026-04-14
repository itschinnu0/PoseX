package com.example.posex.exercise

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.sqrt

object PoseUtils {

    fun calculateAngle(
        first: PoseLandmark,
        mid: PoseLandmark,
        last: PoseLandmark
    ): Double {
        val ax = first.position.x - mid.position.x
        val ay = first.position.y - mid.position.y
        val bx = last.position.x - mid.position.x
        val by = last.position.y - mid.position.y

        val dot = ax * bx + ay * by
        val magA = sqrt((ax * ax + ay * ay).toDouble())
        val magB = sqrt((bx * bx + by * by).toDouble())

        if (magA == 0.0 || magB == 0.0) return 0.0

        val cosAngle = (dot / (magA * magB)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(Math.acos(cosAngle))
    }
}