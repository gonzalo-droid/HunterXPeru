package pe.cazadores.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.cazadores.ads.AdsController
import pe.cazadores.core.config.AppConfig
import pe.cazadores.core.config.AppInfo
import pe.cazadores.domain.model.ThemeMode
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.legend_title
import pe.cazadores.resources.settings_about
import pe.cazadores.resources.settings_about_body
import pe.cazadores.resources.settings_ads_prefs
import pe.cazadores.resources.settings_appearance
import pe.cazadores.resources.settings_info_section
import pe.cazadores.resources.settings_privacy_policy
import pe.cazadores.resources.settings_privacy_section
import pe.cazadores.resources.settings_rate
import pe.cazadores.resources.settings_regime
import pe.cazadores.resources.settings_rewards_section
import pe.cazadores.resources.settings_source
import pe.cazadores.resources.settings_terms
import pe.cazadores.resources.settings_theme_dark
import pe.cazadores.resources.settings_theme_light
import pe.cazadores.resources.settings_theme_system
import pe.cazadores.resources.settings_title
import pe.cazadores.resources.settings_version
import pe.cazadores.ui.components.ScreenTitle
import pe.cazadores.ui.components.SectionLabel
import pe.cazadores.ui.screens.legend.RankLegendSheet
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = koinViewModel(),
    onOpenRegime: () -> Unit,
    actions: ExternalActions = koinInject(),
    ads: AdsController = koinInject(),
) {
    var showLegend by rememberSaveable { mutableStateOf(false) }
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val privacyOptionsRequired by ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    SettingsScreen(
        theme = theme,
        showAdsPreferences = privacyOptionsRequired,
        onThemeChange = viewModel::onThemeChange,
        onAdsPreferences = ads::showPrivacyOptions,
        onOpenUrl = actions::openUrl,
        onRateApp = actions::openStoreReview,
        onOpenRegime = onOpenRegime,
        onOpenRanks = { showLegend = true },
    )
    if (showLegend) RankLegendSheet(onDismiss = { showLegend = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    theme: ThemeMode,
    showAdsPreferences: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onAdsPreferences: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onRateApp: () -> Unit,
    onOpenRegime: () -> Unit,
    onOpenRanks: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Cz.colors.bg).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = CazadoresDimens.screenPadding).padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenTitle(stringResource(Res.string.settings_title))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(Res.string.settings_appearance))
            val options = listOf(
                ThemeMode.SYSTEM to stringResource(Res.string.settings_theme_system),
                ThemeMode.LIGHT to stringResource(Res.string.settings_theme_light),
                ThemeMode.DARK to stringResource(Res.string.settings_theme_dark),
            )
            if (LocalDensity.current.fontScale > 1.3f) {
                // Con letra muy grande el control segmentado parte "Sistema": se pasa a filas.
                Column(Modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card).selectableGroup()) {
                    options.forEachIndexed { index, (mode, label) ->
                        if (index > 0) Divider()
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .selectable(selected = theme == mode, role = Role.RadioButton, onClick = { onThemeChange(mode) })
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = theme == mode,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Cz.colors.ink,
                                    unselectedColor = Cz.colors.inkMuted,
                                ),
                            )
                            Text(label, style = Cz.type.bodyStrong, color = Cz.colors.ink, modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = theme == mode,
                            onClick = { onThemeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            modifier = Modifier.heightIn(min = CazadoresDimens.touchTarget),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Cz.colors.ink,
                                activeContentColor = Cz.colors.bg,
                                inactiveContainerColor = Cz.colors.surface,
                                inactiveContentColor = Cz.colors.ink,
                                activeBorderColor = Cz.colors.ink,
                                inactiveBorderColor = Cz.colors.lineStrong,
                            ),
                            label = { Text(label, style = Cz.type.bodyStrong) },
                        )
                    }
                }
            }
        }

        Group(stringResource(Res.string.settings_rewards_section)) {
            LinkRow(stringResource(Res.string.settings_regime), CzIcons.ChevronRight, onOpenRegime)
            Divider()
            LinkRow(stringResource(Res.string.legend_title), CzIcons.ChevronRight, onOpenRanks)
        }

        Group(stringResource(Res.string.settings_privacy_section)) {
            if (showAdsPreferences) {
                LinkRow(stringResource(Res.string.settings_ads_prefs), CzIcons.Shield, onAdsPreferences)
                Divider()
            }
            LinkRow(stringResource(Res.string.settings_privacy_policy), CzIcons.External) { onOpenUrl(AppConfig.PRIVACY_POLICY_URL) }
            Divider()
            LinkRow(stringResource(Res.string.settings_terms), CzIcons.External) { onOpenUrl(AppConfig.TERMS_URL) }
        }

        Group(stringResource(Res.string.settings_info_section)) {
            LinkRow(stringResource(Res.string.settings_source), CzIcons.External) { onOpenUrl(AppConfig.SOURCE_URL) }
            Divider()
            LinkRow(stringResource(Res.string.settings_rate), CzIcons.Star, onRateApp)
        }

        Column(
            Modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Res.string.settings_about).uppercase(), style = Cz.type.title, color = Cz.colors.ink)
            Text(stringResource(Res.string.settings_about_body), style = Cz.type.bodySmall, color = Cz.colors.ink)
        }

        Text(stringResource(Res.string.settings_version, AppInfo.VERSION_NAME), style = Cz.type.mono, color = Cz.colors.inkMuted)
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(title)
        Column(Modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card)) { content() }
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = Cz.colors.line, modifier = Modifier.padding(horizontal = 16.dp))

@Composable
private fun LinkRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = Cz.type.body, color = Cz.colors.ink, modifier = Modifier.weight(1f))
        Icon(icon, contentDescription = null, tint = Cz.colors.inkMuted, modifier = Modifier.size(20.dp))
    }
}
