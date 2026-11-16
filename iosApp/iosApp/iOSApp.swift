import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // El consentimiento no se pide aquí: Kotlin lo dispara al mostrar Inicio (ya hay una vista en pantalla).
        IosEntryKt.startKoin(adsBridge: AdsBridge())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // cazadores://criminal/{hash} → Ficha
                .onOpenURL { url in _ = DeepLinksIosKt.handleDeepLink(url: url.absoluteString) }
        }
    }
}
