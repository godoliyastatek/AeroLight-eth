package com.aerolight.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "aerolight_alerts"
        const val NOTIFICATION_ID = 1001
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Air Quality Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when air quality reaches WARN or DANGER levels"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showAlert(status: String, reading: SensorReading) {
        val title = if (status == SensorReading.STATUS_DANGER) {
            "⚠️ DANGER: Air Quality Critical"
        } else {
            "⚠️ Warning: Air Quality Degrading"
        }

        val message = "Gas: %.1f | Temp: %.1f°C | Humidity: %.1f%%".format(
            reading.gasLevel, reading.temperature, reading.humidity
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Caller is responsible for having checked/requested POST_NOTIFICATIONS
        // permission on Android 13+ before this is invoked.
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }
}
