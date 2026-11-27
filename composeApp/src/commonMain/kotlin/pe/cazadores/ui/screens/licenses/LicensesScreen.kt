package pe.cazadores.ui.screens.licenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.back
import pe.cazadores.resources.licenses_intro
import pe.cazadores.resources.licenses_open_source
import pe.cazadores.resources.licenses_row_meta
import pe.cazadores.resources.licenses_third_party
import pe.cazadores.resources.licenses_title
import pe.cazadores.ui.components.ScreenTitle
import pe.cazadores.ui.components.SectionLabel
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

@Composable
fun LicensesRoute(onBack: () -> Unit, actions: ExternalActions = koinInject()) {
    LicensesScreen(onBack = onBack, onOpen = actions::openUrl)
}

/** Lista fija de bibliotecas, fuentes y SDK que van dentro de la app (AJU-01 de la base). */
@Composable
fun LicensesScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(Cz.colors.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            IconButton(onClick = onBack) { Icon(CzIcons.Back, stringResource(Res.string.back), tint = Cz.colors.ink) }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = CazadoresDimens.screenPadding).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenTitle(stringResource(Res.string.licenses_title))
            Text(stringResource(Res.string.licenses_intro), style = Cz.type.bodySmall, color = Cz.colors.inkMuted)
            LicenseGroup(stringResource(Res.string.licenses_open_source), OPEN_SOURCE_LICENSES, onOpen)
            LicenseGroup(stringResource(Res.string.licenses_third_party), THIRD_PARTY_SDKS, onOpen)
        }
    }
}

@Composable
private fun LicenseGroup(title: String, entries: List<LicenseEntry>, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(title)
        Column(Modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card)) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = Cz.colors.line, modifier = Modifier.padding(horizontal = 16.dp))
                LicenseRow(entry, onClick = { onOpen(entry.url) })
            }
        }
    }
}

@Composable
private fun LicenseRow(entry: LicenseEntry, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.name, style = Cz.type.body, color = Cz.colors.ink)
            Text(
                stringResource(Res.string.licenses_row_meta, entry.author, entry.license),
                style = Cz.type.caption,
                color = Cz.colors.inkMuted,
            )
        }
        Icon(CzIcons.External, contentDescription = null, tint = Cz.colors.inkMuted, modifier = Modifier.size(20.dp))
    }
}
