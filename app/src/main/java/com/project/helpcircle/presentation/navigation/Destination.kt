package com.project.helpcircle.presentation.navigation

sealed class Destination(val route: String) {
    data object Startup : Destination("startup")

    /**
     * First-run only: explains what the app is for and what it does with the user's data, before
     * anything is asked of them. Returning users never see it — [Startup] routes past it.
     */
    data object Welcome : Destination("welcome")
    data object NicknameSetup : Destination("nickname_setup")

    /** Mandatory onboarding step: pick at least one app to monitor before joining/creating a circle. */
    data object SelectMonitoredApps : Destination("select_monitored_apps")

    /** Mandatory onboarding step: grant the accessibility permission scroll detection depends on. */
    data object AccessibilityPermission : Destination("accessibility_permission")
    data object JoinCommunity : Destination("join_community")

    /** The bottom-nav-tabbed container hosting the Me/Community/Settings screens; see [TabDestination]. */
    data object MainTabs : Destination("main_tabs")
}
