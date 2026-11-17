package pe.cazadores.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.cazadores.core.result.AppResult
import pe.cazadores.domain.model.Catalogs
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.HomeStats
import pe.cazadores.domain.model.SearchFilters
import pe.cazadores.domain.model.SearchPage
import pe.cazadores.domain.model.ThemeMode

interface CriminalRepository {
    /** [page] es 1-indexed. Sin red, la página 1 sale de la caché de esos mismos filtros. */
    suspend fun search(filters: SearchFilters, page: Int): AppResult<SearchPage>
    suspend fun topWanted(): AppResult<List<Criminal>>
    suspend fun detail(hash: String): AppResult<Criminal>
    suspend fun homeStats(): AppResult<HomeStats>
}

interface FavoritesRepository {
    fun observeAll(): Flow<List<Criminal>>
    fun observeIsFavorite(hash: String): Flow<Boolean>
    suspend fun save(criminal: Criminal)
    suspend fun remove(hash: String)
}

interface CatalogRepository {
    suspend fun catalogs(): Catalogs
}

interface SettingsRepository {
    val onboardingDone: Flow<Boolean>
    val themeMode: Flow<ThemeMode>
    /** Departamento elegido para "Cerca de ti" (null = sin elegir). */
    val nearDepartamentoId: Flow<String?>
    suspend fun completeOnboarding()
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setNearDepartamento(id: String?)
}

/** Estado de la conexión (expect/actual por plataforma). */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

/** Reloj inyectable para probar TTLs. */
fun interface TimeProvider {
    fun nowMillis(): Long
}
