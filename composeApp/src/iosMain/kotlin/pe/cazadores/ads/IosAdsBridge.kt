package pe.cazadores.ads

import platform.UIKit.UIView

/** Resultado del flujo de consentimiento, devuelto por Swift. */
data class AdsConsentState(val canRequestAds: Boolean, val privacyOptionsRequired: Boolean)

/**
 * Lo implementa Swift (iosApp/iosApp/AdsBridge.swift) con GoogleMobileAds, UserMessagingPlatform
 * y AppTrackingTransparency, que llegan por SPM. Se inyecta en `doInitKoinIos(adsBridge:)`.
 */
interface IosAdsBridge {
    /** UMP → ATT → MobileAds.start, en ese orden. */
    fun requestConsentAndStart(debugGeographyEea: Boolean, completion: (AdsConsentState) -> Unit)
    fun showPrivacyOptions(completion: (AdsConsentState) -> Unit)
    fun makeBannerView(widthPoints: Double): UIView
    fun bannerHeight(widthPoints: Double): Double
}
