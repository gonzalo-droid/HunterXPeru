package pe.cazadores.ui.navigation

import kotlinx.serialization.Serializable
import pe.cazadores.ui.screens.legal.LegalPage

/** Rutas type-safe. Las cuatro pestañas viven dentro de la barra inferior; la ficha va a pantalla completa. */
sealed interface Route {
    @Serializable data object Onboarding : Route
    @Serializable data object Home : Route
    @Serializable data class Search(val focus: Boolean = false, val sortByReward: Boolean = false) : Route
    @Serializable data object Saved : Route
    @Serializable data object Settings : Route

    /** Régimen especial de recompensas por extorsión y sicariato (D. S. N.º 097-2025-PCM). */
    @Serializable data object SpecialRegime : Route

    /** Propósito del proyecto y cómo apoyarlo (desde Ajustes). */
    @Serializable data object About : Route

    /** Licencias de código abierto y SDK de terceros (desde Ajustes). */
    @Serializable data object Licenses : Route

    /**
     * Política de privacidad o términos en una vista web dentro de la app (desde Ajustes).
     * El argumento viaja como el nombre del enum: en iOS, Navigation no trae `NavType` para enums
     * y la app se cerraba al armar el grafo. Un nombre desconocido cae en la política de privacidad.
     */
    @Serializable data class Legal(val pageName: String) : Route {
        constructor(page: LegalPage) : this(page.name)

        val page: LegalPage get() = LegalPage.entries.firstOrNull { it.name == pageName } ?: LegalPage.PRIVACY
    }

    /** Ficha. También es el destino del deep link `cazadores://criminal/{hash}`. */
    @Serializable data class Detail(val hash: String) : Route
}

const val DEEP_LINK_SCHEME = "cazadores"
