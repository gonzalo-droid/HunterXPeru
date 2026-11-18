package pe.cazadores.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
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
 * Para pantallas con íconos claros (p. ej. Inicio) que abren un `ModalBottomSheet`: la hoja expandida
 * se dibuja bajo la barra de estado y, con su superficie clara, los íconos blancos desaparecen de golpe.
 * Android lo resuelve en la ventana del diálogo de la hoja; iOS no, así que mientras la hoja cubre la
 * barra de estado se apila el estilo que corresponde a la superficie. Llamar fuera del contenido de la hoja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetStatusBarAppearance(sheetState: SheetState) {
    val statusBarTop = WindowInsets.statusBars.getTop(LocalDensity.current)
    val coversStatusBar by remember(sheetState, statusBarTop) {
        derivedStateOf {
            val offset = runCatching { sheetState.requireOffset() }.getOrNull()
            statusBarTop > 0 && offset != null && offset < statusBarTop
        }
    }
    if (coversStatusBar) StatusBarAppearance(lightIcons = Cz.colors.isDark)
}
