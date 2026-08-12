package com.project.helpcircle.presentation.navigation

sealed class Destination(val route: String) {
    data object Startup : Destination("startup")
    data object NicknameSetup : Destination("nickname_setup")
    data object JoinCommunity : Destination("join_community")
    data object CommunityDashboard : Destination("community_dashboard")
}
