package pe.cazadores.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.cazadores.core.result.AppResult
import pe.cazadores.core.result.DataError
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.Departamento
import pe.cazadores.domain.model.HomeStats
import pe.cazadores.domain.model.SearchFilters
import pe.cazadores.domain.model.SortOrder
import pe.cazadores.domain.repository.SettingsRepository
import pe.cazadores.domain.repository.TimeProvider
import pe.cazadores.domain.usecase.GetCatalogsUseCase
import pe.cazadores.domain.usecase.GetHomeStatsUseCase
import pe.cazadores.domain.usecase.GetTopWantedUseCase
import pe.cazadores.domain.usecase.SearchCriminalsUseCase

private const val NEAR_LIMIT = 6

data class HomeUiState(
    val isLoading: Boolean = true,
    val top: List<Criminal> = emptyList(),
    val stats: HomeStats? = null,
    /** Error solo si no hay nada que mostrar (ni top ni caché). */
    val error: DataError? = null,
    val updatedAtMillis: Long? = null,
    val cachedAtMillis: Long? = null,
    /** Departamentos para el selector de "Cerca de ti" (vacío mientras no carga el catálogo). */
    val departamentos: List<Departamento> = emptyList(),
    /** Id elegido (aunque el catálogo aún no haya cargado su nombre). */
    val nearId: String? = null,
    /** Ya se leyó el ajuste (evita mostrar el aviso de "elegir" antes de tiempo). */
    val nearReady: Boolean = false,
    val nearDepartamento: Departamento? = null,
    val near: List<Criminal> = emptyList(),
    val nearLoading: Boolean = false,
    /** "Cerca de ti" falló: no afecta al resto de Inicio. */
    val nearFailed: Boolean = false,
)

class HomeViewModel(
    private val getTop: GetTopWantedUseCase,
    private val getStats: GetHomeStatsUseCase,
    private val search: SearchCriminalsUseCase,
    private val getCatalogs: GetCatalogsUseCase,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var pickedId: String? = null
    private var nearJob: Job? = null

    init {
        load()
        // El ajuste se observa de inmediato: "Cerca de ti" no espera al catálogo.
        viewModelScope.launch {
            settings.nearDepartamentoId.collect { id ->
                pickedId = id
                val known = _state.value.departamentos
                if (id != null && known.isNotEmpty() && known.none { it.id == id }) {
                    // Id guardado que ya no existe (el catálogo llegó antes que el ajuste): se descarta sin buscar.
                    onPickDepartamento(null)
                } else {
                    startNear(id)
                }
            }
        }
        viewModelScope.launch {
            val departamentos = getCatalogs().departamentos
            _state.update { it.copy(departamentos = departamentos, nearDepartamento = departamentos.firstOrNull { d -> d.id == it.nearId }) }
            val saved = pickedId
            // Id guardado que ya no existe en el catálogo: se descarta como "sin elegir".
            if (saved != null && departamentos.isNotEmpty() && departamentos.none { it.id == saved }) onPickDepartamento(null)
        }
    }

    fun onRetry() {
        load()
        retryNear()
    }

    fun retryNear() = startNear(pickedId)

    /** `null` borra la elección y oculta la sección. */
    fun onPickDepartamento(id: String?) {
        viewModelScope.launch { settings.setNearDepartamento(id) }
    }

    /** Un solo trabajo de "Cerca de ti": el nuevo cancela al anterior. */
    private fun startNear(id: String?) {
        nearJob?.cancel()
        nearJob = viewModelScope.launch { loadNear(id) }
    }

    private suspend fun loadNear(id: String?) {
        if (id == null) {
            _state.update {
                it.copy(nearReady = true, nearId = null, nearDepartamento = null, near = emptyList(), nearLoading = false, nearFailed = false)
            }
            return
        }
        _state.update {
            it.copy(
                nearReady = true,
                nearId = id,
                nearDepartamento = it.departamentos.firstOrNull { d -> d.id == id },
                near = emptyList(),
                nearLoading = true,
                nearFailed = false,
            )
        }
        val result = search(SearchFilters(departamentoId = id, sort = SortOrder.REWARD), page = 1)
        _state.update {
            when (result) {
                is AppResult.Success -> it.copy(near = result.data.items.take(NEAR_LIMIT), nearLoading = false)
                is AppResult.Failure -> it.copy(nearLoading = false, nearFailed = true)
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val top = async { getTop() }
            val stats = async { getStats() }
            val topResult = top.await()
            val statsResult = stats.await()
            _state.update { current ->
                val topItems = (topResult as? AppResult.Success)?.data
                val cachedAt = (topResult as? AppResult.Success)?.cachedAtMillis
                current.copy(
                    isLoading = false,
                    top = topItems ?: current.top,
                    stats = (statsResult as? AppResult.Success)?.data ?: current.stats,
                    cachedAtMillis = cachedAt,
                    updatedAtMillis = if (topItems != null) cachedAt ?: time.nowMillis() else current.updatedAtMillis,
                    error = if (topItems == null && current.top.isEmpty()) (topResult as AppResult.Failure).error else null,
                )
            }
        }
    }
}
