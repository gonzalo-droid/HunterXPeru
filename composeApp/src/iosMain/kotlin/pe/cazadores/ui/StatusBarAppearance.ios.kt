package pe.cazadores.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
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

/** En iOS la hoja comparte la ventana de la app y nunca llega a la barra de estado: nada que ajustar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun sheetWindowProperties(sheetLightIcons: Boolean): ModalBottomSheetProperties =
    ModalBottomSheetProperties()
