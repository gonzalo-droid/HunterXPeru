package pe.cazadores.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Pila de preferencias: la última pantalla compuesta manda. Al salir una pantalla se aplica la que
 * queda arriba, sin importar el orden en que la navegación compone y desecha pantallas.
 */
internal object StatusBarStack {
    private val entries = mutableListOf<Pair<Any, Boolean>>()

    fun push(token: Any, lightIcons: Boolean): Boolean {
        entries += token to lightIcons
        return lightIcons
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
    DisposableEffect(lightIcons) {
        val token = Any()
        apply(StatusBarStack.push(token, lightIcons))
        onDispose { StatusBarStack.remove(token)?.let(apply) }
    }
}
