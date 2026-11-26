package com.coinflipper.app.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.sqrt

private const val SHAKE_THRESHOLD_G = 2.4f
private const val SHAKE_COOLDOWN_MS = 1200L

/** Calls [onShake] when the phone is shaken, only while the screen is visible. */
@Composable
fun ShakeEffect(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnShake by rememberUpdatedState(onShake)

    DisposableEffect(enabled, lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose { }
        }

        var lastShake = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                val now = SystemClock.elapsedRealtime()
                if (gForce > SHAKE_THRESHOLD_G && now - lastShake > SHAKE_COOLDOWN_MS) {
                    lastShake = now
                    currentOnShake()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun register() = sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        fun unregister() = sensorManager.unregisterListener(listener)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE -> unregister()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) register()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregister()
        }
    }
}
