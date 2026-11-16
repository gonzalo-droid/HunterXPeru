package pe.cazadores.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.android.inject
import pe.cazadores.App
import pe.cazadores.ads.AndroidAdsController
import pe.cazadores.ui.navigation.DeepLinks

class MainActivity : ComponentActivity() {
    private val ads: AndroidAdsController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // El formulario de consentimiento (UMP) necesita una Activity visible.
        ads.attach(this)
        // Solo en el primer arranque: al recrear la Activity el deep link ya se consumió.
        if (savedInstanceState == null) DeepLinks.handle(intent?.dataString)
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        DeepLinks.handle(intent.dataString)
    }
}
