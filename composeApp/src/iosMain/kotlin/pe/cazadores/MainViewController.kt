package pe.cazadores

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Punto de entrada de iOS: lo monta ContentView.swift. Sin desplazar la vista al enfocar un campo: los campos
 * que el teclado podría tapar viven en hojas que ya reciben su inset, y el desplazamiento extra las descuadraba.
 */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController =
    ComposeUIViewController(configure = { onFocusBehavior = OnFocusBehavior.DoNothing }) { App() }
