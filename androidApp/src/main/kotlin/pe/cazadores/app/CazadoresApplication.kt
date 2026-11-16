package pe.cazadores.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pe.cazadores.ads.AndroidAdUnits
import pe.cazadores.di.initKoin

class CazadoresApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CazadoresApplication)
            // Debug: ID de prueba de Google. Release: ADMOB_BANNER_ANDROID de local.properties (ver build.gradle.kts).
            modules(module { single { AndroidAdUnits(banner = getString(R.string.admob_banner_id)) } })
        }
    }
}
