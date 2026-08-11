package com.project.helpcircle.os

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.project.helpcircle.domain.engine.ScrollSignal
import com.project.helpcircle.domain.usecase.DetectLossOfAgencyUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Passive accessibility boundary: observes system-wide scroll/tap events and turns each one into
 * a [ScrollSignal] for [DetectLossOfAgencyUseCase]. Reads only the event's type and a local
 * timestamp — never window content, on-screen text, or which app it came from beyond filtering
 * out this app's own UI — so raw usage data never leaves this boundary, per the Zero-PII rule.
 * Event coalescing is left to the framework's `notificationTimeout` (see the service's
 * accessibility config) rather than custom throttling here.
 */
@AndroidEntryPoint
class DoomscrollAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var detectLossOfAgencyUseCase: DetectLossOfAgencyUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName == packageName) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED
        ) {
            return
        }
        val signal = ScrollSignal(timestampMillis = System.currentTimeMillis())
        serviceScope.launch { detectLossOfAgencyUseCase(signal) }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
