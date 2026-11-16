package pe.cazadores.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import pe.cazadores.core.PhotoDecoder
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz

/**
 * Caché LRU de fotos decodificadas (solo hilo principal): la API manda Base64 de hasta ~200 KB por persona y
 * decodificarlo en cada recomposición hace que el scroll se trabe.
 */
internal object PhotoCache {
    private const val MAX = 80
    private val map = LinkedHashMap<String, ImageBitmap>()

    fun get(key: String): ImageBitmap? = map.remove(key)?.also { map[key] = it }

    fun put(key: String, bitmap: ImageBitmap) {
        map.remove(key)
        map[key] = bitmap
        while (map.size > MAX) map.remove(map.keys.first())
    }
}

/**
 * Foto de la persona o silueta si no hay foto o no se puede leer.
 * [cacheKey] suele ser el hash de la persona.
 */
@Composable
fun CriminalPhoto(
    cacheKey: String,
    base64: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CazadoresShapes.photo,
) {
    val bitmap by produceState(PhotoCache.get(cacheKey), cacheKey, base64) {
        if (value == null && base64 != null) {
            val decoded = withContext(Dispatchers.Default) {
                PhotoDecoder.decode(base64)?.let { bytes -> runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
            }
            // La caché solo se toca desde el hilo principal (produceState corre ahí).
            decoded?.let { PhotoCache.put(cacheKey, it) }
            value = decoded
        }
    }
    val described = if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier
    Box(
        modifier.clip(shape).background(Cz.colors.photoPlaceholder).then(described),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val image = bitmap
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
        } else {
            Silhouette(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun Silhouette(modifier: Modifier) {
    val color = Cz.colors.photoSilhouette
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val headR = w * 0.22f
        drawCircle(color, radius = headR, center = Offset(w / 2, h * 0.38f))
        val body = Path().apply {
            moveTo(w * 0.06f, h)
            cubicTo(w * 0.1f, h * 0.72f, w * 0.3f, h * 0.62f, w / 2, h * 0.62f)
            cubicTo(w * 0.7f, h * 0.62f, w * 0.9f, h * 0.72f, w * 0.94f, h)
            close()
        }
        drawPath(body, color)
    }
}
