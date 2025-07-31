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
    private val shakeThreshold = 12f

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
        if (diffTime > 100) {
            val deltaX = x - lastX
            val deltaY = y - lastY
            val deltaZ = z - lastZ
            val speed = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / diffTime * 10000
            if (speed > shakeThreshold) {
                onShake()
            }
            lastTime = now
            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {}
}
