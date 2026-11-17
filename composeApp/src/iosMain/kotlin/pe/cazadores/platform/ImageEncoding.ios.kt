package pe.cazadores.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun ImageBitmap.encodePng(): ByteArray {
    val image = Image.makeFromBitmap(asSkiaBitmap())
    try {
        return checkNotNull(image.encodeToData(EncodedImageFormat.PNG)) { "PNG encoding failed" }.bytes
    } finally {
        image.close()
    }
}
