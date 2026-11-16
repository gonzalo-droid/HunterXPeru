package pe.cazadores.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Módulos de cada plataforma (engine de Ktor, base de datos, launcher…). */
expect val platformModule: Module

val appModule = module { }

/** Arranque único de Koin. Android lo llama desde `Application`; iOS desde `iOSApp.swift`. */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(platformModule, appModule)
    }
}

/** Atajo para Swift, que no ve los parámetros por defecto. */
fun doInitKoinIos() = initKoin()
