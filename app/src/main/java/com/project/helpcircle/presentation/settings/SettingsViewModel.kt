package com.project.helpcircle.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.model.AppCategory
import com.project.helpcircle.domain.model.AppInfo
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import com.project.helpcircle.domain.usecase.GetInstalledAppsUseCase
import com.project.helpcircle.domain.usecase.LeaveCommunityUseCase
import com.project.helpcircle.presentation.common.NETWORK_TIMEOUT_MILLIS
import com.project.helpcircle.presentation.common.SLOW_CONNECTION_MESSAGE
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** UI state for [SettingsScreen]: the device's installed apps, and which ones are pending inclusion on the monitored-apps blacklist. */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val apps: List<AppInfo> = emptyList(),
    val pendingMonitoredPackageNames: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val showLeaveConfirmation: Boolean = false,
    val isLeavingCommunity: Boolean = false,
    val leaveError: String? = null,
    val leaveTimedOut: Boolean = false,
    val hasLeftCommunity: Boolean = false
) {
    /** Apps matching [searchQuery], grouped by [AppCategory] for the settings list. */
    val appsByCategory: Map<AppCategory, List<AppInfo>>
        get() = apps
            .filter { it.displayName.contains(searchQuery, ignoreCase = true) }
            .groupBy { it.category }

    /**
     * How many monitorable apps are currently switched on, and how many aren't.
     *
     * Counted over the whole of [apps] rather than the search-filtered view, so typing in the
     * search box narrows the list without appearing to change what is being monitored.
     */
    val trackedCount: Int get() = apps.count { it.packageName in pendingMonitoredPackageNames }
    val excludedCount: Int get() = apps.size - trackedCount
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getInstalledApps: GetInstalledAppsUseCase,
    private val monitoredAppsRepository: MonitoredAppsRepository,
    private val communityRepository: CommunityRepository,
    private val leaveCommunity: LeaveCommunityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var savedMonitoredPackageNames: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            val apps = getInstalledApps()
            val monitored = monitoredAppsRepository.monitoredPackageNames.first()
            savedMonitoredPackageNames = monitored
            _uiState.update {
                it.copy(isLoading = false, apps = apps, pendingMonitoredPackageNames = monitored)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /** Toggles [packageName]'s pending blacklist membership; takes effect only once [onSaveClicked] is pressed. */
    fun onAppToggled(packageName: String) {
        _uiState.update { state ->
            val pending = state.pendingMonitoredPackageNames
            state.copy(
                pendingMonitoredPackageNames = if (packageName in pending) {
                    pending - packageName
                } else {
                    pending + packageName
                },
                saveMessage = null
            )
        }
    }

    fun onSaveClicked() {
        val pending = _uiState.value.pendingMonitoredPackageNames
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveMessage = null) }
            (pending - savedMonitoredPackageNames).forEach {
                monitoredAppsRepository.setMonitored(it, isMonitored = true)
            }
            (savedMonitoredPackageNames - pending).forEach {
                monitoredAppsRepository.setMonitored(it, isMonitored = false)
            }
            savedMonitoredPackageNames = pending
            _uiState.update { it.copy(isSaving = false, saveMessage = "Configuration saved") }
        }
    }

    fun onLeaveCommunityClicked() {
        _uiState.update { it.copy(showLeaveConfirmation = true, leaveError = null, leaveTimedOut = false) }
    }

    fun onLeaveCommunityDismissed() {
        _uiState.update { it.copy(showLeaveConfirmation = false, leaveError = null, leaveTimedOut = false) }
    }

    fun onLeaveCommunityConfirmed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLeavingCommunity = true, leaveError = null, leaveTimedOut = false) }
            try {
                withTimeout(NETWORK_TIMEOUT_MILLIS) {
                    val communityId = communityRepository.getActiveCommunityId()
                    if (communityId != null) {
                        leaveCommunity(communityId)
                    }
                }
                _uiState.update {
                    it.copy(isLeavingCommunity = false, showLeaveConfirmation = false, hasLeftCommunity = true)
                }
            } catch (e: TimeoutCancellationException) {
                // Safe to retry as-is: leaveCommunity() deletes the caller's own member doc and
                // only decrements activeMembers if that doc still exists (see
                // CommunityRepositoryImpl's transaction), so a retry that finds it already gone —
                // because the timed-out attempt actually landed late in the background — just
                // no-ops instead of double-decrementing.
                _uiState.update {
                    it.copy(isLeavingCommunity = false, leaveError = SLOW_CONNECTION_MESSAGE, leaveTimedOut = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLeavingCommunity = false, leaveError = "Couldn't leave that circle. Try again.")
                }
            }
        }
    }

    fun onLeftCommunityHandled() {
        _uiState.update { it.copy(hasLeftCommunity = false) }
    }
}
