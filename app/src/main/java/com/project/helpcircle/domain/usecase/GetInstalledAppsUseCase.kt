package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.AppCategory
import com.project.helpcircle.domain.model.AppInfo
import com.project.helpcircle.domain.repository.InstalledAppsRepository

/**
 * Lists the device's launchable apps for the monitored-apps settings screen to choose from,
 * restricted to the feed-style apps this project targets. An app is monitorable if its package is
 * on [ALLOWED_PACKAGE_NAMES] — needed because category self-declaration turned out unreliable on
 * real devices (YouTube declares neither Social nor Video) — or if it's a *non-system* app whose
 * OS-reported category is Social or Video. System apps are excluded from that category-based path
 * specifically (rather than not at all) because plenty of stock OS components self-declare Social
 * too: confirmed on-device, the phone's own pre-installed Messages and Contacts apps both report
 * `CATEGORY_SOCIAL`, so a system app only ever becomes monitorable via the explicit allowlist, never
 * by category alone. [BLOCKED_PACKAGE_NAMES] overrides every other check, since some messaging apps
 * self-declare as Social too (confirmed on-device: WhatsApp does) and must never be selectable
 * regardless of category, system status, or allowlist membership.
 */
class GetInstalledAppsUseCase(
    private val installedAppsRepository: InstalledAppsRepository
) {
    suspend operator fun invoke(): List<AppInfo> =
        installedAppsRepository.getInstalledApps()
            .filter { it.packageName !in BLOCKED_PACKAGE_NAMES }
            .filter {
                it.packageName in ALLOWED_PACKAGE_NAMES ||
                    (!it.isSystemApp && (it.category == AppCategory.SOCIAL || it.category == AppCategory.VIDEO))
            }
            .sortedBy { it.displayName.lowercase() }

    private companion object {
        /** Feed/short-video apps whose OS-declared category isn't reliably Social or Video. */
        val ALLOWED_PACKAGE_NAMES = setOf(
            "com.google.android.youtube", // YouTube (incl. Shorts)
            "com.instagram.android", // Instagram
            "com.zhiliaoapp.musically", // TikTok
            "com.twitter.android", // X (Twitter)
            "com.facebook.katana" // Facebook
        )

        /** Messaging apps that self-declare a Social category but must never be monitorable. */
        val BLOCKED_PACKAGE_NAMES = setOf(
            "com.whatsapp", // WhatsApp
            "com.whatsapp.w4b", // WhatsApp Business
            "org.telegram.messenger", // Telegram
            "com.facebook.orca" // Facebook Messenger
        )
    }
}
