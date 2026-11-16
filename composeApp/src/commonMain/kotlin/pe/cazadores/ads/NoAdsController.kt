package pe.cazadores.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Sin anuncios (tests y previews). */
class NoAdsController : AdsController {
    override val canRequestAds: StateFlow<Boolean> = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(false)
    override fun start() = Unit
    override fun showPrivacyOptions() = Unit
}
