package pe.cazadores.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import org.koin.compose.koinInject

@Composable
internal actual fun PlatformBanner(placement: AdPlacement, modifier: Modifier) {
    val bridge = koinInject<IosAdsBridge>()
    BoxWithConstraints(modifier) {
        val width = maxWidth.value.toDouble()
        val height = remember(width) { bridge.bannerHeight(width) }
        UIKitView(
            factory = { bridge.makeBannerView(width) },
            modifier = Modifier.width(maxWidth).height(height.dp),
        )
    }
}
