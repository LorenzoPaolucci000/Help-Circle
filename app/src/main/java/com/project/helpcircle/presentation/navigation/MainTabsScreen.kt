package com.project.helpcircle.presentation.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.helpcircle.presentation.common.MonitoringDisabledBanner
import com.project.helpcircle.presentation.common.MonitoringStatusViewModel
import com.project.helpcircle.presentation.community.CommunityDashboardScreen
import com.project.helpcircle.presentation.help.HelpScreen
import com.project.helpcircle.presentation.home.HomeScreen
import com.project.helpcircle.presentation.settings.SettingsScreen
import com.project.helpcircle.ui.theme.Spacing

/**
 * Hosts the four peer top-level screens (Me/Community/Help/Settings) behind a Material3
 * [NavigationBar]. Tab switches use the standard "multiple back stacks" pattern
 * (saveState/restoreState + launchSingleTop against the graph's start destination) so each tab
 * keeps its own state when the user switches away and back, rather than resetting on every tap.
 * Starts on the Community tab, since that's the screen a returning or freshly-onboarded user
 * expects to land on.
 */
@Composable
fun MainTabsScreen(
    onLeftCommunity: () -> Unit,
    modifier: Modifier = Modifier,
    monitoringStatusViewModel: MonitoringStatusViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isMonitoringActive by monitoringStatusViewModel.isMonitoringActive.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
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
                        label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Sits above the tab content rather than inside any one screen: monitoring being off
            // invalidates what every tab is showing, so it shouldn't be dismissible by switching
            // tabs.
            if (!isMonitoringActive) {
                MonitoringDisabledBanner(
                    onTurnBackOnClicked = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm)
                )
            }
            NavHost(
                navController = navController,
                startDestination = TabDestination.COMMUNITY.route
            ) {
                composable(TabDestination.ME.route) {
                    HomeScreen()
                }
                composable(TabDestination.COMMUNITY.route) {
                    CommunityDashboardScreen()
                }
                composable(TabDestination.HELP.route) {
                    HelpScreen()
                }
                composable(TabDestination.SETTINGS.route) {
                    SettingsScreen(onLeftCommunity = onLeftCommunity)
                }
            }
        }
    }
}
