package com.project.helpcircle.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.repository.UserRepository
import com.project.helpcircle.domain.usecase.NicknameValidationResult
import com.project.helpcircle.domain.usecase.ValidateNicknameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NicknameSetupUiState(
    val nickname: String = "",
    val validationResult: NicknameValidationResult? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class NicknameSetupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val validateNickname: ValidateNicknameUseCase,
    private val nicknameGenerator: NicknameGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(NicknameSetupUiState())
    val uiState: StateFlow<NicknameSetupUiState> = _uiState.asStateFlow()

    fun onNicknameChanged(nickname: String) {
        _uiState.update {
            it.copy(
                nickname = nickname,
                validationResult = if (nickname.isEmpty()) null else validateNickname(nickname)
            )
        }
    }

    fun onGenerateClicked() {
        val generated = nicknameGenerator.generate()
        _uiState.update { it.copy(nickname = generated, validationResult = validateNickname(generated)) }
    }

    fun onContinueClicked() {
        val state = _uiState.value
        if (state.validationResult != NicknameValidationResult.Valid) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            userRepository.saveNickname(state.nickname)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
