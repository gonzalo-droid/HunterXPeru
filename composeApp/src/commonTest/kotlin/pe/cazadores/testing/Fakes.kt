package pe.cazadores.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import pe.cazadores.core.result.AppResult
import pe.cazadores.domain.model.Catalogs
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.HomeStats
import pe.cazadores.domain.model.RewardPosition
import pe.cazadores.domain.model.SearchFilters
import pe.cazadores.domain.model.SearchPage
import pe.cazadores.domain.repository.CatalogRepository
import pe.cazadores.domain.repository.CriminalRepository
import pe.cazadores.domain.model.ThemeMode
import pe.cazadores.domain.repository.FavoritesRepository
import pe.cazadores.domain.repository.SettingsRepository

fun criminal(hash: String, name: String = "PERSONA $hash", crimes: List<String> = listOf("EXTORSION")) =
    Criminal(id = hash.hashCode(), hash = hash, displayName = name, reward = 10_000.0, crimes = crimes)

class FakeFavoritesRepository : FavoritesRepository {
    val items = MutableStateFlow<List<Criminal>>(emptyList())
    override fun observeAll(): Flow<List<Criminal>> = items
    override fun observeIsFavorite(hash: String): Flow<Boolean> = items.map { list -> list.any { it.hash == hash } }
    override suspend fun save(criminal: Criminal) {
        items.value = items.value.filterNot { it.hash == criminal.hash } + criminal
    }
    override suspend fun remove(hash: String) {
        items.value = items.value.filterNot { it.hash == hash }
    }
}

/** Repositorio programable: cada llamada a `search` consume la siguiente respuesta. */
class FakeCriminalRepository : CriminalRepository {
    val searchCalls = mutableListOf<Pair<SearchFilters, Int>>()
    var searchResponder: (SearchFilters, Int) -> AppResult<SearchPage> = { _, page ->
        AppResult.Success(SearchPage(List(20) { criminal("p$page-$it") }, page, 45, 3, isLast = page >= 3))
    }
    var detailResult: AppResult<Criminal> = AppResult.Success(criminal("D"))
    var topResult: AppResult<List<Criminal>> = AppResult.Success(listOf(criminal("T1"), criminal("T2")))
    var statsResult: AppResult<HomeStats> = AppResult.Success(HomeStats(649, 500_000.0))

    override suspend fun search(filters: SearchFilters, page: Int): AppResult<SearchPage> {
        searchCalls += filters to page
        return searchResponder(filters, page)
    }
    val topLimits = mutableListOf<Int>()
    override suspend fun topWanted(limit: Int): AppResult<List<Criminal>> {
        topLimits += limit
        return topResult
    }
    override suspend fun detail(hash: String) = detailResult
    override suspend fun homeStats() = statsResult
    var positionResult: RewardPosition? = null
    override suspend fun rewardPosition(hash: String) = positionResult
}

class FakeCatalogRepository : CatalogRepository {
    var catalogs = Catalogs(emptyList(), emptyList(), emptyList())
    var gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    override suspend fun catalogs(): Catalogs {
        gate?.await()
        return catalogs
    }
}

class FakeSettingsRepository : SettingsRepository {
    override val onboardingDone: Flow<Boolean> = MutableStateFlow(true)
    override val themeMode: Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)
    override suspend fun completeOnboarding() = Unit
    override suspend fun setThemeMode(mode: ThemeMode) = Unit
}
