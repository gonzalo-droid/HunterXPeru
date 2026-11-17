package pe.cazadores.data.repository

import kotlinx.coroutines.flow.Flow
import pe.cazadores.data.local.SettingsDataSource
import pe.cazadores.domain.model.ThemeMode
import pe.cazadores.domain.repository.SettingsRepository

class SettingsRepositoryImpl(private val source: SettingsDataSource) : SettingsRepository {
    override val onboardingDone: Flow<Boolean> = source.onboardingDoneFlow
    override val themeMode: Flow<ThemeMode> = source.themeModeFlow
    override val nearDepartamentoId: Flow<String?> = source.nearDepartamentoFlow
    override suspend fun setNearDepartamento(id: String?) = source.setNearDepartamento(id)
    override suspend fun completeOnboarding() = source.setOnboardingDone()
    override suspend fun setThemeMode(mode: ThemeMode) = source.setThemeMode(mode)
}
