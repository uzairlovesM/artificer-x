package com.waheed.artificerx.util

import android.content.Context
import android.hardware.SensorManager
import android.util.Log

class PressureManager(context: Context? = null) {
    private val sensorManager = context?.getSystemService(SensorManager::class.java)
    private var currentPressureLevel = 0.5f

    fun readPressureSensors(): SensorData {
        val pressureSensor = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_PRESSURE)
        val available = pressureSensor != null
        return SensorData(
            pressureLevel = currentPressureLevel,
            stabilityScore = if (available) 1f else 0f,
            temperature = 25f,
        )
    }

    fun registerPressureCallback(callback: (Float) -> Unit) {
        callback(currentPressureLevel)
    }

    fun dispose() = Unit

    data class SensorData(
        val pressureLevel: Float,
        val stabilityScore: Float,
        val temperature: Float,
    )
}
