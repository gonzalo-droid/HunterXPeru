package pe.cazadores.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.cazadores.core.config.AppEnvironment
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual val platformModule: Module = module {
    single { AppEnvironment(platformName = "iOS", isDebug = Platform.isDebugBinary) }
    // iOS completa la cadena TLS por su cuenta (verificado): no hace falta handleChallenge.
    single<HttpClientEngine> { Darwin.create() }
}
