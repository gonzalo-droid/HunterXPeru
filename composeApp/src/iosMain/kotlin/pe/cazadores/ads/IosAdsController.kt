package pe.cazadores.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IosAdsController(
    private val bridge: IosAdsBridge,
    private val isDebug: Boolean,
) : AdsController {
    private val _canRequestAds = MutableStateFlow(false)
    override val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var started = false

    override fun start() {
        if (started) return
        started = true
        bridge.requestConsentAndStart(debugGeographyEea = isDebug, completion = ::apply)
    }

    override fun showPrivacyOptions() = bridge.showPrivacyOptions(::apply)

    private val _rewarded = MutableStateFlow(RewardedState.IDLE)
    override val rewarded: StateFlow<RewardedState> = _rewarded.asStateFlow()

    override fun preloadRewarded() {
        if (!_canRequestAds.value || _rewarded.value != RewardedState.IDLE) return
        _rewarded.value = RewardedState.LOADING
        bridge.loadRewarded(
            onLoaded = { _rewarded.value = RewardedState.READY },
            onFailed = { _rewarded.value = RewardedState.IDLE },
        )
    }

    override fun showRewarded(onResult: (RewardedOutcome) -> Unit) {
        if (_rewarded.value != RewardedState.READY) return onResult(RewardedOutcome.FAILED)
        _rewarded.value = RewardedState.IDLE
        bridge.showRewarded(onResult)
    }

    private fun apply(state: AdsConsentState) {
        _privacyOptionsRequired.value = state.privacyOptionsRequired
        _canRequestAds.value = state.canRequestAds
    }
}
