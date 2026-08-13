package com.project.helpcircle.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.helpcircle.presentation.community.CommunityDashboardScreen
import com.project.helpcircle.presentation.settings.SettingsScreen

/**
 * Hosts the three peer top-level screens (Me/Community/Settings) behind a Material3
 * [NavigationBar]. Tab switches use the standard "multiple back stacks" pattern
 * (saveState/restoreState + launchSingleTop against the graph's start destination) so each tab
 * keeps its own state when the user switches away and back, rather than resetting on every tap.
 * Starts on the Community tab, since that's the screen a returning or freshly-onboarded user
 * expects to land on.
 */
@Composable
fun MainTabsScreen(
    onLeftCommunity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route
            NavigationBar {
                TabDestination.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TabDestination.COMMUNITY.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TabDestination.ME.route) {
                MeTabPlaceholder()
            }
            composable(TabDestination.COMMUNITY.route) {
                CommunityDashboardScreen()
            }
            composable(TabDestination.SETTINGS.route) {
                SettingsScreen(onLeftCommunity = onLeftCommunity)
            }
        }
    }
}

/** Stand-in for the "Me" tab's real content until the personal agency home screen is built. */
@Composable
private fun MeTabPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Coming soon", style = MaterialTheme.typography.bodyMedium)
    }
}
