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

    private fun apply(state: AdsConsentState) {
        _privacyOptionsRequired.value = state.privacyOptionsRequired
        _canRequestAds.value = state.canRequestAds
    }
}
