package pe.cazadores.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.android.inject
import pe.cazadores.App
import pe.cazadores.ads.AndroidAdsController

class MainActivity : ComponentActivity() {
    private val ads: AndroidAdsController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // El formulario de consentimiento (UMP) necesita una Activity visible.
        ads.attach(this)
        setContent { App() }
    }
}
