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
import com.project.helpcircle.MainActivity
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
        systemNotificationManager.createNotificationChannel(
            NotificationChannel(
                PEER_ALERT_CHANNEL_ID,
                context.getString(R.string.peer_alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.peer_alert_channel_description)
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

    fun postContentBlurFallbackNotification() {
        postNudgeNotification(
            context.getString(R.string.content_blur_nudge_title),
            context.getString(R.string.content_blur_nudge_text)
        )
    }

    /**
     * Tells this device that a peer is in crisis. Deliberately keyed on [peerId] rather than an
     * incrementing counter, so a peer who slips back into a crisis replaces their own earlier
     * alert instead of stacking a second one; a pile of notifications about the same person is
     * noise, not urgency.
     *
     * Carries no "I'm back" action: that resolves the reader's *own* crisis and would make no
     * sense here. Tapping opens the Help tab, which is where they can actually do something.
     */
    fun postPeerCrisisNotification(peerId: String, nickname: String) {
        val text = context.getString(R.string.peer_crisis_text)
        systemNotificationManager.notify(
            peerAlertNotificationId(peerId),
            NotificationCompat.Builder(context, PEER_ALERT_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.peer_crisis_title, nickname))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setAutoCancel(true)
                .setContentIntent(openHelpTabIntent(peerId))
                .build()
        )
    }

    private fun openHelpTabIntent(peerId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_HELP)
        return PendingIntent.getActivity(
            context,
            peerAlertNotificationId(peerId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * A stable id per peer. Offset well clear of the nudge notifications, which count up from
     * [MONITORING_NOTIFICATION_ID], so the two can never land on the same id and overwrite one
     * another.
     */
    private fun peerAlertNotificationId(peerId: String): Int =
        PEER_ALERT_ID_BASE + (peerId.hashCode() and 0xFFFF)

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
        const val PEER_ALERT_CHANNEL_ID = "peer_alerts"
        private const val MONITORING_CHANNEL_ID = "monitoring"
        private const val PEER_ALERT_ID_BASE = 100_000

        // long-short-short-long: off, on(long), off, on(short), off, on(short), off, on(long)
        private val SOS_PATTERN_MILLIS = longArrayOf(0, 400, 100, 150, 100, 150, 100, 400)
        private const val NO_REPEAT = -1
    }
}
