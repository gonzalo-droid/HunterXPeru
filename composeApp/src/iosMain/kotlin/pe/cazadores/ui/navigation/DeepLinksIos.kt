package pe.cazadores.ui.navigation

/** Llamado desde SwiftUI `.onOpenURL`: `DeepLinksIosKt.handleDeepLink(url: url.absoluteString)`. */
fun handleDeepLink(url: String): Boolean = DeepLinks.handle(url)
