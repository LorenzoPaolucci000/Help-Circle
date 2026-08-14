package com.project.helpcircle.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.repository.MonitoringStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tracks whether passive monitoring is running, so [MonitoringDisabledBanner] can be shown across
 * the main tabs when it isn't. Seeded with a synchronous read so a cold start doesn't briefly flash
 * the banner before the first emission arrives.
 */
@HiltViewModel
class MonitoringStatusViewModel @Inject constructor(
    monitoringStatusRepository: MonitoringStatusRepository
) : ViewModel() {

    private val _isMonitoringActive = MutableStateFlow(monitoringStatusRepository.isMonitoringActiveNow())
    val isMonitoringActive: StateFlow<Boolean> = _isMonitoringActive.asStateFlow()

    init {
        viewModelScope.launch {
            monitoringStatusRepository.isMonitoringActive.collect { isActive ->
                _isMonitoringActive.value = isActive
            }
        }
    }
}
