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
 * launchable, non-system apps (excluding this app itself) are surfaced, since the monitored-apps
 * blacklist is only ever meaningful for apps the user can actually open and scroll through.
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
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { applicationInfo ->
                AppInfo(
                    packageName = applicationInfo.packageName,
                    displayName = packageManager.getApplicationLabel(applicationInfo).toString(),
                    category = applicationInfo.toAppCategory()
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
