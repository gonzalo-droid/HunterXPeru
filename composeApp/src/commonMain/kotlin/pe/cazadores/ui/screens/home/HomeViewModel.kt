package pe.cazadores.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.cazadores.core.result.AppResult
import pe.cazadores.core.result.DataError
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.HomeStats
import pe.cazadores.domain.repository.TimeProvider
import pe.cazadores.domain.usecase.GetHomeStatsUseCase
import pe.cazadores.domain.usecase.GetTopWantedUseCase

data class HomeUiState(
    val isLoading: Boolean = true,
    val top: List<Criminal> = emptyList(),
    val stats: HomeStats? = null,
    /** Error solo si no hay nada que mostrar (ni top ni caché). */
    val error: DataError? = null,
    val updatedAtMillis: Long? = null,
    val cachedAtMillis: Long? = null,
)

class HomeViewModel(
    private val getTop: GetTopWantedUseCase,
    private val getStats: GetHomeStatsUseCase,
    private val time: TimeProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

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
