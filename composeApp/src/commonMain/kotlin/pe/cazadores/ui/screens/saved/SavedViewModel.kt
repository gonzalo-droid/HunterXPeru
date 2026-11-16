package pe.cazadores.ui.screens.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.usecase.ObserveFavoritesUseCase
import pe.cazadores.domain.usecase.ToggleFavoriteUseCase

data class SavedUiState(
    val isLoading: Boolean = true,
    val items: List<Criminal> = emptyList(),
)

class SavedViewModel(
    observeFavorites: ObserveFavoritesUseCase,
    private val favorites: ToggleFavoriteUseCase,
) : ViewModel() {

    val state: StateFlow<SavedUiState> = observeFavorites()
        .map { SavedUiState(isLoading = false, items = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedUiState())

    fun onRemove(criminal: Criminal) {
        viewModelScope.launch { favorites.remove(criminal.hash) }
    }

    fun onUndoRemove(criminal: Criminal) {
        viewModelScope.launch { favorites.restore(criminal) }
    }
}
