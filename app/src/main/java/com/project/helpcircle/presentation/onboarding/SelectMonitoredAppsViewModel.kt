package com.project.helpcircle.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.model.AppCategory
import com.project.helpcircle.domain.model.AppInfo
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import com.project.helpcircle.domain.usecase.GetInstalledAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the mandatory onboarding step: the user must pick at least one app to monitor before they can continue on to joining/creating a circle. */
data class SelectMonitoredAppsUiState(
    val isLoading: Boolean = true,
    val apps: List<AppInfo> = emptyList(),
    val pendingMonitoredPackageNames: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSaving: Boolean = false,
    val showEmptySelectionError: Boolean = false,
    val isDone: Boolean = false
) {
    /** Apps matching [searchQuery], grouped by [AppCategory] for the selection list. */
    val appsByCategory: Map<AppCategory, List<AppInfo>>
        get() = apps
            .filter { it.displayName.contains(searchQuery, ignoreCase = true) }
            .groupBy { it.category }
}

@HiltViewModel
class SelectMonitoredAppsViewModel @Inject constructor(
    private val getInstalledApps: GetInstalledAppsUseCase,
    private val monitoredAppsRepository: MonitoredAppsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectMonitoredAppsUiState())
    val uiState: StateFlow<SelectMonitoredAppsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = getInstalledApps()
            val monitored = monitoredAppsRepository.monitoredPackageNames.first()
            _uiState.update {
                it.copy(isLoading = false, apps = apps, pendingMonitoredPackageNames = monitored)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onAppToggled(packageName: String) {
        _uiState.update { state ->
            val pending = state.pendingMonitoredPackageNames
            state.copy(
                pendingMonitoredPackageNames = if (packageName in pending) pending - packageName else pending + packageName,
                showEmptySelectionError = false
            )
        }
    }

    fun onContinueClicked() {
        val pending = _uiState.value.pendingMonitoredPackageNames
        if (pending.isEmpty()) {
            _uiState.update { it.copy(showEmptySelectionError = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            pending.forEach { monitoredAppsRepository.setMonitored(it, isMonitored = true) }
            _uiState.update { it.copy(isSaving = false, isDone = true) }
        }
    }

    /** Consumes the one-shot [SelectMonitoredAppsUiState.isDone] navigation event so returning to this screen later (e.g. via a Back button further along the flow) doesn't immediately re-trigger it. */
    fun onDoneHandled() {
        _uiState.update { it.copy(isDone = false) }
    }
}
