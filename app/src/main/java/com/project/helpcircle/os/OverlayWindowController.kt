package com.project.helpcircle.os

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds/removes a single non-interactive, full-screen [android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]
 * view. Shared by every intervention that needs to draw over other apps (grayscale fallback,
 * content blur), since only one such overlay should ever be showing at a time — showing a new
 * one replaces whatever was there.
 *
 * `SYSTEM_ALERT_WINDOW` is a special permission the user must grant manually via system
 * settings; it is never auto-granted, so [canDrawOverlays] must be checked before [show] and
 * callers must have a fallback for when it returns false.
 */
@Singleton
class OverlayWindowController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    /**
     * Shows [view] full-screen, replacing any overlay already shown. Returns false if not
     * permitted or if adding the view fails. When [blurBehindRadius] is positive and the device
     * is running Android 12+, the window compositor blurs whatever is behind the overlay
     * (`FLAG_BLUR_BEHIND`); below that API level the flag/field don't exist, so [view] itself
     * must carry a fallback visual (e.g. an opaque scrim background).
     */
    fun show(view: View, blurBehindRadius: Int = 0): Boolean {
        if (!canDrawOverlays()) return false
        dismiss()
        val useBlurBehind = blurBehindRadius > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        if (useBlurBehind) flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        )
        if (useBlurBehind) params.blurBehindRadius = blurBehindRadius
        return runCatching { windowManager.addView(view, params) }
            .onSuccess { overlayView = view }
            .isSuccess
    }

    fun dismiss() {
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayView = null
    }
}
