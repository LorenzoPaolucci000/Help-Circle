package com.project.helpcircle.presentation.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where a launch should land once it's known whether this device already has a nickname and/or an active circle. */
enum class StartupDestination { NICKNAME_SETUP, JOIN_COMMUNITY, COMMUNITY_DASHBOARD }

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<StartupDestination?>(null)
    val destination: StateFlow<StartupDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val hasNickname = userRepository.getOrCreateIdentity().nickname.isNotBlank()
            _destination.value = when {
                !hasNickname -> StartupDestination.NICKNAME_SETUP
                communityRepository.getActiveCommunityId() == null -> StartupDestination.JOIN_COMMUNITY
                else -> StartupDestination.COMMUNITY_DASHBOARD
            }
        }
    }
}
