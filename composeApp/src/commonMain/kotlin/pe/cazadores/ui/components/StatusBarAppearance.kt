package pe.cazadores.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

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
 * Propiedades para un `ModalBottomSheet` cuya superficie no sigue el tema (p. ej. el papel de la leyenda).
 * En Android la hoja vive en su propia ventana y Material elige los íconos por el color del contenido de la
 * hoja aunque esta no llegue a la barra: sobre el velo del tema oscuro quedaban íconos negros invisibles.
 * Aquí la ventana de la hoja usa los íconos de la pantalla de abajo: la hoja nunca llega a la barra de estado
 * (ver [sheetModifier]). [sheetLightIcons] = true si la superficie es oscura (íconos de la barra de navegación).
 * En iOS la hoja comparte la ventana de la app y no cambia nada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal expect fun sheetWindowProperties(sheetLightIcons: Boolean): ModalBottomSheetProperties

/**
 * Insets para el contenido de cualquier `ModalBottomSheet`, junto con [sheetModifier]. Con los de Material
 * (incluyen la barra de estado) la hoja suma arriba el trozo de barra que tapa: eso la alarga, la sube y suma
 * más relleno; si el contenido mide casi lo que la pantalla, no hay punto fijo y la hoja tiembla sin parar (iOS).
 * Aquí la hoja nunca pasa bajo la barra de estado y solo se respeta el borde inferior.
 */
@OptIn(ExperimentalMaterial3Api::class)
val sheetContentInsets: WindowInsets
    @Composable get() = BottomSheetDefaults.windowInsets.only(WindowInsetsSides.Bottom)

/** Mantiene la hoja debajo de la barra de estado (ver [sheetContentInsets]). */
val sheetModifier: Modifier
    @Composable get() = Modifier.statusBarsPadding()
