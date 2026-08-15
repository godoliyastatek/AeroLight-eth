package com.aerolight.app

/**
 * Mirrors the JSON structure written by the AeroLight device to Firebase.
 * Adjust field names here if your device writes different keys
 * (e.g. "temp" instead of "temperature").
 */
data class SensorReading(
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val gasLevel: Float = 0f,
    val status: String = "OK",      // expected values: OK, WARN, DANGER
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_OK = "OK"
        const val STATUS_WARN = "WARN"
        const val STATUS_DANGER = "DANGER"
    }
}
