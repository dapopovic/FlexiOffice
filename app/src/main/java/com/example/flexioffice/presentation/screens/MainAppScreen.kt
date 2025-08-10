package com.example.flexioffice.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.flexioffice.navigation.FlexiOfficeNavigation
import com.example.flexioffice.navigation.FlexiOfficeRoutes
import com.example.flexioffice.presentation.InAppNotificationViewModel
import com.example.flexioffice.presentation.MainViewModel
import com.example.flexioffice.presentation.components.InAppNotificationBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController,
    inAppNotificationViewModel: InAppNotificationViewModel = hiltViewModel(),
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val notificationState by inAppNotificationViewModel.notificationState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val hideBottomBar =
                currentDestination?.route ==
                    com.example.flexioffice.navigation.FlexiOfficeRoutes.GeofencingSettings.route
            val showBottomBar = uiState.availableNavItems.isNotEmpty() && !hideBottomBar

            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                NavigationBar(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                ) {
                    uiState.availableNavItems.forEach { item ->
                        val isSelected =
                            currentDestination?.hierarchy?.any { it.route == item.route } == true

                        // Animate icon scale and label alpha on selection change
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.12f else 1f,
                            animationSpec = tween(durationMillis = 180),
                            label = "nav_icon_scale",
                        )
                        val labelAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.85f,
                            animationSpec = tween(durationMillis = 180),
                            label = "nav_label_alpha",
                        )

                        NavigationBarItem(
                            icon = {
                                if (item.badgeCount != null) {
                                    BadgedBox(
                                        badge = {
                                            Badge { Text(item.badgeCount.toString()) }
                                        },
                                    ) {
                                        Crossfade(targetState = isSelected, label = "nav_icon_crossfade") { selected ->
                                            Icon(
                                                imageVector =
                                                    if (selected) {
                                                        ImageVector.vectorResource(item.selectedIconId)
                                                    } else {
                                                        ImageVector.vectorResource(item.unselectedIconId)
                                                    },
                                                contentDescription = item.title,
                                                modifier = Modifier.scale(iconScale),
                                            )
                                        }
                                    }
                                } else {
                                    Crossfade(targetState = isSelected, label = "nav_icon_crossfade") { selected ->
                                        Icon(
                                            imageVector =
                                                if (selected) {
                                                    ImageVector.vectorResource(item.selectedIconId)
                                                } else {
                                                    ImageVector.vectorResource(item.unselectedIconId)
                                                },
                                            contentDescription = item.title,
                                            modifier = Modifier.scale(iconScale),
                                        )
                                    }
                                }
                            },
                            label = { Text(item.title, modifier = Modifier.graphicsLayer(alpha = labelAlpha)) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected
                                    // item
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Navigate to the resolved default route once loading finishes, without rebuilding NavHost
            LaunchedEffect(uiState.isLoading, uiState.availableNavItems) {
                if (!uiState.isLoading) {
                    val targetRoute = mainViewModel.getDefaultRoute()
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    // Only redirect automatically from the Loading screen to avoid hijacking active screens
                    if (currentRoute == FlexiOfficeRoutes.Loading.route && currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
            // Main Navigation Content
            Box(modifier = Modifier.padding(innerPadding)) {
                FlexiOfficeNavigation(
                    navController = navController,
                    // Keep a stable start destination to avoid re-creating the NavHost on state changes
                    startDestination = FlexiOfficeRoutes.Loading.route,
                    mainViewModel = mainViewModel,
                )
            }

            // In-App Notification Banner - positioned as overlay
            InAppNotificationBanner(
                modifier = Modifier.statusBarsPadding(),
                title = notificationState.title,
                message = notificationState.message,
                type = notificationState.type,
                isVisible = notificationState.isVisible,
                onDismiss = { inAppNotificationViewModel.dismissNotification() },
                onAction = {
                    // Navigate to appropriate screen based on notification type
                    val targetRoute =
                        when (notificationState.type) {
                            "booking_status_update" -> "booking"
                            "new_booking_request" -> "requests"
                            "team_invitation", "team_invitation_response", "team_invitation_cancelled" -> "teams"
                            else -> "calendar"
                        }

                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    inAppNotificationViewModel.dismissNotification()
                },
            )
        }
    }
}
