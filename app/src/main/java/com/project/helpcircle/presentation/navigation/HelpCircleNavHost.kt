package com.project.helpcircle.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.helpcircle.presentation.community.JoinCommunityScreen
import com.project.helpcircle.presentation.onboarding.AccessibilityPermissionScreen
import com.project.helpcircle.presentation.onboarding.NicknameSetupScreen
import com.project.helpcircle.presentation.onboarding.SelectMonitoredAppsScreen
import com.project.helpcircle.presentation.onboarding.WelcomeScreen
import com.project.helpcircle.presentation.startup.StartupDestination
import com.project.helpcircle.presentation.startup.StartupScreen

@Composable
fun HelpCircleNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    initialTab: TabDestination? = null
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Startup.route,
        modifier = modifier
    ) {
        composable(Destination.Startup.route) {
            StartupScreen(
                onDestinationResolved = { destination ->
                    val route = when (destination) {
                        // A user with no nickname yet is a first-run user, so they start at the
                        // welcome screen rather than being asked for a nickname cold.
                        StartupDestination.NICKNAME_SETUP -> Destination.Welcome.route
                        StartupDestination.JOIN_COMMUNITY -> Destination.JoinCommunity.route
                        StartupDestination.COMMUNITY_DASHBOARD -> Destination.MainTabs.route
                    }
                    navController.navigate(route) {
                        popUpTo(Destination.Startup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Destination.NicknameSetup.route) {
                        popUpTo(Destination.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.NicknameSetup.route) {
            NicknameSetupScreen(
                onNicknameSaved = {
                    navController.navigate(Destination.SelectMonitoredApps.route) {
                        popUpTo(Destination.NicknameSetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.SelectMonitoredApps.route) {
            SelectMonitoredAppsScreen(
                onContinue = {
                    // Reached either from onboarding (NicknameSetup already popped away, nothing
                    // to return to) or from JoinCommunityScreen's "Go to Settings" banner (pushed
                    // on top, still sitting right below). In the latter case just pop back to that
                    // existing instance instead of pushing a second one on top of it — the
                    // accessibility permission step only belongs in the first-time onboarding
                    // path, since reaching this screen via the banner means onboarding (and that
                    // permission grant) already happened once before.
                    if (navController.previousBackStackEntry?.destination?.route == Destination.JoinCommunity.route) {
                        navController.popBackStack()
                    } else {
                        // No popUpTo here: this screen stays on the back stack so
                        // AccessibilityPermissionScreen's "Back" button has somewhere to return to.
                        navController.navigate(Destination.AccessibilityPermission.route)
                    }
                }
            )
        }
        composable(Destination.AccessibilityPermission.route) {
            AccessibilityPermissionScreen(
                onContinue = {
                    navController.navigate(Destination.JoinCommunity.route) {
                        popUpTo(Destination.SelectMonitoredApps.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Destination.JoinCommunity.route) {
            JoinCommunityScreen(
                onJoined = {
                    navController.navigate(Destination.MainTabs.route) {
                        popUpTo(Destination.JoinCommunity.route) { inclusive = true }
                    }
                },
                onGoToMonitoredApps = {
                    navController.navigate(Destination.SelectMonitoredApps.route)
                }
            )
        }
        composable(Destination.MainTabs.route) {
            MainTabsScreen(
                initialTab = initialTab ?: TabDestination.COMMUNITY,
                onLeftCommunity = {
                    navController.navigate(Destination.JoinCommunity.route) {
                        popUpTo(Destination.MainTabs.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
