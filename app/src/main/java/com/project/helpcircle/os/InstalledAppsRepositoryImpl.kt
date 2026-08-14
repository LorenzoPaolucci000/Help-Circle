package com.project.helpcircle.os

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.project.helpcircle.domain.model.AppCategory
import com.project.helpcircle.domain.model.AppInfo
import com.project.helpcircle.domain.repository.InstalledAppsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * [InstalledAppsRepository] backed by `PackageManager` — the only place in the app that queries
 * the device's installed-app listing, kept at the OS boundary per Clean Architecture. Only
 * launchable apps (excluding this app itself) are surfaced, tagged with [AppInfo.isSystemApp]
 * (`ApplicationInfo.FLAG_SYSTEM`) rather than filtered on it here — deciding which apps are
 * actually monitorable, system or not, is [com.project.helpcircle.domain.usecase.GetInstalledAppsUseCase]'s
 * business rule to make, not this repository's. A blanket system-app exclusion at this layer was
 * tried and reverted: several target feed apps (e.g. YouTube) ship pre-installed as system apps on
 * GMS-certified devices, so it silently defeated that use case's allowlist — confirmed on-device
 * (Vivo V2110).
 * Requires the `<queries>` package-visibility declaration in AndroidManifest.xml on API 30+.
 */
class InstalledAppsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : InstalledAppsRepository {

    override suspend fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        @Suppress("DEPRECATION")
        val resolvedActivities = packageManager.queryIntentActivities(launcherIntent, 0)

        return resolvedActivities
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { applicationInfo ->
                AppInfo(
                    packageName = applicationInfo.packageName,
                    displayName = packageManager.getApplicationLabel(applicationInfo).toString(),
                    category = applicationInfo.toAppCategory(),
                    isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                )
            }
            .toList()
    }

    private fun ApplicationInfo.toAppCategory(): AppCategory = when (category) {
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
        ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
        ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
        ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
        else -> AppCategory.OTHER
    }
}
