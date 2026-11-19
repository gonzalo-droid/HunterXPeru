package pe.cazadores.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UMP → MobileAds.initialize. Android no tiene ATT. La Activity la registra MainActivity
 * porque el formulario de consentimiento necesita una ventana visible.
 */
class AndroidAdsController(
    private val isDebug: Boolean,
    private val scope: CoroutineScope,
    private val rewardedUnitId: String,
) : AdsController {
    private val _rewarded = MutableStateFlow(RewardedState.IDLE)
    override val rewarded: StateFlow<RewardedState> = _rewarded.asStateFlow()
    private var rewardedAd: RewardedAd? = null

    // load, show y sus callbacks corren en el hilo principal.
    override fun preloadRewarded() {
        val activity = activityRef.get() ?: return
        if (!_canRequestAds.value || _rewarded.value != RewardedState.IDLE) return
        _rewarded.value = RewardedState.LOADING
        RewardedAd.load(activity, rewardedUnitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                _rewarded.value = RewardedState.READY
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                _rewarded.value = RewardedState.IDLE
            }
        })
    }

    override fun showRewarded(onResult: (RewardedOutcome) -> Unit) {
        val ad = rewardedAd
        val activity = activityRef.get()
        if (ad == null || activity == null) return onResult(RewardedOutcome.FAILED)
        rewardedAd = null
        _rewarded.value = RewardedState.IDLE
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() =
                onResult(if (earned) RewardedOutcome.EARNED else RewardedOutcome.DISMISSED)

            override fun onAdFailedToShowFullScreenContent(error: AdError) = onResult(RewardedOutcome.FAILED)
        }
        ad.show(activity) { earned = true }
    }

    private val _canRequestAds = MutableStateFlow(false)
    override val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var activityRef = WeakReference<Activity>(null)
    private val started = AtomicBoolean(false)
    private val sdkInitialized = AtomicBoolean(false)

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    override fun start() {
        val activity = activityRef.get() ?: return
        if (!started.compareAndSet(false, true)) return
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().apply {
            // En debug se fuerza la geografía EEE para ver el formulario desde Perú (los emuladores son dispositivos de prueba).
            if (isDebug) {
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .build(),
                )
            }
        }.build()

        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    onConsentResolved(activity, consent)
                }
            },
            { _ ->
                // Sin respuesta de UMP: se usa el consentimiento guardado de una sesión anterior, si existe.
                onConsentResolved(activity, consent)
            },
        )
    }

    override fun showPrivacyOptions() {
        val activity = activityRef.get() ?: return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
            onConsentResolved(activity, UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    private fun onConsentResolved(activity: Activity, consent: ConsentInformation) {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (!consent.canRequestAds()) return
        if (sdkInitialized.compareAndSet(false, true)) {
            val appContext = activity.applicationContext
            // initialize hace trabajo de disco: fuera del hilo principal.
            scope.launch(Dispatchers.IO) {
                MobileAds.initialize(appContext) { _canRequestAds.value = true }
            }
        } else {
            _canRequestAds.value = true
        }
    }
}
