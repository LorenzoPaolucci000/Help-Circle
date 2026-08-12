package com.project.helpcircle.os

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.engine.ScrollSignal
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.repository.NudgeRepository
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
 *
 * Runs itself as a foreground service with a silent, persistent notification so aggressive OEM
 * battery managers are less likely to kill it while it's the only thing keeping the doomscroll
 * detector alive. Being the app's one long-running background component, it also collects
 * [NudgeRepository.incomingNudges] and dispatches each one to [HelpCircleNotificationManager],
 * and marks the delivery on [CrisisEpisodeTracker] so it can score how the crisis episode ends.
 */
@AndroidEntryPoint
class DoomscrollAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var detectLossOfAgencyUseCase: DetectLossOfAgencyUseCase

    @Inject
    lateinit var crisisEpisodeTracker: CrisisEpisodeTracker

    @Inject
    lateinit var nudgeRepository: NudgeRepository

    @Inject
    lateinit var notificationManager: HelpCircleNotificationManager

    @Inject
    lateinit var grayscaleInterventionController: GrayscaleInterventionController

    @Inject
    lateinit var contentBlurInterventionController: ContentBlurInterventionController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onServiceConnected() {
        super.onServiceConnected()
        startForeground(
            HelpCircleNotificationManager.MONITORING_NOTIFICATION_ID,
            notificationManager.buildMonitoringNotification()
        )
        serviceScope.launch {
            nudgeRepository.incomingNudges.collect { nudge -> handleNudge(nudge) }
        }
    }

    private fun handleNudge(nudge: Nudge) {
        crisisEpisodeTracker.onNudgeReceived(System.currentTimeMillis())
        when (nudge) {
            is Nudge.Text -> notificationManager.postTextNudgeNotification(nudge.message)
            Nudge.Haptic -> {
                notificationManager.fireHapticPattern()
                notificationManager.postHapticNudgeNotification()
            }
            is Nudge.GreyscaleLevel -> grayscaleInterventionController.apply(nudge.level)
            Nudge.ContentBlur -> contentBlurInterventionController.apply()
        }
    }

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
