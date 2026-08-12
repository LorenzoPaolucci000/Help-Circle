package com.project.helpcircle.data.repository

import com.project.helpcircle.data.local.dao.MonitoredAppDao
import com.project.helpcircle.data.local.entity.MonitoredAppEntity
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed [MonitoredAppsRepository]: persists the user's monitored-apps blacklist locally. */
class MonitoredAppsRepositoryImpl @Inject constructor(
    private val monitoredAppDao: MonitoredAppDao
) : MonitoredAppsRepository {

    override val monitoredPackageNames: Flow<Set<String>> =
        monitoredAppDao.observePackageNames().map { it.toSet() }

    override suspend fun setMonitored(packageName: String, isMonitored: Boolean) {
        if (isMonitored) {
            monitoredAppDao.insert(MonitoredAppEntity(packageName))
        } else {
            monitoredAppDao.delete(MonitoredAppEntity(packageName))
        }
    }

    override suspend fun isMonitored(packageName: String): Boolean = monitoredAppDao.isMonitored(packageName)
}
