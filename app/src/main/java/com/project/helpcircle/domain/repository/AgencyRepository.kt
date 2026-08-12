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

    /**
     * Adds [deltaAutonomy]/[deltaSupport] to the running Delta_Autonomy/Delta_Support totals,
     * recomputes IA_ind from the baseline formula, persists both, and returns the new index.
     */
    suspend fun adjustAgencyDeltas(deltaAutonomy: Int = 0, deltaSupport: Int = 0): AgencyIndex

    /** The IA_ind value archived at the last weekly reset, or null if none has happened yet. */
    suspend fun getLastArchivedAgencyIndex(): Int?
    suspend fun archiveAgencyIndex(agencyIndexValue: Int)

    suspend fun getLastWeeklyResetAtEpochMillis(): Long?

    /** Zeroes the running Delta_Autonomy/Delta_Support totals (IA_ind back to baseline) and records [atEpochMillis] as the new last-reset time. */
    suspend fun resetAgencyIndexForNewWeek(atEpochMillis: Long)
}
