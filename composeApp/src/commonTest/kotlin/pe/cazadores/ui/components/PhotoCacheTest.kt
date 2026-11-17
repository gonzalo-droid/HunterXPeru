package pe.cazadores.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Garantiza que la imagen compartida espera a la foto: el paso "decodificar y cachear" es lo que evita la silueta. */
@OptIn(ExperimentalEncodingApi::class)
class PhotoCacheTest {

    private class FakeBitmap : ImageBitmap {
        override val width = 1
        override val height = 1
        override val colorSpace: ColorSpace = ColorSpaces.Srgb
        override val hasAlpha = false
        override val config = ImageBitmapConfig.Argb8888
        override fun readPixels(buffer: IntArray, startX: Int, startY: Int, width: Int, height: Int, bufferOffset: Int, stride: Int) = Unit
        override fun prepareToDraw() = Unit
    }

    private val photo = Base64.Default.encode(byteArrayOf(1, 2, 3))

    @Test fun decodesAndCachesWhenMissing() = runTest {
        val bitmap = FakeBitmap()
        val result = ensurePhotoDecoded("t-missing", photo, decode = { bitmap })
        assertSame(bitmap, result)
        assertSame(bitmap, PhotoCache.get("t-missing"))
    }

    @Test fun usesCachedPhotoWithoutDecodingAgain() = runTest {
        val cached = FakeBitmap()
        PhotoCache.put("t-cached", cached)
        val result = ensurePhotoDecoded("t-cached", photo, decode = { error("no debe decodificar") })
        assertSame(cached, result)
    }

    @Test fun returnsNullWhenThereIsNoValidPhoto() = runTest {
        assertNull(ensurePhotoDecoded("t-none", null, decode = { FakeBitmap() }))
        assertNull(ensurePhotoDecoded("t-invalid", photo, decode = { null }))
        assertNull(PhotoCache.get("t-invalid"))
        assertNotNull(ensurePhotoDecoded("t-ok", photo, decode = { FakeBitmap() }))
    }
}
