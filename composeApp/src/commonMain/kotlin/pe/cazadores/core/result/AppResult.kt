package pe.cazadores.core.result

/** Errores de datos que la UI sabe explicar. */
sealed interface DataError {
    /** Sin conexión, DNS o TLS: el usuario puede reintentar cuando tenga red. */
    data object Network : DataError
    data object Timeout : DataError
    /** Hash inválido o deep link viejo. */
    data object NotFound : DataError
    data class Server(val code: Int) : DataError
    data class Unknown(val message: String?) : DataError
}

/**
 * Resultado de una operación de datos. [cachedAtMillis] distinto de null indica que los datos
 * salen de la caché local porque la red falló: la UI muestra "Sin conexión · datos del …".
 */
sealed interface AppResult<out T> {
    /**
     * [dataAtMillis]: fecha de los datos cuando no son "de ahora" sin ser caché de emergencia
     * (el padrón semanal se publicó en ese momento).
     */
    data class Success<T>(val data: T, val cachedAtMillis: Long? = null, val dataAtMillis: Long? = null) : AppResult<T>
    data class Failure(val error: DataError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data), cachedAtMillis, dataAtMillis)
    is AppResult.Failure -> this
}
