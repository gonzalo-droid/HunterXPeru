package pe.cazadores.di

import org.koin.dsl.module
import pe.cazadores.ads.IosAdsBridge
import platform.Foundation.NSLog
import kotlin.experimental.ExperimentalNativeApi

/** Llamado desde iOSApp.swift: `IosEntryKt.startKoin(adsBridge: AdsBridge())`. */
@OptIn(ExperimentalNativeApi::class)
fun startKoin(adsBridge: IosAdsBridge) {
    // Kotlin/Native solo escribe el error en stderr, que no queda en el log del sistema.
    // Así el mensaje y la traza aparecen en Console.app / `log show` antes del abort.
    setUnhandledExceptionHook { error ->
        NSLog("CazadoresPeru: excepción Kotlin no capturada: %@", error.stackTraceToString())
        terminateWithUnhandledException(error)
    }
    doInitKoinIos(listOf(module { single<IosAdsBridge> { adsBridge } }))
}
