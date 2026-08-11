package com.project.helpcircle.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.usecase.CreateCommunityUseCase
import com.project.helpcircle.domain.usecase.JoinCommunityByInviteCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class JoinCommunityTab { JOIN, CREATE }

data class JoinCommunityUiState(
    val selectedTab: JoinCommunityTab = JoinCommunityTab.JOIN,
    val inviteCodeInput: String = "",
    val isJoining: Boolean = false,
    val joinError: String? = null,
    val isCreating: Boolean = false,
    val createError: String? = null,
    val createdInviteCode: String? = null,
    val hasJoined: Boolean = false
)

@HiltViewModel
class JoinCommunityViewModel @Inject constructor(
    private val joinCommunityByInviteCode: JoinCommunityByInviteCodeUseCase,
    private val createCommunity: CreateCommunityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinCommunityUiState())
    val uiState: StateFlow<JoinCommunityUiState> = _uiState.asStateFlow()

    fun onTabSelected(tab: JoinCommunityTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onInviteCodeInputChanged(inviteCode: String) {
        _uiState.update { it.copy(inviteCodeInput = inviteCode, joinError = null) }
    }

    fun onJoinClicked() {
        val inviteCode = _uiState.value.inviteCodeInput.trim()
        if (inviteCode.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, joinError = null) }
            try {
                val joined = joinCommunityByInviteCode(inviteCode)
                if (joined != null) {
                    _uiState.update { it.copy(isJoining = false, hasJoined = true) }
                } else {
                    _uiState.update { it.copy(isJoining = false, joinError = "Code not found") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isJoining = false, joinError = "Couldn't join that circle. Try again.")
                }
            }
        }
    }

    fun onCreateClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }
            try {
                val state = createCommunity()
                _uiState.update { it.copy(isCreating = false, createdInviteCode = state.inviteCode) }
            } catch (e: CancellationException) {
                throw e
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
