package pe.cazadores.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sin anuncios (`ADS_ENABLED=false`, solo para capturas de tienda). Nunca pide consentimiento ni anuncios:
 * los banners no ocupan espacio y Compartir sigue sin el bonificado (D33).
 */
object NoAdsController : AdsController {
    override val canRequestAds: StateFlow<Boolean> = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(false)
    override val rewarded: StateFlow<RewardedState> = MutableStateFlow(RewardedState.IDLE)
    override fun start() = Unit
    override fun showPrivacyOptions() = Unit
    override fun preloadRewarded() = Unit
    override fun showRewarded(onResult: (RewardedOutcome) -> Unit) = onResult(RewardedOutcome.FAILED)
}
