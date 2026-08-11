package com.project.helpcircle.os

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.project.helpcircle.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android's notification APIs for this app's system boundary: the persistent low-priority
 * notification that keeps [DoomscrollAccessibilityService] alive as a foreground service, and
 * delivered-nudge notifications (plus, for haptic nudges, the vibration itself) on a separate,
 * higher-priority channel. Every nudge notification carries an "I'm back" action that reports
 * recovery to the device's active community via [RecoveryActionReceiver].
 */
@Singleton
class HelpCircleNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val systemNotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val nextNudgeNotificationId = AtomicInteger(MONITORING_NOTIFICATION_ID)

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

    fun postTextNudgeNotification(message: String) {
        postNudgeNotification(context.getString(R.string.text_nudge_title), message)
    }

    fun postHapticNudgeNotification() {
        postNudgeNotification(
            context.getString(R.string.haptic_nudge_title),
            context.getString(R.string.haptic_nudge_text)
        )
    }

    fun postGrayscaleFallbackNotification() {
        postNudgeNotification(
            context.getString(R.string.grayscale_nudge_title),
            context.getString(R.string.grayscale_nudge_text)
        )
    }

    fun fireHapticPattern() {
        vibrator.vibrate(VibrationEffect.createWaveform(SOS_PATTERN_MILLIS, NO_REPEAT))
    }

    private fun postNudgeNotification(title: String, text: String) {
        val notificationId = nextNudgeNotificationId.incrementAndGet()
        systemNotificationManager.notify(
            notificationId,
            NotificationCompat.Builder(context, NUDGE_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(recoveryAction(notificationId))
                .build()
        )
    }

    private fun recoveryAction(notificationId: Int): NotificationCompat.Action {
        val intent = Intent(context, RecoveryActionReceiver::class.java)
            .putExtra(RecoveryActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, context.getString(R.string.recovery_action_label), pendingIntent)
            .build()
    }

    companion object {
        const val MONITORING_NOTIFICATION_ID = 1
        const val NUDGE_CHANNEL_ID = "nudges"
        private const val MONITORING_CHANNEL_ID = "monitoring"

        // long-short-short-long: off, on(long), off, on(short), off, on(short), off, on(long)
        private val SOS_PATTERN_MILLIS = longArrayOf(0, 400, 100, 150, 100, 150, 100, 400)
        private const val NO_REPEAT = -1
    }
}
