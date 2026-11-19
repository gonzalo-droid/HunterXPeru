package pe.cazadores.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp
import pe.cazadores.domain.model.ThemeMode

/** Radios y medidas del sistema (§2 "Forma"). */
object CazadoresShapes {
    val card = RoundedCornerShape(14.dp)
    val cardLarge = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(14.dp)
    val field = RoundedCornerShape(14.dp)
    val chip = RoundedCornerShape(18.dp)
    val badge = RoundedCornerShape(6.dp)
    val photo = RoundedCornerShape(10.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

object CazadoresDimens {
    val buttonHeight = 52.dp
    val buttonHeightLarge = 56.dp
    val touchTarget = 48.dp
    val screenPadding = 20.dp

    /** Ancho máximo del contenido en pantallas anchas (iPad, ventanas grandes). */
    val contentMaxWidth = 720.dp
}

private fun CazadoresColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = ink,
        onPrimary = bg,
        secondary = reward,
        onSecondary = onReward,
        error = action,
        onError = onAction,
        background = bg,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceAlt,
        onSurfaceVariant = inkMuted,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        outline = lineStrong,
        outlineVariant = line,
        // Snackbar: contraste invertido respecto del fondo, con la acción en un color que se lea (≥ 4.5:1).
        inverseSurface = ink,
        inverseOnSurface = bg,
        inversePrimary = if (isDark) levelExtremo else reward,
    )
}

@Composable
fun CazadoresTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkCazadoresColors else LightCazadoresColors
    val typography = cazadoresTypography()
    CompositionLocalProvider(
        LocalCazadoresColors provides colors,
        LocalCazadoresTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = typography.toMaterial(),
            shapes = Shapes(
                small = CazadoresShapes.badge,
                medium = CazadoresShapes.card,
                large = CazadoresShapes.cardLarge,
                extraLarge = CazadoresShapes.sheet,
            ),
            content = content,
        )
    }
}

/** Acceso corto: `Cz.colors.ink`, `Cz.type.body`. */
object Cz {
    val colors: CazadoresColors
        @Composable @ReadOnlyComposable get() = LocalCazadoresColors.current
    val type: CazadoresTypography
        @Composable @ReadOnlyComposable get() = LocalCazadoresTypography.current
}
