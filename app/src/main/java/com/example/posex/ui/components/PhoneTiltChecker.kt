package com.example.posex.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.acos

@Composable
fun PhoneTiltWarning(
    context: Context,
    modifier: Modifier = Modifier,
    tiltThresholdDegrees: Float = 30f,
    onTiltStateChanged: (isTooTilted: Boolean) -> Unit = {}
) {
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accelerometer = remember(sensorManager) {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    if (accelerometer == null) return

    var isTooTilted by remember { mutableStateOf(false) }
    val onTiltStateChangedState by rememberUpdatedState(onTiltStateChanged)

    DisposableEffect(sensorManager, accelerometer, tiltThresholdDegrees) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gravityY = event.values.getOrNull(1) ?: return
                val ratio = (abs(gravityY) / 9.8f).coerceIn(0f, 1f)
                val tiltAngle = Math.toDegrees(acos(ratio).toDouble()).toFloat()
                val nowTooTilted = tiltAngle > tiltThresholdDegrees
                if (nowTooTilted != isTooTilted) {
                    isTooTilted = nowTooTilted
                    onTiltStateChangedState(nowTooTilted)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    if (!isTooTilted) return

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color(0xCCFF5252), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "⚠",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "  Prop your phone more upright for accurate detection",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
