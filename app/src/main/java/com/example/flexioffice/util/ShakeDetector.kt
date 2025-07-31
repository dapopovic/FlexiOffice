package com.example.flexioffice.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

class ShakeDetector(
    private val onShake: () -> Unit,
) : SensorEventListener {
    private var lastTime: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private val shakeThreshold = 13f
    private val shakeCountThreshold = 2
    private val shakeWindowMs = 800
    private var lastShakeTime: Long = 0
    private var shakeCount = 0

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER || event.values.size < 3) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val now = System.currentTimeMillis()

        if (lastTime == 0L) {
            lastTime = now
            lastX = x
            lastY = y
            lastZ = z
            return
        }

        val diffTime = now - lastTime
        if (diffTime > 50) {
            val deltaX = x - lastX
            val deltaY = y - lastY
            val deltaZ = z - lastZ
            val maxDelta = maxOf(kotlin.math.abs(deltaX), kotlin.math.abs(deltaY), kotlin.math.abs(deltaZ))

            // Zähle Richtungswechsel, wenn Schwelle überschritten
            if (maxDelta > shakeThreshold) {
                if (shakeCount == 0 || (now - lastShakeTime < shakeWindowMs)) {
                    shakeCount++
                    lastShakeTime = now
                } else {
                    shakeCount = 1
                    lastShakeTime = now
                }
            }

            // Shake nur auslösen, wenn mehrere Richtungswechsel innerhalb des Zeitfensters
            if (shakeCount >= shakeCountThreshold) {
                shakeCount = 0
                lastShakeTime = now
                onShake()
            }

            lastTime = now
            lastX = x
            lastY = y
            lastZ = z
        }
    }
}
