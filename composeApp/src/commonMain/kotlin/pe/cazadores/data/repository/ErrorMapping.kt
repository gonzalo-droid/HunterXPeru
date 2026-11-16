package pe.cazadores.data.repository

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import pe.cazadores.core.result.AppResult
import pe.cazadores.core.result.DataError

internal fun Throwable.toDataError(): DataError = when (this) {
    is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> DataError.Timeout
    is ResponseException -> when (response.status) {
        HttpStatusCode.NotFound -> DataError.NotFound
        else -> DataError.Server(response.status.value)
    }
    is IOException -> DataError.Network
    is SerializationException -> DataError.Server(-1)
    else -> DataError.Unknown(message)
}

/** Sin red o sin respuesta: casos en los que vale la pena mirar la caché. */
internal val DataError.allowsCacheFallback: Boolean
    get() = this is DataError.Network || this is DataError.Timeout || this is DataError.Server

internal suspend inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    AppResult.Failure(e.toDataError())
}
