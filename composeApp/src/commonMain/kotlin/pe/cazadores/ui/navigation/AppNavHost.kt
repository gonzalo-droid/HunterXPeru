package pe.cazadores.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.koin.compose.koinInject
import pe.cazadores.ads.AdsController
import pe.cazadores.ui.screens.about.AboutRoute
import pe.cazadores.ui.screens.detail.DetailRoute
import pe.cazadores.ui.screens.home.HomeRoute
import pe.cazadores.ui.screens.onboarding.OnboardingScreen
import pe.cazadores.ui.screens.saved.SavedRoute
import pe.cazadores.ui.screens.settings.SettingsRoute
import pe.cazadores.ui.screens.regime.SpecialRegimeRoute
import pe.cazadores.ui.screens.search.SearchRoute
import pe.cazadores.ui.theme.Cz

@Composable
fun AppNavHost(
    startDestination: Route,
    onOnboardingFinished: () -> Unit,
    navController: NavHostController = rememberNavController(),
    ads: AdsController = koinInject(),
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentTab = backStack?.destination.topLevel()

    val pendingHash by DeepLinks.pendingHash.collectAsState()
    LaunchedEffect(pendingHash) {
        pendingHash?.let { hash ->
            DeepLinks.consume()
            navController.navigate(Route.Detail(hash)) { launchSingleTop = true }
        }
    }

    Scaffold(
        containerColor = Cz.colors.bg,
        // Cada pantalla maneja sus márgenes del sistema; aquí solo se reserva la barra de pestañas.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
            composable<Route.Onboarding> {
                OnboardingScreen(onFinish = {
                    onOnboardingFinished()
                    navController.navigate(Route.Home) { popUpTo<Route.Onboarding> { inclusive = true } }
                })
            }
            composable<Route.Home> {
                // Primer momento válido para pedir consentimiento: ya pasó el onboarding y hay una vista en pantalla.
                LaunchedEffect(Unit) { ads.start() }
                HomeRoute(
                    onOpenSearch = {
                        navController.navigate(Route.Search(focus = true)) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onSeeAll = {
                        navController.navigate(Route.Search(sortByReward = true)) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenDetail = { navController.navigate(Route.Detail(it)) },
                    onOpenRegime = { navController.navigate(Route.SpecialRegime) },
                )
            }
            composable<Route.Search> { entry ->
                SearchRoute(
                    focusSearch = entry.toRoute<Route.Search>().focus,
                    sortByReward = entry.toRoute<Route.Search>().sortByReward,
                    onOpenDetail = { navController.navigate(Route.Detail(it)) },
                )
            }
            composable<Route.Saved> {
                SavedRoute(
                    onOpenDetail = { navController.navigate(Route.Detail(it)) },
                    onGoSearch = { navController.navigateToTab(TopLevel.SEARCH) },
                )
            }
            composable<Route.Settings> {
                SettingsRoute(
                    onOpenRegime = { navController.navigate(Route.SpecialRegime) },
                    onOpenAbout = { navController.navigate(Route.About) },
                )
            }
            composable<Route.Detail> { entry ->
                DetailRoute(
                    hash = entry.toRoute<Route.Detail>().hash,
                    onBack = { navController.navigateUp() },
                    onOpenRegime = { navController.navigate(Route.SpecialRegime) },
                )
            }
            composable<Route.SpecialRegime> { SpecialRegimeRoute(onBack = { navController.navigateUp() }) }
            composable<Route.About> { AboutRoute(onBack = { navController.navigateUp() }) }
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
