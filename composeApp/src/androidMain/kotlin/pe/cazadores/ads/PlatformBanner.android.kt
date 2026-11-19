package pe.cazadores.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import org.koin.compose.koinInject

/** IDs de bloque de anuncios. Los provee androidApp (resValue por build type). */
data class AndroidAdUnits(val banner: String, val rewarded: String)

@Composable
internal actual fun PlatformBanner(placement: AdPlacement, modifier: Modifier) {
    val units = koinInject<AndroidAdUnits>()
    val context = LocalContext.current
    BoxWithConstraints(modifier) {
        val widthDp = maxWidth.value.toInt()
        val size = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        AndroidView(
            modifier = Modifier.height(size.height.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(size)
                    adUnitId = units.banner
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
