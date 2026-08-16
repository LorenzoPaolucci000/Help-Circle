package com.project.helpcircle.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The four peer top-level screens hosted behind [MainTabsScreen]'s bottom
 * [androidx.compose.material3.NavigationBar]. Declaration order is bar order, since
 * [MainTabsScreen] renders one item per entry.
 */
enum class TabDestination(val route: String, val label: String, val icon: ImageVector) {
    ME("me", "Me", Icons.Filled.Person),
    COMMUNITY("community", "Community", Icons.Filled.Home),
    HELP("help", "Help", Icons.Filled.Favorite),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}
