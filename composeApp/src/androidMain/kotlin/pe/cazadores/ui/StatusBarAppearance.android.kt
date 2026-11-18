package pe.cazadores.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import pe.cazadores.ui.theme.Cz

@Composable
internal actual fun rememberStatusBarApplier(): (lightIcons: Boolean) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { lightIcons ->
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !lightIcons
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal actual val sheetSharesAppWindow: Boolean = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun sheetWindowProperties(sheetState: SheetState, sheetLightIcons: Boolean): ModalBottomSheetProperties {
    // Íconos de la pantalla visible al abrir la hoja (la pila no cambia mientras está abierta).
    val screenLightIcons = remember { StatusBarStack.top() } ?: Cz.colors.isDark
    val lightIcons = if (rememberSheetCoversStatusBar(sheetState)) sheetLightIcons else screenLightIcons
    return ModalBottomSheetProperties(
        isAppearanceLightStatusBars = !lightIcons,
        // La barra de navegación queda siempre sobre la superficie de la hoja.
        isAppearanceLightNavigationBars = !sheetLightIcons,
    )
}
