package pe.cazadores.ui.components

import androidx.compose.runtime.Composable

/**
 * Íconos de la barra de estado claros (sobre fondo oscuro) u oscuros. Al salir de la composición
 * vuelve al valor [restoreLightIcons] (el que corresponde al tema).
 */
@Composable
expect fun StatusBarAppearance(lightIcons: Boolean, restoreLightIcons: Boolean)
