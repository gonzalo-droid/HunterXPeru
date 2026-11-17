package pe.cazadores.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

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
