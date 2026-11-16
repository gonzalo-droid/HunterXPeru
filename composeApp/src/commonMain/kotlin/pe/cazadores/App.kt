package pe.cazadores

import androidx.compose.runtime.Composable
import pe.cazadores.ui.navigation.AppNavHost
import pe.cazadores.ui.navigation.Route
import pe.cazadores.ui.theme.CazadoresTheme

/** Raíz de la UI compartida (Android e iOS). */
@Composable
fun App() {
    CazadoresTheme {
        AppNavHost(startDestination = Route.Home)
    }
}
