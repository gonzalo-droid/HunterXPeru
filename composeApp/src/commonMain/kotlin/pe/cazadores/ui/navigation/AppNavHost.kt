package pe.cazadores.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import pe.cazadores.ui.screens.detail.DetailRoute
import pe.cazadores.ui.screens.home.HomeRoute
import pe.cazadores.ui.screens.placeholder.PlaceholderScreen
import pe.cazadores.ui.screens.search.SearchRoute
import pe.cazadores.ui.theme.Cz

@Composable
fun AppNavHost(
    startDestination: Route,
    navController: NavHostController = rememberNavController(),
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentTab = backStack?.destination.topLevel()

    Scaffold(
        containerColor = Cz.colors.bg,
        bottomBar = {
            if (currentTab != null) {
                CazadoresBottomBar(current = currentTab) { tab -> navController.navigateToTab(tab) }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable<Route.Onboarding> { PlaceholderScreen("Onboarding") }
            composable<Route.Home> {
                HomeRoute(
                    onOpenSearch = {
                        navController.navigate(Route.Search(focus = true)) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenDetail = { navController.navigate(Route.Detail(it)) },
                )
            }
            composable<Route.Search> { entry ->
                SearchRoute(
                    focusSearch = entry.toRoute<Route.Search>().focus,
                    onOpenDetail = { navController.navigate(Route.Detail(it)) },
                )
            }
            composable<Route.Saved> { PlaceholderScreen("Guardados") }
            composable<Route.Settings> { PlaceholderScreen("Ajustes") }
            composable<Route.Detail>(
                deepLinks = listOf(navDeepLink<Route.Detail>(basePath = "$DEEP_LINK_SCHEME://criminal")),
            ) { entry ->
                DetailRoute(hash = entry.toRoute<Route.Detail>().hash, onBack = { navController.navigateUp() })
            }
        }
    }
}

/** Cambio de pestaña estándar: un solo ejemplar de cada pestaña y estado guardado. */
fun NavHostController.navigateToTab(tab: TopLevel) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
