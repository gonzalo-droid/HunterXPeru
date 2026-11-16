package pe.cazadores.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import pe.cazadores.di.initKoin

class CazadoresApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@CazadoresApplication) }
    }
}
