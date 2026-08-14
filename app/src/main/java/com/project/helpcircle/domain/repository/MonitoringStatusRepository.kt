package com.project.helpcircle.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Reports whether passive doomscroll monitoring is actually running right now.
 *
 * This exists because monitoring can stop without the app having any part in it: the OS can revoke
 * the passive-monitoring permission on its own (some manufacturers' battery/privacy managers clear
 * it after a period of inactivity), leaving the app running normally while silently detecting
 * nothing. Nothing in the app can prevent that, so the next best thing is to notice and say so
 * rather than keep presenting a score the app can no longer keep up to date.
 */
interface MonitoringStatusRepository {

    /** Emits whether monitoring is active, re-emitting whenever the underlying permission changes. */
    val isMonitoringActive: Flow<Boolean>

    /** One-shot check, for callers that need the current value without collecting. */
    fun isMonitoringActiveNow(): Boolean
}
