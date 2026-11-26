package pe.cazadores.di

import android.content.pm.ApplicationInfo
import androidx.room.Room
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
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
import pe.cazadores.ads.AndroidAdUnits
import pe.cazadores.ads.AndroidAdsController
import pe.cazadores.domain.repository.NetworkMonitor
import pe.cazadores.platform.AndroidExternalActions
import pe.cazadores.platform.ExternalActions
import pe.cazadores.platform.AndroidNetworkMonitor

actual val platformModule: Module = module {
    single {
        val flags = androidContext().applicationInfo.flags
        AppEnvironment(
            platformName = "Android",
            isDebug = flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            osVersion = android.os.Build.VERSION.RELEASE,
        )
    }
    // La confianza TLS para la API la define res/xml/network_security_config.xml (androidApp).
    single<HttpClientEngine> { OkHttp.create() }
    single {
        val context = androidContext()
        Room.databaseBuilder<AppDatabase>(context, context.getDatabasePath(AppDatabase.FILE_NAME).absolutePath)
            .buildCazadoresDatabase()
    }
    single { createDataStore(androidContext().filesDir.resolve(SETTINGS_FILE_NAME).absolutePath) }
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
    single<ExternalActions> { AndroidExternalActions(androidContext()) }
    single {
        AndroidAdsController(
            isDebug = get<AppEnvironment>().isDebug,
            scope = get(APP_SCOPE),
            rewardedUnitId = get<AndroidAdUnits>().rewarded,
        )
    }
    single<AdsController> { if (AppInfo.ADS_ENABLED) get<AndroidAdsController>() else NoAdsController }
}
