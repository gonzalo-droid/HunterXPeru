package pe.cazadores.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import pe.cazadores.domain.model.ThemeMode

const val SETTINGS_FILE_NAME = "cazadores.preferences_pb"

fun createDataStore(absolutePath: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { absolutePath.toPath() })

class SettingsDataSource(private val store: DataStore<Preferences>) {
    private val onboardingDone = booleanPreferencesKey("onboarding_done")
    private val themeMode = stringPreferencesKey("theme_mode")
    private val nearDepartamento = stringPreferencesKey("near_departamento")

    val nearDepartamentoFlow: Flow<String?> = store.data.map { it[nearDepartamento] }

    val onboardingDoneFlow: Flow<Boolean> = store.data.map { it[onboardingDone] ?: false }

    val themeModeFlow: Flow<ThemeMode> = store.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeMode] } ?: ThemeMode.SYSTEM
    }

    suspend fun setOnboardingDone() {
        store.edit { it[onboardingDone] = true }
    }

    suspend fun setNearDepartamento(id: String?) {
        store.edit { if (id == null) it.remove(nearDepartamento) else it[nearDepartamento] = id }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[themeMode] = mode.name }
    }
}
