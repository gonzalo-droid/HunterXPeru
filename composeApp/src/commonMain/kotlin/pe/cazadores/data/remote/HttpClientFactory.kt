package pe.cazadores.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import pe.cazadores.core.config.AppConfig

val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
}

/**
 * Cliente único. Cabeceras honestas: la API acepta peticiones sin `Origin`/`Referer` de
 * recompensas.pe (verificado 2026-11-16), así que no se imitan las del navegador.
 */
fun createHttpClient(
    engine: HttpClientEngine,
    userAgent: String,
    debug: Boolean,
): HttpClient = HttpClient(engine) {
    expectSuccess = true

    install(ContentNegotiation) { json(ApiJson) }

    install(HttpTimeout) {
        connectTimeoutMillis = 30_000
        requestTimeoutMillis = 60_000
        socketTimeoutMillis = 60_000
    }

    install(HttpRequestRetry) {
        maxRetries = 3
        retryOnExceptionIf { _, cause -> cause.isTransient() }
        retryIf { _, _ -> false }
        exponentialDelay(base = 2.0, maxDelayMs = 8_000)
    }

    if (debug) {
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) = println("Ktor: $message")
            }
        }
    }

    defaultRequest {
        url(AppConfig.API_BASE_URL + "/")
        contentType(ContentType.Application.Json)
        headers.append(HttpHeaders.Accept, "application/json")
        headers.append(HttpHeaders.AcceptLanguage, "es-PE")
        headers.append(HttpHeaders.UserAgent, userAgent)
    }
}

/**
 * Cliente para Firebase Storage (padrón, D21), aparte del de la API: Storage responde 403
 * `SignatureDoesNotMatch` a un GET con `Content-Type`, que el cliente de la API manda siempre.
 */
fun createStorageClient(engine: HttpClientEngine, userAgent: String): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(HttpTimeout) {
        connectTimeoutMillis = 30_000
        requestTimeoutMillis = 60_000
        socketTimeoutMillis = 60_000
    }
    install(HttpRequestRetry) {
        maxRetries = 2
        retryOnExceptionIf { _, cause -> cause.isTransient() }
        retryIf { _, _ -> false }
        exponentialDelay(base = 2.0, maxDelayMs = 8_000)
    }
    defaultRequest {
        headers.append(HttpHeaders.UserAgent, userAgent)
    }
}

internal fun Throwable.isTransient(): Boolean = when (this) {
    is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> true
    is IOException -> true
    else -> false
}
