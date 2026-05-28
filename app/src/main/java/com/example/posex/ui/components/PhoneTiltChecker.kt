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

enum class PhoneOrientation { PORTRAIT, LANDSCAPE }

@Composable
fun PhoneTiltWarning(
    context: Context,
    modifier: Modifier = Modifier,
    expectedOrientation: PhoneOrientation = PhoneOrientation.PORTRAIT,
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

    DisposableEffect(sensorManager, accelerometer, expectedOrientation, tiltThresholdDegrees) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gravityX = event.values.getOrNull(0) ?: return
                val gravityY = event.values.getOrNull(1) ?: return

                val relevantGravity = when (expectedOrientation) {
                    PhoneOrientation.PORTRAIT  -> abs(gravityY)
                    PhoneOrientation.LANDSCAPE -> abs(gravityX)
                }

                val ratio = (relevantGravity / 9.8f).coerceIn(0f, 1f)
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
            val message = when (expectedOrientation) {
                PhoneOrientation.PORTRAIT -> "⚠  Hold your phone upright for accurate detection"
                PhoneOrientation.LANDSCAPE -> "⚠  Lay your phone on its side for accurate detection"
            }
            Text(
                text = message,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
