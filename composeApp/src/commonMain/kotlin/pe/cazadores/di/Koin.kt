package pe.cazadores.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.cazadores.core.config.AppEnvironment
import pe.cazadores.data.remote.RecompensasApi
import pe.cazadores.data.remote.createHttpClient

/** Provee: AppEnvironment, HttpClientEngine y lo específico de cada plataforma. */
expect val platformModule: Module

val networkModule = module {
    single {
        val env = get<AppEnvironment>()
        createHttpClient(engine = get(), userAgent = env.userAgent, debug = env.isDebug)
    }
    single { RecompensasApi(get()) }
}

fun appModules(): List<Module> = listOf(platformModule, networkModule)

/** Arranque único de Koin. Android lo llama desde `Application`; iOS desde `iOSApp.swift`. */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModules())
    }
}

/** Atajo para Swift, que no ve los parámetros por defecto. */
fun doInitKoinIos() = initKoin()
