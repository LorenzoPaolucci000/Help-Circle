package com.project.helpcircle.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persists the user's personal blacklist of apps to monitor for doomscroll detection — a manual
 * opt-in list, not a system-wide scan, so only packages the user has explicitly flagged are ever
 * watched for foreground/background transitions.
 */
interface MonitoredAppsRepository {
    val monitoredPackageNames: Flow<Set<String>>
    suspend fun setMonitored(packageName: String, isMonitored: Boolean)
    suspend fun isMonitored(packageName: String): Boolean
}
