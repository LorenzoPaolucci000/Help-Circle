package com.project.helpcircle.os

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Applies the "Content Blur" nudge as a full-screen overlay: a real window-compositor blur
 * (`FLAG_BLUR_BEHIND`) on Android 12+, or a near-opaque scrim on older versions where that API
 * doesn't exist. Falls back to a plain notification if the overlay permission isn't granted.
 * The overlay stays non-focusable/non-touch-intercepting — purely visual, same as the grayscale
 * fallback — to keep this `SYSTEM_ALERT_WINDOW` usage as low-risk as such an overlay can be.
 * [apply] auto-[revert]s after a fixed duration so a missed "I'm back" tap can't leave the
 * screen blurred indefinitely.
 */
@Singleton
class ContentBlurInterventionController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val overlayWindowController: OverlayWindowController,
    private val notificationManager: HelpCircleNotificationManager
) {
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var revertJob: Job? = null

    fun apply() {
        revertJob?.cancel()
        if (!overlayWindowController.show(blurOverlayView(), blurBehindRadius = BLUR_RADIUS_PX)) {
            notificationManager.postContentBlurFallbackNotification()
        }
        revertJob = controllerScope.launch {
            delay(DURATION_MILLIS)
            revert()
        }
    }

    fun revert() {
        revertJob?.cancel()
        revertJob = null
        overlayWindowController.dismiss()
    }

    private fun blurOverlayView(): View = View(context).apply {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            setBackgroundColor(Color.argb(SCRIM_ALPHA, 20, 20, 20))
        }
    }

    companion object {
        private const val DURATION_MILLIS = 20_000L
        private const val BLUR_RADIUS_PX = 60
        private const val SCRIM_ALPHA = 217 // ~85% opaque; pre-Android 12 fallback with no real blur API
    }
}
