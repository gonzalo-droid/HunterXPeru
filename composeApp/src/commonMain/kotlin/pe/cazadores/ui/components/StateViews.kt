package pe.cazadores.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.core.DateFormatter
import pe.cazadores.core.config.AppConfig
import pe.cazadores.core.result.DataError
import pe.cazadores.resources.Res
import pe.cazadores.resources.call_line
import pe.cazadores.resources.error_network
import pe.cazadores.resources.error_not_found
import pe.cazadores.resources.error_server
import pe.cazadores.resources.error_timeout
import pe.cazadores.resources.error_title
import pe.cazadores.resources.loading_cd
import pe.cazadores.resources.offline_banner
import pe.cazadores.resources.offline_call_note
import pe.cazadores.resources.poster_no_signal
import pe.cazadores.resources.retry
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

@Composable
fun DataError.message(): String = stringResource(
    when (this) {
        DataError.Network -> Res.string.error_network
        DataError.Timeout -> Res.string.error_timeout
        DataError.NotFound -> Res.string.error_not_found
        is DataError.Server, is DataError.Unknown -> Res.string.error_server
    },
)

/** Mensaje centrado con ícono, texto y acción (vacíos y errores). */
@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Cz.colors.inkMuted, modifier = Modifier.size(40.dp))
        Text(title.uppercase(), style = Cz.type.title, color = Cz.colors.ink, textAlign = TextAlign.Center)
        Text(body, style = Cz.type.bodySmall, color = Cz.colors.inkMuted, textAlign = TextAlign.Center)
        action?.let {
            Spacer(Modifier.height(4.dp))
            it()
        }
    }
}

/**
 * Error o falta de red como cartel "SIN SEÑAL" sobre papel: motivo, Reintentar y la línea gratuita, que
 * funciona sin internet. Sin alturas fijas; la llamada va en `FlowRow` para que la letra 2× no parta palabras.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ErrorState(
    error: DataError,
    onRetry: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offline = error == DataError.Network
    val title = stringResource(if (offline) Res.string.poster_no_signal else Res.string.error_title)
    Column(
        modifier.fillMaxWidth().padding(vertical = 16.dp)
            .background(Cz.colors.paper, CazadoresShapes.cardLarge)
            .border(1.dp, Cz.colors.paperLine, CazadoresShapes.cardLarge)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (offline) CzIcons.WifiOff else CzIcons.Alert,
            contentDescription = null,
            tint = Cz.colors.onPaper,
            modifier = Modifier.size(32.dp),
        )
        Text(
            title.uppercase(),
            style = Cz.type.headline,
            color = Cz.colors.onPaper,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(error.message(), style = Cz.type.bodySmall, color = Cz.colors.onPaperMuted, textAlign = TextAlign.Center)
        PrimaryButton(stringResource(Res.string.retry), onRetry, icon = CzIcons.Refresh)
        HorizontalDivider(color = Cz.colors.paperLine)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.offline_call_note), style = Cz.type.bodySmall, color = Cz.colors.onPaper)
            TextAction(
                stringResource(Res.string.call_line, AppConfig.REPORT_PHONE),
                onCall,
                color = Cz.colors.onPaper,
            )
        }
    }
}

/** "Sin conexión · datos del 16/11 09:40". */
@Composable
fun OfflineBanner(cachedAtMillis: Long, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(Cz.colors.surfaceAlt, CazadoresShapes.card)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(CzIcons.WifiOff, contentDescription = null, tint = Cz.colors.ink, modifier = Modifier.size(20.dp))
        Text(
            stringResource(Res.string.offline_banner, DateFormatter.dateTime(cachedAtMillis)),
            style = Cz.type.bodySmall,
            color = Cz.colors.ink,
        )
    }
}

/** Bloque gris con pulso suave para los skeletons. */
@Composable
fun SkeletonBlock(modifier: Modifier) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(0.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse))
    Box(modifier.alpha(alpha).background(Cz.colors.line, CazadoresShapes.photo))
}

@Composable
fun CardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBlock(Modifier.size(72.dp, 92.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBlock(Modifier.width(180.dp).height(18.dp))
            SkeletonBlock(Modifier.width(120.dp).height(14.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBlock(Modifier.width(90.dp).height(22.dp))
        }
    }
}

@Composable
fun ListSkeleton(count: Int = 5, modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.loading_cd)
    Column(
        modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { repeat(count) { CardSkeleton() } }
}
