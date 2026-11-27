package pe.cazadores.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.android.inject
import pe.cazadores.App
import pe.cazadores.ads.AndroidAdsController
import pe.cazadores.ads.AndroidDebugAdsSwitch
import pe.cazadores.ads.DebugAdsSwitch
import pe.cazadores.ui.navigation.DeepLinks

class MainActivity : ComponentActivity() {
    private val ads: AndroidAdsController by inject()
    private val debugAds: DebugAdsSwitch by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // ADS-06: `disableAds` (Maestro `launchApp: arguments`, `adb shell am start --ez disableAds true`) antes de que
        // Koin cree el AdsController. En release `adsActive` lo ignora.
        intent?.extras?.get(AndroidDebugAdsSwitch.KEY)?.let { debugAds.adsDisabled = it.toString().toBoolean() }
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
