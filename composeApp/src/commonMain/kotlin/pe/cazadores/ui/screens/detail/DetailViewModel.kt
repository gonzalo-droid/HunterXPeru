package pe.cazadores.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.cazadores.core.DangerAssessment
import pe.cazadores.core.DangerLevelCalculator
import pe.cazadores.core.SpecialRegime
import pe.cazadores.core.result.AppResult
import pe.cazadores.core.result.DataError
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.RewardPosition
import pe.cazadores.domain.repository.TimeProvider
import pe.cazadores.domain.usecase.GetCriminalDetailUseCase
import pe.cazadores.domain.usecase.GetRewardPositionUseCase
import pe.cazadores.domain.usecase.IsFavoriteUseCase
import pe.cazadores.domain.usecase.ToggleFavoriteUseCase

sealed interface DetailContent {
    data object Loading : DetailContent
    data class Error(val error: DataError) : DetailContent
    data class Loaded(
        val criminal: Criminal,
        val danger: DangerAssessment,
        /** Delitos de extorsión o sicariato: aplica el régimen especial de recompensas. */
        val specialRegime: Boolean,
        /** Momento de los datos: ahora, o la fecha de la caché si no hubo red. */
        val dataAtMillis: Long,
        val fromCache: Boolean,
        /** Puesto nacional por recompensa; `null` sin padrón o sin monto. */
        val position: RewardPosition? = null,
    ) : DetailContent
}

data class DetailUiState(
    val content: DetailContent = DetailContent.Loading,
    val isFavorite: Boolean = false,
    val showReport: Boolean = false,
)

sealed interface DetailEvent {
    data class FavoriteChanged(val saved: Boolean) : DetailEvent
}

class DetailViewModel(
    private val hash: String,
    private val getDetail: GetCriminalDetailUseCase,
    isFavorite: IsFavoriteUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val time: TimeProvider,
    private val getPosition: GetRewardPositionUseCase,
) : ViewModel() {

    private val content = MutableStateFlow<DetailContent>(DetailContent.Loading)
    private val showReport = MutableStateFlow(false)
    private val events = Channel<DetailEvent>(Channel.BUFFERED)

    val eventFlow: Flow<DetailEvent> = events.receiveAsFlow()

    val state: StateFlow<DetailUiState> =
        combine(content, isFavorite(hash), showReport) { c, fav, report -> DetailUiState(c, fav, report) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    init {
        load()
    }

    fun onRetry() = load()

    fun onToggleFavorite() {
        val loaded = content.value as? DetailContent.Loaded ?: return
        viewModelScope.launch {
            val saved = toggleFavorite(loaded.criminal)
            events.send(DetailEvent.FavoriteChanged(saved))
        }
    }

    fun onOpenReport() {
        showReport.value = true
    }

    fun onDismissReport() {
        showReport.value = false
    }

    private fun load() {
        viewModelScope.launch {
            content.value = DetailContent.Loading
            content.value = when (val result = getDetail(hash)) {
                is AppResult.Success -> DetailContent.Loaded(
                    criminal = result.data,
                    danger = DangerLevelCalculator.assess(result.data.crimes),
                    specialRegime = SpecialRegime.applies(result.data.crimes),
                    dataAtMillis = result.cachedAtMillis ?: result.dataAtMillis ?: time.nowMillis(),
                    fromCache = result.cachedAtMillis != null,
                    position = getPosition(hash),
                )
                is AppResult.Failure -> DetailContent.Error(result.error)
            }
        }
    }
}
