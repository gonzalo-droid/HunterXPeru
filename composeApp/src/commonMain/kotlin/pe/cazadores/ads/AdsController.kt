package pe.cazadores.ads

import kotlinx.coroutines.flow.StateFlow

/**
 * Consentimiento y SDK de anuncios. Orden obligatorio (docs/DECISIONES.md D7):
 * UMP → (iOS) ATT → MobileAds.initialize → recién entonces se piden anuncios.
 */
interface AdsController {
    /** `true` cuando ya se puede pedir anuncios (consentimiento resuelto y SDK inicializado). */
    val canRequestAds: StateFlow<Boolean>

    /** El formulario de privacidad debe poder reabrirse desde Ajustes (EEE/Reino Unido/Suiza). */
    val privacyOptionsRequired: StateFlow<Boolean>

    /** Idempotente. Se llama al mostrar la primera pantalla real (después del onboarding). */
    fun start()

    fun showPrivacyOptions()
}
