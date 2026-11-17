package pe.cazadores

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.cazadores.domain.repository.PhotoRepository
import pe.cazadores.ui.components.LocalPhotoRepository
import pe.cazadores.ui.app.AppUiState
import pe.cazadores.ui.app.AppViewModel
import pe.cazadores.ui.components.StatusBarAppearance
import pe.cazadores.ui.navigation.AppNavHost
import pe.cazadores.ui.navigation.Route
import pe.cazadores.ui.theme.CazadoresTheme
import pe.cazadores.ui.theme.Cz

/** Raíz de la UI compartida (Android e iOS). */
@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = state as? AppUiState.Ready
    CazadoresTheme(themeMode = ready?.themeMode ?: pe.cazadores.domain.model.ThemeMode.SYSTEM) {
        StatusBarAppearance(lightIcons = Cz.colors.isDark)
        if (ready == null) {
            Box(Modifier.fillMaxSize().background(Cz.colors.bg))
        } else {
            // El destino inicial se decide una sola vez: al terminar el onboarding se navega, no se recompone el grafo.
            val start = remember { if (ready.showOnboarding) Route.Onboarding else Route.Home }
            CompositionLocalProvider(LocalPhotoRepository provides koinInject<PhotoRepository>()) {
                AppNavHost(startDestination = start, onOnboardingFinished = viewModel::onOnboardingFinished)
            }
        }
    }
}
