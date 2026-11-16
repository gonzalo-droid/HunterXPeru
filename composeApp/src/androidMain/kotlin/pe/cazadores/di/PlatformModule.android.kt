package pe.cazadores.di

import android.content.pm.ApplicationInfo
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.cazadores.core.config.AppEnvironment

actual val platformModule: Module = module {
    single {
        val flags = androidContext().applicationInfo.flags
        AppEnvironment(platformName = "Android", isDebug = flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
    }
    // La confianza TLS para la API la define res/xml/network_security_config.xml (androidApp).
    single<HttpClientEngine> { OkHttp.create() }
}
