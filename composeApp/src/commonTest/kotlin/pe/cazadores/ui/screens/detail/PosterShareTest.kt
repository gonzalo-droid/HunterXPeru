package pe.cazadores.ui.screens.detail

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import pe.cazadores.platform.ExternalActions
import kotlin.test.Test
import kotlin.test.assertEquals

class PosterShareTest {
    private val shared = mutableListOf<String>()
    private val actions = object : ExternalActions {
        override fun dial(telUri: String) = Unit
        override fun email(to: String, subject: String, body: String) = Unit
        override fun share(text: String) { shared += "text:$text" }
        override fun shareImage(png: ByteArray, text: String) { shared += "image(${png.size}):$text" }
        override fun openUrl(url: String) = Unit
        override fun openInAppBrowser(url: String) = Unit
        override fun openStoreReview() = Unit
    }

    @Test fun encodedPosterIsSharedAsImage() = runTest {
        sharePoster("bitmap", "hola", actions, encode = { byteArrayOf(1, 2, 3) }, StandardTestDispatcher(testScheduler))
        assertEquals(listOf("image(3):hola"), shared)
    }

    @Test fun missingBitmapFallsBackToText() = runTest {
        sharePoster<String>(null, "hola", actions, encode = { error("no debe codificar") }, StandardTestDispatcher(testScheduler))
        assertEquals(listOf("text:hola"), shared)
    }

    @Test fun encodingFailureFallsBackToText() = runTest {
        sharePoster("bitmap", "hola", actions, encode = { error("sin memoria") }, StandardTestDispatcher(testScheduler))
        assertEquals(listOf("text:hola"), shared)
    }
}
