package com.project.helpcircle.os

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.project.helpcircle.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android's notification APIs for this app's system boundary: the persistent low-priority
 * notification that keeps [DoomscrollAccessibilityService] alive as a foreground service, and
 * (in later steps) delivered-nudge notifications on a separate, higher-priority channel.
 */
@Singleton
class HelpCircleNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val systemNotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        systemNotificationManager.createNotificationChannel(
            NotificationChannel(
                MONITORING_CHANNEL_ID,
                context.getString(R.string.monitoring_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = context.getString(R.string.monitoring_channel_description)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )
        systemNotificationManager.createNotificationChannel(
            NotificationChannel(
                NUDGE_CHANNEL_ID,
                context.getString(R.string.nudge_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.nudge_channel_description)
            }
        )
    }

    fun buildMonitoringNotification(): Notification =
        NotificationCompat.Builder(context, MONITORING_CHANNEL_ID)
            .setContentText(context.getString(R.string.monitoring_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()

    companion object {
        const val MONITORING_NOTIFICATION_ID = 1
        const val NUDGE_CHANNEL_ID = "nudges"
        private const val MONITORING_CHANNEL_ID = "monitoring"
    }
}
