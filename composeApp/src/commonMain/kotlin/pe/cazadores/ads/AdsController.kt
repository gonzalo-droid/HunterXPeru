package pe.cazadores.ads

import kotlinx.coroutines.flow.StateFlow

/**
 * Consentimiento y SDK de anuncios. Orden: UMP → (iOS) ATT → MobileAds.initialize → anuncios.
 * Implementado por plataforma en la fase de AdMob.
 */
interface AdsController {
    /** `true` cuando ya se puede pedir anuncios (consentimiento resuelto y SDK inicializado). */
    val canRequestAds: StateFlow<Boolean>

    /** El formulario de privacidad debe poder reabrirse desde Ajustes (EEE/Reino Unido). */
    val privacyOptionsRequired: StateFlow<Boolean>

    fun showPrivacyOptions()
}
