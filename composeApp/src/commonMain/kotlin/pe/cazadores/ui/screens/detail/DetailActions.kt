package pe.cazadores.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.core.config.AppConfig
import pe.cazadores.resources.Res
import pe.cazadores.resources.back
import pe.cazadores.resources.call_line
import pe.cazadores.resources.detail_report
import pe.cazadores.resources.detail_save
import pe.cazadores.resources.detail_saved
import pe.cazadores.resources.detail_share
import pe.cazadores.ui.components.CallButton
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

@Composable
internal fun TopActions(
    showActions: Boolean,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(CzIcons.Back, stringResource(Res.string.back), tint = Cz.colors.ink) }
        Spacer(Modifier.weight(1f))
        if (showActions) {
            IconButton(onClick = onShare) { Icon(CzIcons.Share, stringResource(Res.string.detail_share), tint = Cz.colors.ink) }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) CzIcons.BookmarkFilled else CzIcons.Bookmark,
                    contentDescription = stringResource(if (isFavorite) Res.string.detail_saved else Res.string.detail_save),
                    tint = Cz.colors.ink,
                )
            }
        }
    }
}

@Composable
internal fun BottomActions(onCall: () -> Unit, onReport: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Cz.colors.bg)) {
        HorizontalDivider(color = Cz.colors.line)
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = CazadoresDimens.screenPadding, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CallButton(
                text = stringResource(Res.string.call_line, AppConfig.REPORT_PHONE),
                onClick = onCall,
                icon = CzIcons.Phone,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onReport,
                modifier = Modifier.size(56.dp)
                    .border(1.5.dp, Cz.colors.ink, CazadoresShapes.button)
                    .background(Cz.colors.surface, CazadoresShapes.button),
            ) { Icon(CzIcons.Edit, contentDescription = stringResource(Res.string.detail_report), tint = Cz.colors.ink) }
        }
    }
}
