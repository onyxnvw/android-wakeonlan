package de.onyxnvw.wakeonlan

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class WakeOnLanApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // create NotificationChannels for Android versions that require that action
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // notification channel for status notifications (high importance)
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    "de.onyxnvw.wakeonlan.status_notification_channel_id",
                    "Netzwerkgeräte-Status",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }
}
