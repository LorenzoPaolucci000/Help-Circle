package com.project.helpcircle.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.usecase.JoinCommunityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinCommunityUiState(
    val communityId: String = "",
    val isJoining: Boolean = false,
    val errorMessage: String? = null,
    val hasJoined: Boolean = false
)

@HiltViewModel
class JoinCommunityViewModel @Inject constructor(
    private val joinCommunity: JoinCommunityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinCommunityUiState())
    val uiState: StateFlow<JoinCommunityUiState> = _uiState.asStateFlow()

    fun onCommunityIdChanged(communityId: String) {
        _uiState.update { it.copy(communityId = communityId, errorMessage = null) }
    }

    fun onJoinClicked() {
        val communityId = _uiState.value.communityId.trim()
        if (communityId.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, errorMessage = null) }
            try {
                joinCommunity(communityId)
                _uiState.update { it.copy(isJoining = false, hasJoined = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isJoining = false, errorMessage = "Couldn't join that circle. Check the code and try again.")
                }
            }
        }
    }
}
