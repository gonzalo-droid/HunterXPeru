package pe.cazadores.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import pe.cazadores.ui.theme.Cz

/**
 * Pila de preferencias: la última pantalla compuesta manda. Al salir una pantalla se aplica la que
 * queda arriba, sin importar el orden en que la navegación compone y desecha pantallas.
 * Si una entrada cambia de valor (p. ej. la base de `App` al cambiar de tema) se actualiza en su sitio:
 * volver a apilarla la pondría encima de la pantalla visible.
 */
internal object StatusBarStack {
    private val entries = mutableListOf<Pair<Any, Boolean>>()

    fun push(token: Any, lightIcons: Boolean): Boolean {
        entries += token to lightIcons
        return lightIcons
    }

    /** Cambia el valor sin mover la entrada; devuelve el que queda arriba (null si el token no está). */
    fun update(token: Any, lightIcons: Boolean): Boolean? {
        val index = entries.indexOfFirst { it.first === token }
        if (index < 0) return null
        entries[index] = token to lightIcons
        return entries.last().second
    }

    /** Estilo vigente (el de la pantalla visible), o null si no hay ninguno. */
    fun top(): Boolean? = entries.lastOrNull()?.second

    fun remove(token: Any): Boolean? {
        entries.removeAll { it.first === token }
        return entries.lastOrNull()?.second
    }
}

/** Aplica el estilo de íconos en la plataforma (Android: WindowInsetsController; iOS: UIApplication). */
@Composable
internal expect fun rememberStatusBarApplier(): (lightIcons: Boolean) -> Unit

/** Íconos de la barra de estado claros (sobre fondo oscuro) u oscuros mientras esta pantalla esté visible. */
@Composable
fun StatusBarAppearance(lightIcons: Boolean) {
    val apply = rememberStatusBarApplier()
    val token = remember { Any() }
    DisposableEffect(token) {
        apply(StatusBarStack.push(token, lightIcons))
        onDispose { StatusBarStack.remove(token)?.let(apply) }
    }
    DisposableEffect(lightIcons) {
        StatusBarStack.update(token, lightIcons)?.let(apply)
        onDispose { }
    }
}

/**
 * true si el `ModalBottomSheet` se dibuja en la misma ventana que la pantalla (iOS). En Android la hoja vive
 * en su propia ventana de diálogo, que ya ajusta los íconos, así que ahí no se toca la pila.
 */
internal expect val sheetSharesAppWindow: Boolean

/**
 * Para cualquier `ModalBottomSheet`: la hoja expandida (contenido alto o letra grande) se dibuja bajo la
 * barra de estado y, si su superficie no combina con los íconos de la pantalla de abajo, estos desaparecen
 * (p. ej. íconos blancos de Inicio, o del tema oscuro, sobre una hoja clara). Mientras la hoja cubre la
 * barra de estado se apila el estilo de su superficie: [lightIcons] = true si la superficie es oscura.
 * Solo actúa en iOS (ver [sheetSharesAppWindow]). Llamar fuera del contenido de la hoja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetStatusBarAppearance(sheetState: SheetState, lightIcons: Boolean = Cz.colors.isDark) {
    if (!sheetSharesAppWindow) return
    if (rememberSheetCoversStatusBar(sheetState)) StatusBarAppearance(lightIcons = lightIcons)
}

/** true mientras la hoja expandida llega a la barra de estado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberSheetCoversStatusBar(sheetState: SheetState): Boolean {
    val statusBarTop = WindowInsets.statusBars.getTop(LocalDensity.current)
    val covers by remember(sheetState, statusBarTop) {
        derivedStateOf {
            val offset = runCatching { sheetState.requireOffset() }.getOrNull()
            statusBarTop > 0 && offset != null && offset < statusBarTop
        }
    }
    return covers
}

/**
 * Propiedades para un `ModalBottomSheet` cuya superficie no sigue el tema (p. ej. el papel de la leyenda).
 * En Android la hoja vive en su propia ventana y Material elige los íconos por el color del contenido de la
 * hoja aunque esta no llegue a la barra: sobre el velo del tema oscuro quedaban íconos negros invisibles.
 * Aquí la ventana de la hoja usa los íconos de la pantalla de abajo y solo pasa a los de su superficie
 * ([sheetLightIcons] = true si es oscura) mientras la cubre. En iOS no cambia nada (ver [SheetStatusBarAppearance]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal expect fun sheetWindowProperties(sheetState: SheetState, sheetLightIcons: Boolean): ModalBottomSheetProperties
