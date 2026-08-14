package com.project.helpcircle.os

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.engine.ForegroundAppTracker
import com.project.helpcircle.domain.engine.ScrollSignal
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.model.categoryLabel
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import com.project.helpcircle.domain.repository.NudgeRepository
import com.project.helpcircle.domain.usecase.DetectLossOfAgencyUseCase
import com.project.helpcircle.presentation.fallback.SystemFallbackActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Passive accessibility boundary: observes scroll events in apps on the user's monitored-apps
 * blacklist and turns each one into a [ScrollSignal] for [DetectLossOfAgencyUseCase]. Taps
 * (TYPE_VIEW_CLICKED) are deliberately not observed — counting them alongside scrolls made
 * non-doomscroll activity (e.g. chatting: tapping send/react, scrolling message history) trigger
 * the same crisis window as feed-doomscrolling, so detection is now scroll-cadence-only. Reads only
 * the event's type and a local timestamp — never window content or on-screen text — so raw usage
 * data never leaves this boundary, per the Zero-PII rule. Event coalescing is left to the
 * framework's `notificationTimeout` (see the service's accessibility config) rather than custom
 * throttling here.
 *
 * Also watches TYPE_WINDOW_STATE_CHANGED to know which app is in the foreground, checking each
 * transition against the user's own opt-in [MonitoredAppsRepository] blacklist and recording the
 * result on [ForegroundAppTracker] — only the package name of an app the user explicitly chose to
 * monitor is ever inspected this way, never a system-wide scan. That same recorded result is what
 * scopes crisis detection itself: scroll/tap events are only processed while
 * [ForegroundAppTracker.isCurrentForegroundAppBlacklisted] is true, so an empty blacklist means
 * nothing is watched at all. A scroll event's own package name is used as a second foreground
 * signal (see [ForegroundAppTracker.needsForegroundResolution]), because no window transition
 * arrives when this service (re)connects while a monitored app is already on screen — which the OS
 * causes routinely, and which otherwise leaves detection silently inert for that app.
 *
 * Runs itself as a foreground service with a silent, persistent notification so aggressive OEM
 * battery managers are less likely to kill it while it's the only thing keeping the doomscroll
 * detector alive. Being the app's one long-running background component, it also collects
 * [NudgeRepository.incomingNudges] and dispatches each one to [HelpCircleNotificationManager],
 * and marks the delivery on [CrisisEpisodeTracker] so it can score how the crisis episode ends.
 *
 * Also launches [SystemFallbackActivity] whenever [DetectLossOfAgencyUseCase] reports that the
 * community is offline or hasn't responded to a crisis in time, per the System Fallback spec.
 */
@AndroidEntryPoint
class DoomscrollAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var detectLossOfAgencyUseCase: DetectLossOfAgencyUseCase

    @Inject
    lateinit var crisisEpisodeTracker: CrisisEpisodeTracker

    @Inject
    lateinit var foregroundAppTracker: ForegroundAppTracker

    @Inject
    lateinit var monitoredAppsRepository: MonitoredAppsRepository

    @Inject
    lateinit var nudgeRepository: NudgeRepository

    @Inject
    lateinit var notificationManager: HelpCircleNotificationManager

    @Inject
    lateinit var grayscaleInterventionController: GrayscaleInterventionController

    @Inject
    lateinit var contentBlurInterventionController: ContentBlurInterventionController

    // A transient failure in any suspend call this service launches (most commonly a Firestore
    // read/listener rejecting with PERMISSION_DENIED) must never crash this process — it's the
    // one long-running component meant to survive exactly this kind of failure, not die and take
    // doomscroll detection down with it. This is the scope-wide backstop; the nudge-listener Flow
    // below also has its own explicit .catch{} since Flow exceptions need that, not this handler.
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> }
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        startForeground(
            HelpCircleNotificationManager.MONITORING_NOTIFICATION_ID,
            notificationManager.buildMonitoringNotification()
        )
        serviceScope.launch {
            nudgeRepository.incomingNudges
                // Same rationale as CommunityDashboardViewModel's identical guard: a rejected
                // Firestore listener (e.g. a rules mismatch) must not crash this service — it's
                // the one long-running component that's supposed to survive precisely this kind
                // of transient failure, not die and take doomscroll detection down with it.
                .catch { }
                .collect { nudge -> handleNudge(nudge) }
        }
    }

    private fun handleNudge(nudge: Nudge) {
        crisisEpisodeTracker.onNudgeReceived(System.currentTimeMillis(), nudge.categoryLabel)
        when (nudge) {
            is Nudge.Text -> notificationManager.postTextNudgeNotification(nudge.style.message)
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
        // This runs synchronously on the main thread, outside serviceScope, so the scope's
        // CoroutineExceptionHandler can't cover it. Anything thrown here would be an uncaught
        // exception, and two of those in quick succession is enough for Android to disable the
        // accessibility service outright — taking doomscroll detection down until the user
        // notices and re-enables it by hand. Detection dropping a single event is always the
        // better trade than that.
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                    val scrollingPackageName = event.packageName?.toString() ?: return
                    // A scroll event names the app it came from, so it doubles as a foreground-app
                    // signal — and it's the only one available when this service (re)connects while
                    // a monitored app is already on screen, since no window transition follows.
                    // Without this, every scroll in that app is gated out below and detection
                    // silently does nothing until the user navigates away and back. Resolving the
                    // blacklist status costs one suspending lookup, so this event is dropped and the
                    // next one through is gated on the answer.
                    if (foregroundAppTracker.needsForegroundResolution(scrollingPackageName)) {
                        serviceScope.launch { handleForegroundPackageChanged(scrollingPackageName) }
                        return
                    }
                    // Only scroll activity in an app the user opted to monitor should ever feed
                    // crisis detection; an empty blacklist means nothing is being watched, per
                    // ForegroundAppTracker's default-false state before any foreground app is known.
                    if (!foregroundAppTracker.isCurrentForegroundAppBlacklisted) return
                    val signal = ScrollSignal(timestampMillis = System.currentTimeMillis())
                    serviceScope.launch {
                        val result = detectLossOfAgencyUseCase(signal)
                        if (result.offerSystemFallback) startActivity(SystemFallbackActivity.newIntent(this@DoomscrollAccessibilityService))
                    }
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    val foregroundPackageName = event.packageName?.toString() ?: return
                    serviceScope.launch { handleForegroundPackageChanged(foregroundPackageName) }
                }
            }
        } catch (throwable: Throwable) {
            Log.w(TAG, "Dropped an accessibility event after an unexpected failure", throwable)
        }
    }

    private suspend fun handleForegroundPackageChanged(foregroundPackageName: String) {
        val isBlacklisted = monitoredAppsRepository.isMonitored(foregroundPackageName)
        foregroundAppTracker.onForegroundPackageChanged(foregroundPackageName, System.currentTimeMillis(), isBlacklisted)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "DoomscrollService"
    }
}
