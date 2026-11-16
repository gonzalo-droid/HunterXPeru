package pe.cazadores.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.core.DateFormatter
import pe.cazadores.core.result.DataError
import pe.cazadores.resources.Res
import pe.cazadores.resources.error_network
import pe.cazadores.resources.error_not_found
import pe.cazadores.resources.error_server
import pe.cazadores.resources.error_timeout
import pe.cazadores.resources.error_title
import pe.cazadores.resources.loading_cd
import pe.cazadores.resources.offline_banner
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

@Composable
fun ErrorState(error: DataError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    MessageState(
        icon = if (error == DataError.Network) CzIcons.WifiOff else CzIcons.Alert,
        title = stringResource(Res.string.error_title),
        body = error.message(),
        modifier = modifier,
        action = { SecondaryButton(stringResource(Res.string.retry), onRetry, icon = CzIcons.Refresh) },
    )
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
