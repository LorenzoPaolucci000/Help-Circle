package com.project.helpcircle.domain.model

/** A launchable app on the device, as surfaced to the monitored-apps settings screen. */
data class AppInfo(
    val packageName: String,
    val displayName: String,
    val category: AppCategory,
    val isSystemApp: Boolean = false
)

/** Coarse app category, derived from the OS's own `ApplicationInfo.category` classification. */
enum class AppCategory {
    SOCIAL,
    VIDEO,
    GAME,
    NEWS,
    PRODUCTIVITY,
    OTHER
}
