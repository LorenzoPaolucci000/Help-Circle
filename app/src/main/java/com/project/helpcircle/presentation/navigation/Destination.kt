package com.project.helpcircle.presentation.navigation

sealed class Destination(val route: String) {
    data object Startup : Destination("startup")
    data object NicknameSetup : Destination("nickname_setup")

    /** Mandatory onboarding step: pick at least one app to monitor before joining/creating a circle. */
    data object SelectMonitoredApps : Destination("select_monitored_apps")
    data object JoinCommunity : Destination("join_community")

    /** The bottom-nav-tabbed container hosting the Me/Community/Settings screens; see [TabDestination]. */
    data object MainTabs : Destination("main_tabs")
}
