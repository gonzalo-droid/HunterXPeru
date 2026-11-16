package pe.cazadores.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
 * Orden: memoria → caché local de menos de 7 días → API → respaldo empaquetado.
 * Si la API falla, se usa la caché aunque esté vencida antes que el respaldo.
 */
class CatalogRepositoryImpl(
    private val api: RecompensasApi,
    private val cache: CacheDao,
    private val bundled: BundledCatalogSource,
    private val time: TimeProvider,
    private val json: Json,
) : CatalogRepository {
    private val ubigeoList = ListSerializer(UbigeoDto.serializer())
    private val delitoList = ListSerializer(DelitoDto.serializer())
    private val mutex = Mutex()
    private var memory: Catalogs? = null

    override suspend fun catalogs(): Catalogs = mutex.withLock {
        memory ?: load().also { memory = it }
    }

    private suspend fun load(): Catalogs {
        val delitos = cached(DELITOS, delitoList) { api.delitos() }
        val departamentos = cached(DEPARTAMENTOS, ubigeoList) { api.departamentos() }
        val provincias = cached(PROVINCIAS, ubigeoList) {
            coroutineScope {
                departamentos.map { dep -> async { api.provincias(dep.departamento) } }.awaitAll().flatten()
            }
        }
        return Catalogs(
            delitos = delitos.map { it.toDomain() }.filter { it.name.isNotBlank() }.distinctBy { it.name.uppercase() }.sortedBy { it.name },
            departamentos = departamentos.map { it.toDepartamento() }.sortedBy { it.name },
            provincias = provincias.map { it.toProvincia() }.sortedBy { it.name },
        )
    }

    private suspend fun <T> cached(
        key: String,
        serializer: kotlinx.serialization.KSerializer<List<T>>,
        fetch: suspend () -> List<T>,
    ): List<T> {
        val stored = cache.catalog(key)
        if (stored != null && time.nowMillis() - stored.cachedAt <= TTL_MILLIS) {
            decode(stored.json, serializer)?.let { return it }
        }
        val remote = try {
            fetch().takeIf { it.isNotEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
        if (remote != null) {
            cache.upsertCatalog(CatalogCacheEntity(key, json.encodeToString(serializer, remote), time.nowMillis()))
            return remote
        }
        stored?.let { decode(it.json, serializer) }?.let { return it }
        return decode(bundled.read(key), serializer).orEmpty()
    }

    private fun <T> decode(text: String, serializer: kotlinx.serialization.KSerializer<List<T>>): List<T>? =
        runCatching { json.decodeFromString(serializer, text) }.getOrNull()

    companion object {
        const val TTL_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val DELITOS = "delitos"
        const val DEPARTAMENTOS = "departamentos"
        const val PROVINCIAS = "provincias"
    }
}
