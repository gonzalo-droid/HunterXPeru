package pe.cazadores.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.cazadores.domain.model.ThemeMode
import pe.cazadores.domain.repository.SettingsRepository

sealed interface AppUiState {
    data object Loading : AppUiState
    data class Ready(val showOnboarding: Boolean, val themeMode: ThemeMode) : AppUiState
}

class AppViewModel(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<AppUiState> = combine(settings.onboardingDone, settings.themeMode) { done, theme ->
        AppUiState.Ready(showOnboarding = !done, themeMode = theme)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState.Loading)

    fun onOnboardingFinished() {
        viewModelScope.launch { settings.completeOnboarding() }
    }
}
