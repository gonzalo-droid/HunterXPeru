package pe.cazadores.di

import androidx.room.Room
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.cazadores.core.config.AppEnvironment
import pe.cazadores.data.local.AppDatabase
import pe.cazadores.data.local.SETTINGS_FILE_NAME
import pe.cazadores.data.local.buildCazadoresDatabase
import pe.cazadores.data.local.createDataStore
import pe.cazadores.ads.AdsController
import pe.cazadores.ads.NoAdsController
import pe.cazadores.core.config.AppInfo
import pe.cazadores.ads.IosAdsController
import pe.cazadores.ads.DebugAdsSwitch
import pe.cazadores.ads.IosDebugAdsSwitch
import pe.cazadores.ads.adsActive
import pe.cazadores.domain.repository.NetworkMonitor
import pe.cazadores.platform.ExternalActions
import pe.cazadores.platform.IosExternalActions
import pe.cazadores.platform.IosNetworkMonitor
import pe.cazadores.platform.documentsPath
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual val platformModule: Module = module {
    single {
        val device = platform.UIKit.UIDevice.currentDevice
        AppEnvironment(
            platformName = "iOS",
            isDebug = Platform.isDebugBinary,
            deviceModel = device.model,
            osVersion = device.systemVersion,
        )
    }
    // iOS valida la cadena TLS de la API sin ayuda (verificado): no hace falta handleChallenge.
    single<HttpClientEngine> { Darwin.create() }
    single { Room.databaseBuilder<AppDatabase>(name = documentsPath(AppDatabase.FILE_NAME)).buildCazadoresDatabase() }
    single { createDataStore(documentsPath(SETTINGS_FILE_NAME)) }
    single<NetworkMonitor> { IosNetworkMonitor() }
    single<ExternalActions> { IosExternalActions() }
    single<DebugAdsSwitch> { IosDebugAdsSwitch() }
    single<AdsController> {
        val isDebug = get<AppEnvironment>().isDebug
        if (adsActive(AppInfo.ADS_ENABLED, isDebug, get<DebugAdsSwitch>().adsDisabled)) {
            IosAdsController(bridge = get(), isDebug = isDebug)
        } else {
            NoAdsController
        }
    }
}
