package pe.cazadores.ui.components

import androidx.compose.runtime.Composable
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

/** Requiere `UIViewControllerBasedStatusBarAppearance = NO` en Info.plist. */
@Suppress("DEPRECATION")
@Composable
internal actual fun rememberStatusBarApplier(): (lightIcons: Boolean) -> Unit = { lightIcons ->
    UIApplication.sharedApplication.setStatusBarStyle(
        if (lightIcons) UIStatusBarStyleLightContent else UIStatusBarStyleDarkContent,
        animated = true,
    )
}
