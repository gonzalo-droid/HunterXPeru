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
            // Debug: IDs de prueba de Google. Release: ADMOB_*_ANDROID de local.properties (ver build.gradle.kts).
            modules(
                module {
                    single {
                        AndroidAdUnits(
                            banner = getString(R.string.admob_banner_id),
                            rewarded = getString(R.string.admob_rewarded_id),
                        )
                    }
                },
            )
        }
    }
}
