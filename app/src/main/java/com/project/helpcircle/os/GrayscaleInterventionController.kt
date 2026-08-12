package com.project.helpcircle.os

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.provider.Settings
import android.view.View
import androidx.core.content.ContextCompat
import com.project.helpcircle.domain.model.Nudge
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
 * Applies the "Progressive Grey-scale" nudge, in priority order: system-wide grayscale via
 * `Settings.Secure` if `WRITE_SECURE_SETTINGS` is granted (never auto-granted to a normal
 * install — only reachable via `adb shell pm grant`, so in practice this path mostly matters
 * for development/demo builds); otherwise a translucent grey overlay approximating desaturation,
 * if the overlay permission is granted; otherwise a plain notification, so a grayscale nudge is
 * never silently dropped. [apply] auto-[revert]s after a level-scaled duration so a missed
 * "I'm back" tap can't leave the effect stuck on.
 */
@Singleton
class GrayscaleInterventionController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val overlayWindowController: OverlayWindowController,
    private val notificationManager: HelpCircleNotificationManager
) {
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var revertJob: Job? = null

    fun apply(level: Int) {
        revertJob?.cancel()
        when {
            hasWriteSecureSettingsPermission() -> setSystemGrayscale(enabled = true)
            overlayWindowController.show(grayscaleOverlayView(level)) -> Unit
            else -> notificationManager.postGrayscaleFallbackNotification()
        }
        revertJob = controllerScope.launch {
            delay(level.coerceIn(1, Nudge.GreyscaleLevel.MAX_LEVEL) * LEVEL_DURATION_MILLIS)
            revert()
        }
    }

    fun revert() {
        revertJob?.cancel()
        revertJob = null
        if (hasWriteSecureSettingsPermission()) setSystemGrayscale(enabled = false)
        overlayWindowController.dismiss()
    }

    /** Maps [level] (1-3) directly to its 33%/66%/100% alpha, matching the 3-step grayscale scale. */
    private fun grayscaleOverlayView(level: Int): View {
        val alpha = level.coerceIn(1, Nudge.GreyscaleLevel.MAX_LEVEL).toFloat() / Nudge.GreyscaleLevel.MAX_LEVEL
        return View(context).apply { setBackgroundColor(Color.argb((alpha * 255).toInt(), 128, 128, 128)) }
    }

    private fun hasWriteSecureSettingsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    private fun setSystemGrayscale(enabled: Boolean) {
        runCatching {
            Settings.Secure.putInt(context.contentResolver, SETTING_DALTONIZER_ENABLED, if (enabled) 1 else 0)
            if (enabled) {
                Settings.Secure.putInt(context.contentResolver, SETTING_DALTONIZER_MODE, DALTONIZER_MODE_MONOCHROMACY)
            }
        }
    }

    companion object {
        private const val LEVEL_DURATION_MILLIS = 20_000L

        // Public Settings.Secure keys (android.provider.Settings.Secure since API 18). Mode 0 is
        // AOSP's "simulate monochromacy" daltonizer mode, i.e. full grayscale rather than a
        // color-blindness correction filter.
        private const val SETTING_DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
        private const val SETTING_DALTONIZER_MODE = "accessibility_display_daltonizer"
        private const val DALTONIZER_MODE_MONOCHROMACY = 0
    }
}
