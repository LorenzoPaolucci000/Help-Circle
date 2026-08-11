package com.project.helpcircle.presentation.navigation

sealed class Destination(val route: String) {
    data object NicknameSetup : Destination("nickname_setup")
    data object CommunityDashboard : Destination("community_dashboard")
}
