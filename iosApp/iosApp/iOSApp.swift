import SwiftUI
import ComposeApp
import FirebaseCore
import FirebaseCrashlytics

@main
struct iOSApp: App {
    init() {
        // D38: Crashlytics solo en Release y solo si el build trae GoogleService-Info.plist (no se versiona).
        #if !DEBUG
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }
        #endif
        #if CRASHLYTICS_TEST_CRASH
        // Solo existe si se compila con este flag a mano (ver docs/CRASHLYTICS.md).
        DispatchQueue.main.asyncAfter(deadline: .now() + 5) { fatalError("Crash de prueba de Crashlytics") }
        #endif
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
