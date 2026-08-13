package com.project.helpcircle.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import com.project.helpcircle.domain.usecase.CreateCommunityUseCase
import com.project.helpcircle.domain.usecase.JoinCommunityByInviteCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

enum class JoinCommunityTab { JOIN, CREATE }

private const val NETWORK_TIMEOUT_MILLIS = 15_000L
private const val SLOW_CONNECTION_MESSAGE = "Connection is slow — tap to try again"

data class JoinCommunityUiState(
    val selectedTab: JoinCommunityTab = JoinCommunityTab.JOIN,
    val inviteCodeInput: String = "",
    val isJoining: Boolean = false,
    val joinError: String? = null,
    val joinTimedOut: Boolean = false,
    val isCreating: Boolean = false,
    val createError: String? = null,
    val createTimedOut: Boolean = false,
    val createdInviteCode: String? = null,
    val hasJoined: Boolean = false,
    // Optimistic default so the blocking banner doesn't flash on screen before the first read of
    // the (fast, Room-backed) blacklist actually completes.
    val hasMonitoredApps: Boolean = true
)

@HiltViewModel
class JoinCommunityViewModel @Inject constructor(
    private val joinCommunityByInviteCode: JoinCommunityByInviteCodeUseCase,
    private val createCommunity: CreateCommunityUseCase,
    monitoredAppsRepository: MonitoredAppsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinCommunityUiState())
    val uiState: StateFlow<JoinCommunityUiState> = _uiState.asStateFlow()

    init {
        monitoredAppsRepository.monitoredPackageNames
            .onEach { monitored -> _uiState.update { it.copy(hasMonitoredApps = monitored.isNotEmpty()) } }
            .launchIn(viewModelScope)
    }

    fun onTabSelected(tab: JoinCommunityTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onInviteCodeInputChanged(inviteCode: String) {
        _uiState.update { it.copy(inviteCodeInput = inviteCode, joinError = null, joinTimedOut = false) }
    }

    fun onJoinClicked() {
        val inviteCode = _uiState.value.inviteCodeInput.trim()
        if (inviteCode.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, joinError = null, joinTimedOut = false) }
            try {
                val joined = withTimeout(NETWORK_TIMEOUT_MILLIS) { joinCommunityByInviteCode(inviteCode) }
                if (joined != null) {
                    _uiState.update { it.copy(isJoining = false, hasJoined = true) }
                } else {
                    _uiState.update { it.copy(isJoining = false, joinError = "Code not found") }
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.update {
                    it.copy(isJoining = false, joinError = SLOW_CONNECTION_MESSAGE, joinTimedOut = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                // Defensive fallback: the live hasMonitoredApps observation above should already
                // keep the join/create actions hidden behind the banner before this can fire, but
                // the use case's own check() is the actual source of truth if that ever races.
                _uiState.update { it.copy(isJoining = false, joinError = e.message) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isJoining = false, joinError = "Couldn't join that circle. Try again.")
                }
            }
        }
    }

    fun onCreateClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null, createTimedOut = false) }
            try {
                val state = withTimeout(NETWORK_TIMEOUT_MILLIS) { createCommunity() }
                _uiState.update { it.copy(isCreating = false, createdInviteCode = state.inviteCode) }
            } catch (e: TimeoutCancellationException) {
                _uiState.update {
                    it.copy(isCreating = false, createError = SLOW_CONNECTION_MESSAGE, createTimedOut = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(isCreating = false, createError = e.message) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isCreating = false, createError = "Couldn't create a circle. Try again.")
                }
            }
        }
    }

    fun onContinueAfterCreateClicked() {
        _uiState.update { it.copy(hasJoined = true) }
    }
}
