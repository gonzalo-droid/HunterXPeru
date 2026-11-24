package pe.cazadores.app

import android.app.Application
import android.os.Handler
import android.os.Looper
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
        // D38: solo existe en un release compilado con -PcrashlyticsTestCrash=true (ver docs/CRASHLYTICS.md).
        if (resources.getBoolean(R.bool.crashlytics_test_crash)) {
            Handler(Looper.getMainLooper()).postDelayed({ throw RuntimeException("Crash de prueba de Crashlytics") }, 5_000)
        }
    }
}
