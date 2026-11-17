package pe.cazadores.ui.screens.regime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pe.cazadores.core.config.AppConfig
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.back
import pe.cazadores.resources.call_line
import pe.cazadores.resources.regime_1_body
import pe.cazadores.resources.regime_1_title
import pe.cazadores.resources.regime_2_body
import pe.cazadores.resources.regime_2_title
import pe.cazadores.resources.regime_3_body
import pe.cazadores.resources.regime_3_title
import pe.cazadores.resources.regime_4_body
import pe.cazadores.resources.regime_4_title
import pe.cazadores.resources.regime_5_body
import pe.cazadores.resources.regime_5_title
import pe.cazadores.resources.regime_closing
import pe.cazadores.resources.regime_decree
import pe.cazadores.resources.regime_intro
import pe.cazadores.resources.regime_open_official
import pe.cazadores.resources.regime_title
import pe.cazadores.resources.regime_unofficial
import pe.cazadores.ui.components.CallButton
import pe.cazadores.ui.components.StatusBarAppearance
import pe.cazadores.ui.components.TextAction
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

/**
 * Enlace al comunicado oficial del régimen especial.
 * TODO: el enlace exacto al comunicado está pendiente del responsable del producto; por ahora, la portada.
 */
const val SPECIAL_REGIME_URL = "https://recompensas.pe/"

private data class Point(val title: StringResource, val body: StringResource)

private val POINTS = listOf(
    Point(Res.string.regime_1_title, Res.string.regime_1_body),
    Point(Res.string.regime_2_title, Res.string.regime_2_body),
    Point(Res.string.regime_3_title, Res.string.regime_3_body),
    Point(Res.string.regime_4_title, Res.string.regime_4_body),
    Point(Res.string.regime_5_title, Res.string.regime_5_body),
)

@Composable
fun SpecialRegimeRoute(onBack: () -> Unit, actions: ExternalActions = koinInject()) {
    SpecialRegimeScreen(
        onBack = onBack,
        onCall = { actions.dial(AppConfig.REPORT_PHONE_URI) },
        onOpenOfficial = { actions.openUrl(SPECIAL_REGIME_URL) },
    )
}

/** Resumen del régimen especial (D. S. N.º 097-2025-PCM). Cabecera oscura; sin anuncios. */
@Composable
fun SpecialRegimeScreen(onBack: () -> Unit, onCall: () -> Unit, onOpenOfficial: () -> Unit) {
    StatusBarAppearance(lightIcons = true)
    Column(Modifier.fillMaxSize().background(Cz.colors.bg)) {
        // Solo el botón Volver queda fijo; el título y el decreto se desplazan con el contenido.
        Row(Modifier.fillMaxWidth().background(Cz.colors.header).statusBarsPadding().padding(horizontal = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(CzIcons.Back, stringResource(Res.string.back), tint = Cz.colors.onHeader)
            }
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().background(Cz.colors.header)
                    .padding(start = CazadoresDimens.screenPadding, end = CazadoresDimens.screenPadding, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(Res.string.regime_title),
                    style = Cz.type.headline,
                    color = Cz.colors.onHeader,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(Res.string.regime_decree), style = Cz.type.mono, color = Cz.colors.onHeaderMuted)
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = CazadoresDimens.screenPadding, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stringResource(Res.string.regime_intro), style = Cz.type.body, color = Cz.colors.ink)
                POINTS.forEachIndexed { index, point ->
                    Row(
                        Modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card).padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text((index + 1).toString(), style = Cz.type.title, color = Cz.colors.inkMuted)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(point.title), style = Cz.type.bodyStrong, color = Cz.colors.ink)
                            Text(stringResource(point.body), style = Cz.type.bodySmall, color = Cz.colors.inkMuted)
                        }
                    }
                }
                Text(
                    stringResource(Res.string.regime_closing),
                    style = Cz.type.title,
                    color = Cz.colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                TextAction(
                    text = stringResource(Res.string.regime_open_official),
                    onClick = onOpenOfficial,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(Res.string.regime_unofficial), style = Cz.type.caption, color = Cz.colors.inkMuted)
            }
        }

        Column(Modifier.fillMaxWidth().background(Cz.colors.bg)) {
            HorizontalDivider(color = Cz.colors.line)
            CallButton(
                text = stringResource(Res.string.call_line, AppConfig.REPORT_PHONE),
                onClick = onCall,
                icon = CzIcons.Phone,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = CazadoresDimens.screenPadding, vertical = 12.dp),
            )
        }
    }
}
