package pe.cazadores.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pe.cazadores.data.local.CacheDao
import pe.cazadores.data.local.CatalogCacheEntity
import pe.cazadores.data.mapper.toDepartamento
import pe.cazadores.data.mapper.toDomain
import pe.cazadores.data.mapper.toProvincia
import pe.cazadores.data.remote.RecompensasApi
import pe.cazadores.data.remote.dto.DelitoDto
import pe.cazadores.data.remote.dto.UbigeoDto
import pe.cazadores.domain.model.Catalogs
import pe.cazadores.domain.repository.CatalogRepository
import pe.cazadores.domain.repository.TimeProvider

/** Catálogos empaquetados en la app (composeResources/files/catalogs). */
fun interface BundledCatalogSource {
    suspend fun read(name: String): String
}

/**
 * Departamentos y provincias salen solo del archivo empaquetado: no cambian, así que no se piden a la API
 * ni se guardan en Room (D32).
 * Delitos salen al instante de la caché de Room (o del archivo empaquetado si no hay). Si la caché falta o
 * tiene más de 7 días, se pide la API en segundo plano: nunca se espera a la red.
 */
class CatalogRepositoryImpl(
    private val api: RecompensasApi,
    private val cache: CacheDao,
    private val bundled: BundledCatalogSource,
    private val time: TimeProvider,
    private val json: Json,
    private val scope: CoroutineScope,
) : CatalogRepository {
    private val ubigeoList = ListSerializer(UbigeoDto.serializer())
    private val delitoList = ListSerializer(DelitoDto.serializer())
    private val mutex = Mutex()
    private var memory: Catalogs? = null

    override suspend fun catalogs(): Catalogs = mutex.withLock {
        memory ?: load().also { memory = it }
    }

    private suspend fun load(): Catalogs {
        val stored = cache.catalog(DELITOS)
        val delitos = stored?.let { decode(it.json, delitoList) } ?: bundled(DELITOS, delitoList)
        if (stored == null || time.nowMillis() - stored.cachedAt > TTL_MILLIS) scope.launch { refreshDelitos() }
        return Catalogs(
            delitos = delitos.toDomain(),
            departamentos = bundled(DEPARTAMENTOS, ubigeoList).map { it.toDepartamento() }.sortedBy { it.name },
            provincias = bundled(PROVINCIAS, ubigeoList).map { it.toProvincia() }.sortedBy { it.name },
        )
    }

    /** Sin red o con error no hace nada: queda lo que había y se reintenta en la próxima carga. */
    private suspend fun refreshDelitos() {
        val remote = try {
            api.delitos().takeIf { it.isNotEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        } ?: return
        cache.upsertCatalog(CatalogCacheEntity(DELITOS, json.encodeToString(delitoList, remote), time.nowMillis()))
        mutex.withLock { memory = memory?.copy(delitos = remote.toDomain()) }
    }

    private fun List<DelitoDto>.toDomain() =
        map { it.toDomain() }.filter { it.name.isNotBlank() }.distinctBy { it.name.uppercase() }.sortedBy { it.name }

    private suspend fun <T> bundled(key: String, serializer: kotlinx.serialization.KSerializer<List<T>>): List<T> =
        decode(bundled.read(key), serializer).orEmpty()

    private fun <T> decode(text: String, serializer: kotlinx.serialization.KSerializer<List<T>>): List<T>? =
        runCatching { json.decodeFromString(serializer, text) }.getOrNull()

    companion object {
        const val TTL_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val DELITOS = "delitos"
        const val DEPARTAMENTOS = "departamentos"
        const val PROVINCIAS = "provincias"
    }
}
