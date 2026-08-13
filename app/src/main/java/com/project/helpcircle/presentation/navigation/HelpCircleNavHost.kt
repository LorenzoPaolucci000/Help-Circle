package com.project.helpcircle.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.helpcircle.presentation.community.JoinCommunityScreen
import com.project.helpcircle.presentation.onboarding.NicknameSetupScreen
import com.project.helpcircle.presentation.startup.StartupDestination
import com.project.helpcircle.presentation.startup.StartupScreen

@Composable
fun HelpCircleNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
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
                        StartupDestination.NICKNAME_SETUP -> Destination.NicknameSetup.route
                        StartupDestination.JOIN_COMMUNITY -> Destination.JoinCommunity.route
                        StartupDestination.COMMUNITY_DASHBOARD -> Destination.MainTabs.route
                    }
                    navController.navigate(route) {
                        popUpTo(Destination.Startup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.NicknameSetup.route) {
            NicknameSetupScreen(
                onNicknameSaved = {
                    navController.navigate(Destination.JoinCommunity.route) {
                        popUpTo(Destination.NicknameSetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.JoinCommunity.route) {
            JoinCommunityScreen(
                onJoined = {
                    navController.navigate(Destination.MainTabs.route) {
                        popUpTo(Destination.JoinCommunity.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.MainTabs.route) {
            MainTabsScreen(
                onLeftCommunity = {
                    navController.navigate(Destination.JoinCommunity.route) {
                        popUpTo(Destination.MainTabs.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
