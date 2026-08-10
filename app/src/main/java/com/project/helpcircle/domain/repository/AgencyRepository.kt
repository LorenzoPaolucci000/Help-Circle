package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow

/** Persists and exposes the local user's [AgencyIndex] and [AgencyState]. */
interface AgencyRepository {
    val currentAgencyIndex: Flow<AgencyIndex>
    val currentAgencyState: Flow<AgencyState>

    suspend fun recordFocusSession(session: FocusSession)
    suspend fun updateAgencyIndex(index: AgencyIndex)
    suspend fun reportAgencyState(state: AgencyState)
}
