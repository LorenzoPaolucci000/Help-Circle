package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.AppInfo

/** Gateway to the device's installed-app listing (backed by `PackageManager` at the OS boundary). */
interface InstalledAppsRepository {
    suspend fun getInstalledApps(): List<AppInfo>
}
