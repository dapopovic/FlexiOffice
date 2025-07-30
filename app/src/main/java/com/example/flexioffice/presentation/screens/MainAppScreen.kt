package com.example.flexioffice.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.flexioffice.navigation.FlexiOfficeNavigation
import com.example.flexioffice.presentation.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController,
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (uiState.availableNavItems.isNotEmpty()) {
                NavigationBar {
                    uiState.availableNavItems.forEach { item ->
                        val isSelected =
                            currentDestination?.hierarchy?.any { it.route == item.route } ==
                                true

                        NavigationBarItem(
                            icon = {
                                if (item.badgeCount != null) {
                                    BadgedBox(
                                        badge = {
                                            Badge { Text(item.badgeCount.toString()) }
                                        },
                                    ) {
                                        Icon(
                                            imageVector =
                                                if (isSelected) {
                                                    ImageVector.vectorResource(
                                                        item.selectedIconId,
                                                    )
                                                } else {
                                                    ImageVector.vectorResource(
                                                        item.unselectedIconId,
                                                    )
                                                },
                                            contentDescription = item.title,
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector =
                                            if (isSelected) {
                                                ImageVector.vectorResource(
                                                    item.selectedIconId,
                                                )
                                            } else {
                                                ImageVector.vectorResource(
                                                    item.unselectedIconId,
                                                )
                                            },
                                        contentDescription = item.title,
                                    )
                                }
                            },
                            label = { Text(item.title) },
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
        Box(modifier = Modifier.padding(innerPadding)) {
            FlexiOfficeNavigation(
                navController = navController,
                startDestination = mainViewModel.getDefaultRoute(),
                mainViewModel = mainViewModel,
            )
        }
    }
}
