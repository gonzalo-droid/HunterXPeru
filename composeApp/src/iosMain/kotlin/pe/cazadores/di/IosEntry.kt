package pe.cazadores.di

import org.koin.dsl.module
import pe.cazadores.ads.IosAdsBridge

/** Llamado desde iOSApp.swift: `IosEntryKt.startKoin(adsBridge: AdsBridge())`. */
fun startKoin(adsBridge: IosAdsBridge) = doInitKoinIos(listOf(module { single<IosAdsBridge> { adsBridge } }))
