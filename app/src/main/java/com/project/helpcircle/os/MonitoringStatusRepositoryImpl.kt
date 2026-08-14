package com.project.helpcircle.os

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.project.helpcircle.domain.repository.MonitoringStatusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * [MonitoringStatusRepository] backed by `AccessibilityManager`, kept at the OS boundary per Clean
 * Architecture — the domain layer only ever sees a boolean, never the accessibility APIs behind it.
 *
 * Liveness is read from the enabled-services list rather than from whether this app's own process
 * is alive, because those turned out to be independent: on the project's test device the service
 * stopped receiving events while the process kept running normally, with no crash of any kind —
 * the OS had simply cleared the relevant secure settings out from under it. Changes are observed
 * via a `ContentObserver` on exactly those two settings, so both revocation paths are caught: the
 * global accessibility switch being turned off, and this service alone being dropped from the
 * enabled list while accessibility itself stays on for other services.
 */
class MonitoringStatusRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MonitoringStatusRepository {

    private val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private val monitoringService = ComponentName(context, DoomscrollAccessibilityService::class.java)

    override val isMonitoringActive: Flow<Boolean> = callbackFlow {
        trySend(isMonitoringActiveNow())

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(isMonitoringActiveNow())
            }
        }
        val contentResolver = context.contentResolver
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            observer
        )
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED),
            false,
            observer
        )

        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()

    override fun isMonitoringActiveNow(): Boolean =
        accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { ComponentName.unflattenFromString(it.id) == monitoringService }
}
