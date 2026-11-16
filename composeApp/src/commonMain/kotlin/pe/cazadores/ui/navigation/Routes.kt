package pe.cazadores.ui.navigation

import kotlinx.serialization.Serializable

/** Rutas type-safe. Las cuatro pestañas viven dentro de la barra inferior; la ficha va a pantalla completa. */
sealed interface Route {
    @Serializable data object Onboarding : Route
    @Serializable data object Home : Route
    @Serializable data class Search(val focus: Boolean = false) : Route
    @Serializable data object Saved : Route
    @Serializable data object Settings : Route

    /** Ficha. También es el destino del deep link `cazadores://criminal/{hash}`. */
    @Serializable data class Detail(val hash: String) : Route
}

const val DEEP_LINK_SCHEME = "cazadores"
