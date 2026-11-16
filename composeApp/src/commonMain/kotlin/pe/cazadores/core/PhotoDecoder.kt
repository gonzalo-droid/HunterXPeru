package pe.cazadores.core

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Las fotos llegan en Base64, a veces con prefijo `data:image/jpeg;base64,`. */
object PhotoDecoder {
    @OptIn(ExperimentalEncodingApi::class)
    private val decoder = Base64.Default.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    @OptIn(ExperimentalEncodingApi::class)
    fun decode(raw: String?): ByteArray? {
        if (raw.isNullOrBlank()) return null
        val payload = raw.substringAfter("base64,", raw).filterNot { it.isWhitespace() }
        return runCatching { decoder.decode(payload) }.getOrNull()?.takeIf { it.isNotEmpty() }
    }
}
