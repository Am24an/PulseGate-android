package com.aman.pulsegate.ui.navigation

sealed class Screen(val route: String) {

    // ── Entry
    data object Permission : Screen("permission")

    // ── Bottom Nav
    data object Dashboard : Screen("dashboard")
    data object Destinations : Screen("destinations")
    data object Logs : Screen("logs")

    // ── Sub-screens
    data object AddDestination : Screen("destinations/add")

    // destinationId is Long — passed as nav argument
    data object EditDestination : Screen("destinations/edit/{destinationId}") {
        fun createRoute(destinationId: Long) = "destinations/edit/$destinationId"
    }
}