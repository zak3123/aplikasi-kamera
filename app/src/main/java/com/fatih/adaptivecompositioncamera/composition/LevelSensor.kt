package com.fatih.adaptivecompositioncamera.composition

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.fatih.adaptivecompositioncamera.domain.model.LevelReading
import com.fatih.adaptivecompositioncamera.utility.CameraMath
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun rememberLevelReading(enabled: Boolean): LevelReading {
    val context = LocalContext.current
    var reading by remember { mutableStateOf(LevelReading()) }
    DisposableEffect(context, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            private var filteredX = 0f
            private var filteredY = 0f
            private var filteredZ = 0f

            override fun onSensorChanged(event: SensorEvent) {
                val alpha = 0.16f
                filteredX += alpha * (event.values[0] - filteredX)
                filteredY += alpha * (event.values[1] - filteredY)
                filteredZ += alpha * (event.values[2] - filteredZ)
                val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .defaultDisplay.rotation
                val (screenX, screenY) = when (rotation) {
                    Surface.ROTATION_90 -> filteredY to -filteredX
                    Surface.ROTATION_180 -> -filteredX to -filteredY
                    Surface.ROTATION_270 -> -filteredY to filteredX
                    else -> filteredX to filteredY
                }
                reading = LevelReading(
                    rollDegrees = CameraMath.horizonRollDegrees(screenX, screenY),
                    pitchDegrees = Math.toDegrees(
                        atan2(-filteredZ.toDouble(), hypot(screenX.toDouble(), screenY.toDouble())),
                    ).toFloat(),
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (gravity != null) sensorManager.registerListener(listener, gravity, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return reading
}
