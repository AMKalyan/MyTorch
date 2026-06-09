package com.example.mytorch

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    private val sensorManager: SensorManager,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    companion object {
        private const val SHAKE_THRESHOLD = 12.0f // m/s^2 above gravity
        private const val SHAKE_TIME_WINDOW_MS = 1000L
        private const val SHAKE_DEBOUNCE_MS = 200L
        private const val REQUIRED_SHAKES = 2
        private const val COOLDOWN_MS = 1000L
    }

    private var accelerometer: Sensor? = null
    private var shakeCount = 0
    private var lastShakeTime = 0L
    private var firstShakeTime = 0L
    private var lastTriggerTime = 0L

    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val now = System.currentTimeMillis()

        // Enforce cooldown after a successful toggle
        if (now - lastTriggerTime < COOLDOWN_MS) {
            return
        }

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Compute magnitude minus gravity (~9.81)
        val magnitude = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        // We use absolute magnitude in case they shake in any direction
        if (Math.abs(magnitude) > SHAKE_THRESHOLD) {
            // Check debounce
            if (now - lastShakeTime < SHAKE_DEBOUNCE_MS) {
                return
            }

            // Check if window expired
            if (shakeCount == 0 || now - firstShakeTime > SHAKE_TIME_WINDOW_MS) {
                shakeCount = 0
                firstShakeTime = now
            }

            shakeCount++
            lastShakeTime = now

            if (shakeCount >= REQUIRED_SHAKES) {
                lastTriggerTime = now
                shakeCount = 0
                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
