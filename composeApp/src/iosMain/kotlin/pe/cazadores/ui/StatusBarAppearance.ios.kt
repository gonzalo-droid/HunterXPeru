package pe.cazadores.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

/** Requiere `UIViewControllerBasedStatusBarAppearance = NO` en Info.plist. */
@Suppress("DEPRECATION")
@Composable
actual fun StatusBarAppearance(lightIcons: Boolean, restoreLightIcons: Boolean) {
    DisposableEffect(lightIcons, restoreLightIcons) {
        fun apply(light: Boolean) = UIApplication.sharedApplication.setStatusBarStyle(
            if (light) UIStatusBarStyleLightContent else UIStatusBarStyleDarkContent,
            animated = true,
        )
        apply(lightIcons)
        onDispose { apply(restoreLightIcons) }
    }
}
