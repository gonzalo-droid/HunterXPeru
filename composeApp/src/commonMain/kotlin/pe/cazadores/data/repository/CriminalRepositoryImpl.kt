package pe.cazadores.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import pe.cazadores.core.config.AppConfig
import pe.cazadores.core.result.AppResult
import pe.cazadores.core.result.DataError
import pe.cazadores.data.local.CacheDao
import pe.cazadores.data.local.CachedCriminal
import pe.cazadores.data.local.DetailCacheEntity
import pe.cazadores.data.local.SearchCacheEntity
import pe.cazadores.data.local.SearchCacheMetaEntity
import pe.cazadores.data.local.toCached
import pe.cazadores.data.local.toDomain
import pe.cazadores.data.mapper.toDomain
import pe.cazadores.data.remote.RecompensasApi
import pe.cazadores.data.remote.dto.PageInfoDto
import pe.cazadores.data.remote.dto.SearchDto
import pe.cazadores.data.remote.dto.SearchRequestDto
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.HomeStats
import pe.cazadores.domain.model.SearchFilters
import pe.cazadores.domain.model.SearchPage
import pe.cazadores.domain.model.SortOrder
import pe.cazadores.domain.repository.CriminalRepository
import pe.cazadores.domain.repository.NetworkMonitor
import pe.cazadores.domain.repository.TimeProvider

class CriminalRepositoryImpl(
    private val api: RecompensasApi,
    private val cache: CacheDao,
    private val time: TimeProvider,
    private val json: Json,
    private val network: NetworkMonitor,
    private val pageSize: Int = AppConfig.PAGE_SIZE,
) : CriminalRepository {

    /**
     * Sin red no tiene sentido esperar los reintentos del cliente (~14 s): se va directo a la caché.
     * iOS publica el primer estado de forma asíncrona; si no llega a tiempo se asume que hay red.
     */
    private suspend fun isOffline(): Boolean =
        withTimeoutOrNull(NETWORK_CHECK_TIMEOUT_MS) { network.isOnline.first() } == false

    private suspend inline fun <T> remote(block: () -> T): AppResult<T> =
        if (isOffline()) AppResult.Failure(DataError.Network) else apiCall(block)

    override suspend fun search(filters: SearchFilters, page: Int): AppResult<SearchPage> {
        val remote = remote { api.search(filters.toRequest(page, pageSize)) }
        return when (remote) {
            is AppResult.Success -> {
                val dto = remote.data
                val result = SearchPage(
                    items = dto.content.map { it.toDomain() },
                    page = PageTranslation.toUiPage(dto.number),
                    totalElements = dto.totalElements,
                    totalPages = dto.totalPages,
                    isLast = dto.last || dto.content.isEmpty(),
                )
                if (page == 1) storeSearch(filters.cacheKey, result)
                AppResult.Success(result)
            }
            is AppResult.Failure ->
                if (page == 1 && remote.error.allowsCacheFallback) cachedSearch(filters.cacheKey) ?: remote else remote
        }
    }

    override suspend fun topWanted(): AppResult<List<Criminal>> {
        val remote = remote { api.top5().map { it.toDomain() } }
        return when (remote) {
            is AppResult.Success -> {
                storeSearch(TOP_KEY, SearchPage(remote.data, 1, remote.data.size, 1, true))
                remote
            }
            is AppResult.Failure ->
                if (remote.error.allowsCacheFallback) {
                    cachedSearch(TOP_KEY)?.let { AppResult.Success(it.data.items, it.cachedAtMillis) } ?: remote
                } else {
                    remote
                }
        }
    }

    override suspend fun detail(hash: String): AppResult<Criminal> {
        val remote = remote { api.detail(hash).toDomain() }
        return when (remote) {
            is AppResult.Success -> {
                if (remote.data.hash.isBlank()) return AppResult.Failure(DataError.NotFound)
                cache.deleteDetailsOlderThan(time.nowMillis() - TTL_MILLIS)
                cache.upsertDetail(DetailCacheEntity(hash, json.encodeToString(CachedCriminal.serializer(), remote.data.toCached()), time.nowMillis()))
                remote
            }
            is AppResult.Failure -> {
                if (!remote.error.allowsCacheFallback) return remote
                val cached = cache.detail(hash)?.takeIf { time.nowMillis() - it.cachedAt <= TTL_MILLIS }
                    ?: return remote
                AppResult.Success(json.decodeFromString(CachedCriminal.serializer(), cached.json).toDomain(), cached.cachedAt)
            }
        }
    }

    /** Total = búsqueda vacía; máximo = primer ítem ordenado por recompensa (bug B2 de Flutter). */
    override suspend fun homeStats(): AppResult<HomeStats> = remote {
        val all = api.search(SearchFilters().toRequest(page = 1, size = 1))
        val top = api.search(SearchFilters(sort = SortOrder.REWARD).toRequest(page = 1, size = 1))
        HomeStats(total = all.totalElements, maxReward = top.content.firstOrNull()?.montoRecompensa ?: 0.0)
    }

    private suspend fun storeSearch(key: String, page: SearchPage) {
        val now = time.nowMillis()
        val entries = page.items.take(MAX_ROWS_PER_KEY).mapIndexed { index, criminal ->
            SearchCacheEntity(key, criminal.hash, index, json.encodeToString(CachedCriminal.serializer(), criminal.toCached()), now)
        }
        cache.replaceSearch(
            key = key,
            entries = entries,
            meta = SearchCacheMetaEntity(key, page.totalElements, page.totalPages, now),
            expiredBefore = now - TTL_MILLIS,
            maxKeys = MAX_KEYS,
        )
    }

    private suspend fun cachedSearch(key: String): AppResult.Success<SearchPage>? {
        val meta = cache.searchMeta(key)?.takeIf { time.nowMillis() - it.cachedAt <= TTL_MILLIS } ?: return null
        val items = cache.searchEntries(key).map { json.decodeFromString(CachedCriminal.serializer(), it.json).toDomain() }
        if (items.isEmpty()) return null
        return AppResult.Success(
            SearchPage(items, page = 1, totalElements = meta.totalElements, totalPages = meta.totalPages, isLast = true),
            cachedAtMillis = meta.cachedAt,
        )
    }

    companion object {
        const val TTL_MILLIS = 24L * 60 * 60 * 1000
        const val MAX_ROWS_PER_KEY = 100
        const val MAX_KEYS = 20
        internal const val TOP_KEY = "__top5__"
        private const val NETWORK_CHECK_TIMEOUT_MS = 500L
    }
}

internal fun SearchFilters.toRequest(page: Int, size: Int): SearchRequestDto {
    val (sortBy, direction) = PageTranslation.sortField(sort)
    return SearchRequestDto(
        pageInfo = PageInfoDto(page = PageTranslation.toRequestPage(page), size = size, sortBy = sortBy, direction = direction),
        search = SearchDto(
            nombreCompleto = query.trim().ifBlank { null },
            idDepartamento = departamentoId,
            idProvincia = provinciaId?.takeIf { departamentoId != null },
            idDelito = delitoId,
        ),
    )
}
