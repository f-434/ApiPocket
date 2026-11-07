package com.apipocket.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.apipocket.R
import com.apipocket.di.AppContainer
import com.apipocket.ui.collections.CollectionDetailScreen
import com.apipocket.ui.collections.CollectionsScreen
import com.apipocket.ui.collections.CollectionsViewModel
import com.apipocket.ui.environments.EnvironmentDetailScreen
import com.apipocket.ui.environments.EnvironmentsScreen
import com.apipocket.ui.environments.EnvironmentsViewModel
import com.apipocket.ui.history.HistoryScreen
import com.apipocket.ui.history.HistoryViewModel
import com.apipocket.ui.request.RequestScreen
import com.apipocket.ui.request.RequestViewModel
import com.apipocket.ui.settings.SettingsScreen
import com.apipocket.ui.settings.SettingsViewModel

object Routes {
    const val REQUEST = "request"
    const val HISTORY = "history"
    const val COLLECTIONS = "collections"
    const val ENVIRONMENTS = "environments"
    const val SETTINGS = "settings"
    const val COLLECTION_DETAIL = "collection/{collectionId}"
    const val ENVIRONMENT_DETAIL = "environment/{environmentId}"

    fun collectionDetail(id: String) = "collection/$id"
    fun environmentDetail(id: String) = "environment/$id"

    val bottomRoutes = setOf(REQUEST, HISTORY, COLLECTIONS, ENVIRONMENTS, SETTINGS)
}

private enum class BottomDestination(
    val route: String,
    val icon: ImageVector,
    val label: Int
) {
    REQUEST(Routes.REQUEST, Icons.AutoMirrored.Filled.Send, R.string.tab_request),
    HISTORY(Routes.HISTORY, Icons.Filled.History, R.string.tab_history),
    COLLECTIONS(Routes.COLLECTIONS, Icons.Filled.Folder, R.string.tab_collections),
    ENVIRONMENTS(Routes.ENVIRONMENTS, Icons.Filled.Dns, R.string.tab_environments),
    SETTINGS(Routes.SETTINGS, Icons.Filled.Settings, R.string.tab_settings)
}

@Composable
fun ApiPocketNavHost(container: AppContainer) {
    val requestVm: RequestViewModel = viewModel(
        factory = viewModelFactory { initializer { RequestViewModel(container) } }
    )
    val historyVm: HistoryViewModel = viewModel(
        factory = viewModelFactory { initializer { HistoryViewModel(container.repository) } }
    )
    val collectionsVm: CollectionsViewModel = viewModel(
        factory = viewModelFactory { initializer { CollectionsViewModel(container.repository) } }
    )
    val environmentsVm: EnvironmentsViewModel = viewModel(
        factory = viewModelFactory { initializer { EnvironmentsViewModel(container.repository) } }
    )
    val settingsVm: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container) } }
    )

    val collections by collectionsVm.collections.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomRoutes

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route -> navigateToTab(navController, route) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.REQUEST,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.REQUEST) {
                RequestScreen(
                    vm = requestVm,
                    collections = collections,
                    onOpenEnvironments = { navigateToTab(navController, Routes.ENVIRONMENTS) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    vm = historyVm,
                    onOpenRequest = { entry, sendNow ->
                        requestVm.loadFromHistory(entry)
                        if (sendNow) requestVm.send()
                        navigateToTab(navController, Routes.REQUEST)
                    }
                )
            }
            composable(Routes.COLLECTIONS) {
                CollectionsScreen(
                    vm = collectionsVm,
                    onOpenCollection = { id ->
                        navController.navigate(Routes.collectionDetail(id))
                    }
                )
            }
            composable(
                route = Routes.COLLECTION_DETAIL,
                arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
            ) { entry ->
                val collectionId = entry.arguments?.getString("collectionId") ?: return@composable
                CollectionDetailScreen(
                    vm = collectionsVm,
                    collectionId = collectionId,
                    onBack = { navController.popBackStack() },
                    onOpenRequest = { id, saved ->
                        requestVm.loadFromSaved(id, saved)
                        navigateToTab(navController, Routes.REQUEST)
                    }
                )
            }
            composable(Routes.ENVIRONMENTS) {
                EnvironmentsScreen(
                    vm = environmentsVm,
                    onOpenEnvironment = { id ->
                        navController.navigate(Routes.environmentDetail(id))
                    }
                )
            }
            composable(
                route = Routes.ENVIRONMENT_DETAIL,
                arguments = listOf(navArgument("environmentId") { type = NavType.StringType })
            ) { entry ->
                val environmentId = entry.arguments?.getString("environmentId") ?: return@composable
                EnvironmentDetailScreen(
                    vm = environmentsVm,
                    environmentId = environmentId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm = settingsVm)
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        BottomDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.label),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
