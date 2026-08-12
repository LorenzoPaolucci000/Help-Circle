package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.AppInfo
import com.project.helpcircle.domain.repository.InstalledAppsRepository

/** Lists the device's launchable apps for the monitored-apps settings screen to choose from. */
class GetInstalledAppsUseCase(
    private val installedAppsRepository: InstalledAppsRepository
) {
    suspend operator fun invoke(): List<AppInfo> =
        installedAppsRepository.getInstalledApps().sortedBy { it.displayName.lowercase() }
}
