package pe.cazadores.ui.screens.detail

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.cazadores.platform.ExternalActions

/**
 * Comparte el cartel como PNG con [text]. Sin imagen, o si la codificación falla, comparte solo el texto
 * (D17). Genérico en el bitmap para probarlo sin `ImageBitmap`, que en los tests JVM no existe.
 */
internal suspend fun <B : Any> sharePoster(
    bitmap: B?,
    text: String,
    actions: ExternalActions,
    encode: (B) -> ByteArray,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    val png = bitmap?.let { withContext(dispatcher) { runCatching { encode(it) }.getOrNull() } }
    if (png != null) actions.shareImage(png, text) else actions.share(text)
}
