package pe.cazadores.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
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
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.repository.PhotoRepository
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
 * Devuelve la foto decodificada de [key]: de la caché o, si falta, decodificando [base64] fuera del hilo principal
 * y guardándola. `null` solo si no hay foto o el Base64 no es una imagen válida. Llamar desde el hilo principal
 * (la caché no es segura entre hilos). [decode] se inyecta para poder probarlo sin decodificador de imágenes.
 */
internal suspend fun ensurePhotoDecoded(
    key: String,
    base64: String?,
    decode: (ByteArray) -> ImageBitmap? = { bytes -> runCatching { bytes.decodeToImageBitmap() }.getOrNull() },
): ImageBitmap? {
    PhotoCache.get(key)?.let { return it }
    if (base64 == null) return null
    val decoded = withContext(Dispatchers.Default) { PhotoDecoder.decode(base64)?.let(decode) }
    decoded?.let { PhotoCache.put(key, it) }
    return decoded
}

/** Imágenes del padrón (miniatura y foto por clave). `null` en previews y tests: solo se usa el Base64. */
val LocalPhotoRepository = staticCompositionLocalOf<PhotoRepository?> { null }

/** Imagen del padrón por [key] ("thumbs/…" o "photos/…"): de la caché o bajada con [photos] y decodificada. */
internal suspend fun ensureKeyDecoded(
    key: String?,
    photos: PhotoRepository?,
    decode: (ByteArray) -> ImageBitmap? = { bytes -> runCatching { bytes.decodeToImageBitmap() }.getOrNull() },
): ImageBitmap? {
    if (key == null) return null
    PhotoCache.get(key)?.let { return it }
    val bytes = photos?.image(key) ?: return null
    val decoded = withContext(Dispatchers.Default) { decode(bytes) }
    decoded?.let { PhotoCache.put(key, it) }
    return decoded
}

/** La mejor foto que ya está en la caché, sin bajar nada. */
internal fun cachedPhoto(cacheKey: String, photoKey: String?, thumbKey: String?): ImageBitmap? =
    photoKey?.let(PhotoCache::get) ?: PhotoCache.get(cacheKey) ?: thumbKey?.let(PhotoCache::get)

/**
 * Deja en la caché la mejor foto de [criminal] y la devuelve: Base64 (API o favorito) o, si viene del padrón,
 * la foto grande con la miniatura como respaldo. Para quien necesita la foto antes de componer (el cartel a compartir).
 */
internal suspend fun ensureBestPhoto(criminal: Criminal, photos: PhotoRepository?): ImageBitmap? =
    ensurePhotoDecoded(criminal.hash, criminal.photoBase64)
        ?: ensureKeyDecoded(criminal.photoKey, photos)
        ?: ensureKeyDecoded(criminal.thumbKey, photos)

/**
 * Foto de la persona o silueta si no hay foto o no se puede leer.
 * [cacheKey] suele ser el hash de la persona. Sin [base64], usa las imágenes del padrón: primero la miniatura
 * ([thumbKey], ya guardada en el teléfono) y, si se pide [photoKey], la cambia por la foto grande cuando llega.
 */
@Composable
fun CriminalPhoto(
    cacheKey: String,
    base64: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CazadoresShapes.photo,
    thumbKey: String? = null,
    photoKey: String? = null,
) {
    val photos = LocalPhotoRepository.current
    val bitmap by produceState(cachedPhoto(cacheKey, photoKey, thumbKey), cacheKey, base64, thumbKey, photoKey) {
        // La caché solo se toca desde el hilo principal (produceState corre ahí).
        if (base64 != null) {
            if (value == null) value = ensurePhotoDecoded(cacheKey, base64)
            return@produceState
        }
        if (photoKey != null && PhotoCache.get(photoKey) != null) return@produceState
        if (value == null) value = ensureKeyDecoded(thumbKey, photos)
        ensureKeyDecoded(photoKey, photos)?.let { value = it }
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

/** Foto completa de [criminal] (cartel, ficha): miniatura del padrón al instante y luego la foto grande. */
@Composable
fun CriminalPhoto(
    criminal: Criminal,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CazadoresShapes.photo,
    full: Boolean = true,
) = CriminalPhoto(
    cacheKey = criminal.hash,
    base64 = criminal.photoBase64,
    contentDescription = contentDescription,
    modifier = modifier,
    shape = shape,
    thumbKey = criminal.thumbKey,
    photoKey = criminal.photoKey.takeIf { full },
)

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
