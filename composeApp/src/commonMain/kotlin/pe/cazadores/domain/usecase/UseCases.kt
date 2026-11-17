package pe.cazadores.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.cazadores.core.result.AppResult
import pe.cazadores.domain.model.Catalogs
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.HomeStats
import pe.cazadores.domain.model.RewardPosition
import pe.cazadores.domain.model.SearchFilters
import pe.cazadores.domain.model.SearchPage
import pe.cazadores.domain.repository.CatalogRepository
import pe.cazadores.domain.repository.CriminalRepository
import pe.cazadores.domain.repository.FavoritesRepository

class GetTopWantedUseCase(private val repository: CriminalRepository) {
    suspend operator fun invoke(): AppResult<List<Criminal>> = repository.topWanted()
}

class SearchCriminalsUseCase(private val repository: CriminalRepository) {
    /** [page] es 1-indexed. */
    suspend operator fun invoke(filters: SearchFilters, page: Int): AppResult<SearchPage> =
        repository.search(filters, page)
}

class GetCriminalDetailUseCase(
    private val repository: CriminalRepository,
    private val favorites: FavoritesRepository,
) {
    /** Si la persona está guardada, aprovecha el detalle para completar el favorito. */
    suspend operator fun invoke(hash: String): AppResult<Criminal> {
        val result = repository.detail(hash)
        if (result is AppResult.Success && result.cachedAtMillis == null &&
            favorites.observeIsFavorite(hash).first()
        ) {
            favorites.save(result.data)
        }
        return result
    }
}

/** Puesto de la persona entre todas, por monto de recompensa. `null` si no hay padrón. */
class GetRewardPositionUseCase(private val repository: CriminalRepository) {
    suspend operator fun invoke(hash: String): RewardPosition? = repository.rewardPosition(hash)
}

class GetHomeStatsUseCase(private val repository: CriminalRepository) {
    suspend operator fun invoke(): AppResult<HomeStats> = repository.homeStats()
}

class GetCatalogsUseCase(private val repository: CatalogRepository) {
    suspend operator fun invoke(): Catalogs = repository.catalogs()
}

class ObserveFavoritesUseCase(private val repository: FavoritesRepository) {
    operator fun invoke(): Flow<List<Criminal>> = repository.observeAll()
}

class IsFavoriteUseCase(private val repository: FavoritesRepository) {
    operator fun invoke(hash: String): Flow<Boolean> = repository.observeIsFavorite(hash)
}

/**
 * Guarda o quita. Al guardar desde una lista (sin apellidos ni ubicación) trae el detalle en
 * segundo plano, en [appScope], para que el favorito quede completo aunque se cierre la pantalla.
 */
class ToggleFavoriteUseCase(
    private val favorites: FavoritesRepository,
    private val criminals: CriminalRepository,
    private val appScope: CoroutineScope,
) {
    /** Devuelve `true` si quedó guardado. */
    suspend operator fun invoke(criminal: Criminal): Boolean {
        if (favorites.observeIsFavorite(criminal.hash).first()) {
            favorites.remove(criminal.hash)
            return false
        }
        favorites.save(criminal)
        if (!criminal.isComplete) {
            appScope.launch {
                val detail = criminals.detail(criminal.hash)
                if (detail is AppResult.Success && favorites.observeIsFavorite(criminal.hash).first()) {
                    favorites.save(detail.data)
                }
            }
        }
        return true
    }

    /** "Deshacer" tras quitar desde Guardados. */
    suspend fun restore(criminal: Criminal) = favorites.save(criminal)

    suspend fun remove(hash: String) = favorites.remove(hash)
}
