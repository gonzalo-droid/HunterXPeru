package pe.cazadores.ads

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Estado del anuncio bonificado precargado (uno a la vez). */
enum class RewardedState { IDLE, LOADING, READY }

/** Cómo terminó el anuncio bonificado. */
enum class RewardedOutcome { EARNED, DISMISSED, FAILED }

/** D33: compartir nunca depende del anuncio; solo cerrarlo sin ganar la recompensa lo cancela. */
fun RewardedOutcome.allowsShare() = this != RewardedOutcome.DISMISSED

/** Cuánto se espera a un anuncio que todavía carga antes de compartir sin él. */
const val REWARDED_WAIT_MS = 4_000L

/**
 * Muestra un anuncio bonificado antes de compartir. Sin consentimiento, sin anuncio a tiempo o si falla,
 * devuelve [RewardedOutcome.FAILED] (se comparte igual). Al terminar deja precargado el siguiente.
 */
suspend fun AdsController.showRewardedForShare(waitMs: Long = REWARDED_WAIT_MS): RewardedOutcome {
    if (!canRequestAds.value) return RewardedOutcome.FAILED
    preloadRewarded()
    val state = withTimeoutOrNull(waitMs) { rewarded.first { it != RewardedState.LOADING } }
    if (state != RewardedState.READY) return RewardedOutcome.FAILED
    val outcome = suspendCancellableCoroutine { cont -> showRewarded { if (cont.isActive) cont.resume(it) } }
    preloadRewarded()
    return outcome
}
