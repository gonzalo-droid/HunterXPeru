package pe.cazadores.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.resources.Res
import pe.cazadores.resources.tab_home
import pe.cazadores.resources.tab_saved
import pe.cazadores.resources.tab_search
import pe.cazadores.resources.tab_settings
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons
import kotlin.reflect.KClass

enum class TopLevel(val route: Route, val routeClass: KClass<out Route>, val label: StringResource, val icon: ImageVector) {
    HOME(Route.Home, Route.Home::class, Res.string.tab_home, CzIcons.Home),
    SEARCH(Route.Search(), Route.Search::class, Res.string.tab_search, CzIcons.Search),
    SAVED(Route.Saved, Route.Saved::class, Res.string.tab_saved, CzIcons.Bookmark),
    SETTINGS(Route.Settings, Route.Settings::class, Res.string.tab_settings, CzIcons.Settings),
}

fun NavDestination?.topLevel(): TopLevel? =
    TopLevel.entries.firstOrNull { tab -> this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true }

@Composable
fun CazadoresBottomBar(current: TopLevel?, onSelect: (TopLevel) -> Unit) {
    // Las etiquetas de pestaña escalan hasta 1,5×: a 2× "Guardados" no cabe en un cuarto de pantalla.
    // El ícono y la descripción accesible identifican cada pestaña a cualquier tamaño.
    val fontScale = LocalDensity.current.fontScale
    val labelScale = if (fontScale > MAX_LABEL_SCALE) MAX_LABEL_SCALE / fontScale else 1f
    NavigationBar(containerColor = Cz.colors.surface, contentColor = Cz.colors.ink) {
        TopLevel.entries.forEach { tab ->
            val selected = tab == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = {
                    Text(
                        stringResource(tab.label),
                        style = Cz.type.caption.copy(fontSize = Cz.type.caption.fontSize * labelScale),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Cz.colors.bg,
                    selectedTextColor = Cz.colors.ink,
                    indicatorColor = Cz.colors.ink,
                    unselectedIconColor = Cz.colors.inkMuted,
                    unselectedTextColor = Cz.colors.inkMuted,
                ),
            )
        }
    }
}

private const val MAX_LABEL_SCALE = 1.5f
