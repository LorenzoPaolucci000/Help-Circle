package com.project.helpcircle.data.repository

import com.project.helpcircle.data.local.dao.AgencyStateDao
import com.project.helpcircle.data.local.dao.FocusSessionDao
import com.project.helpcircle.data.local.entity.AgencyStateEntity
import com.project.helpcircle.data.local.entity.FocusSessionEntity
import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.FocusSession
import com.project.helpcircle.domain.repository.AgencyRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/** Room-backed [AgencyRepository]: persists the current agency index/state and session history locally. */
class AgencyRepositoryImpl @Inject constructor(
    private val agencyStateDao: AgencyStateDao,
    private val focusSessionDao: FocusSessionDao
) : AgencyRepository {

    override val currentAgencyIndex: Flow<AgencyIndex> = agencyStateDao.observe()
        .filterNotNull()
        .map { AgencyIndex.of(it.agencyIndexValue) }

    override val currentAgencyState: Flow<AgencyState> = agencyStateDao.observe()
        .filterNotNull()
        .map { it.agencyState.toAgencyState() }

    override suspend fun recordFocusSession(session: FocusSession) {
        focusSessionDao.insert(session.toEntity())
    }

    override suspend fun updateAgencyIndex(index: AgencyIndex) {
        val current = agencyStateDao.observe().firstOrNull()
        agencyStateDao.upsert(
            AgencyStateEntity(
                agencyIndexValue = index.value,
                agencyState = current?.agencyState ?: AgencyState.Stable.toStorageName()
            )
        )
    }

    override suspend fun reportAgencyState(state: AgencyState) {
        val current = agencyStateDao.observe().firstOrNull()
        agencyStateDao.upsert(
            AgencyStateEntity(
                agencyIndexValue = current?.agencyIndexValue ?: AgencyIndex.BASELINE,
                agencyState = state.toStorageName()
            )
        )
    }
}

private fun AgencyState.toStorageName(): String = when (this) {
    AgencyState.Stable -> "STABLE"
    AgencyState.Warning -> "WARNING"
    AgencyState.Crisis -> "CRISIS"
}

private fun String.toAgencyState(): AgencyState = when (this) {
    "WARNING" -> AgencyState.Warning
    "CRISIS" -> AgencyState.Crisis
    else -> AgencyState.Stable
}

private fun FocusSession.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id,
    startedAtEpochMillis = startedAtEpochMillis,
    endedAtEpochMillis = endedAtEpochMillis,
    peakAgencyState = peakAgencyState.toStorageName()
)
