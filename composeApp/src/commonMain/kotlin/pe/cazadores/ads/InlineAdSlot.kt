package pe.cazadores.ads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pe.cazadores.resources.Res
import pe.cazadores.resources.ad_label
import pe.cazadores.ui.theme.Cz

/** Banner adaptativo de la plataforma (AdView en Android, GADBannerView desde Swift en iOS). */
@Composable
internal expect fun PlatformBanner(placement: AdPlacement, modifier: Modifier)

/**
 * Banner marcado "Anuncio". No ocupa espacio hasta que el consentimiento esté resuelto
 * y el SDK inicializado: nunca se pide un anuncio antes.
 */
@Composable
fun AdBanner(placement: AdPlacement, modifier: Modifier = Modifier, ads: AdsController = koinInject()) {
    val ready by ads.canRequestAds.collectAsStateWithLifecycle()
    if (!ready) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(Res.string.ad_label).uppercase(), style = Cz.type.monoSmall, color = Cz.colors.inkMuted)
        PlatformBanner(placement, Modifier.fillMaxWidth())
    }
}

/** Anuncio dentro de listas (cada [AD_EVERY_N_ITEMS] ítems). */
@Composable
fun InlineAdSlot(placement: AdPlacement) = AdBanner(placement, Modifier.fillMaxWidth().padding(vertical = 4.dp))
