package com.aman.pulsegate.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aman.pulsegate.R
import com.aman.pulsegate.ui.dashboard.DashboardScreen
import com.aman.pulsegate.ui.destinations.AddEditDestinationScreen
import com.aman.pulsegate.ui.destinations.DestinationsScreen
import com.aman.pulsegate.ui.logs.LogsScreen
import com.aman.pulsegate.ui.permission.PermissionScreen

private data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, R.string.nav_dashboard, Icons.Filled.Home),
    BottomNavItem(Screen.Destinations, R.string.nav_destinations, Icons.AutoMirrored.Filled.Send),
    BottomNavItem(Screen.Logs, R.string.nav_logs, Icons.Filled.Menu)
)

@Composable
fun PulseGateNavGraph(
    allPermissionsGranted: Boolean,
    onStartService: () -> Unit,                          // ← ADD: called once after permission granted
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (allPermissionsGranted) Screen.Dashboard.route
    else Screen.Permission.route

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == item.screen.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(item.labelRes),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Permission.route) {
                PermissionScreen(
                    onAllPermissionsGranted = {
                        onStartService()                 // ← START SERVICE: permissions just confirmed
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Permission.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }

            composable(Screen.Destinations.route) {
                DestinationsScreen(
                    onAddDestination = {
                        navController.navigate(Screen.AddDestination.route)
                    },
                    onEditDestination = { destinationId ->
                        navController.navigate(Screen.EditDestination.createRoute(destinationId))
                    }
                )
            }

            composable(Screen.AddDestination.route) {
                AddEditDestinationScreen(
                    destinationId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditDestination.route,
                arguments = listOf(
                    navArgument("destinationId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                // getLong returns 0L when key missing — treat 0L as absent → null → Add mode
                val destinationId = backStackEntry.arguments
                    ?.getLong("destinationId")
                    ?.takeIf { it != 0L }
                AddEditDestinationScreen(
                    destinationId = destinationId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Logs.route) {
                LogsScreen()
            }
        }
    }
}