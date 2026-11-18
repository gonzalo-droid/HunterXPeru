package pe.cazadores.ui.screens.legal

import io.ktor.http.Url
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import pe.cazadores.core.config.AppConfig
import pe.cazadores.resources.Res
import pe.cazadores.resources.settings_privacy_policy
import pe.cazadores.resources.settings_terms

/**
 * Páginas legales que se abren dentro de la app. Es un enum y no una URL para que la ruta (y cualquier
 * deep link futuro) no pueda cargar direcciones arbitrarias en la vista web.
 */
@Serializable
enum class LegalPage {
    PRIVACY,
    TERMS,
    ;

    val url: String
        get() = when (this) {
            PRIVACY -> AppConfig.PRIVACY_POLICY_URL
            TERMS -> AppConfig.TERMS_URL
        }

    val title: StringResource
        get() = when (this) {
            PRIVACY -> Res.string.settings_privacy_policy
            TERMS -> Res.string.settings_terms
        }
}

/** Qué hacer con una navegación que la página pide dentro de la vista web. */
internal enum class LinkDecision { IN_APP, EXTERNAL, BLOCK }

internal object WebLinkPolicy {
    private val EXTERNAL_SCHEMES = setOf("http", "https", "mailto", "tel")

    /**
     * Mismo host que la página legal y por HTTPS: sigue en la vista web. Otros hosts, correo y teléfono:
     * a la app del sistema. Cualquier otro esquema (javascript:, intent:, file:…) se ignora.
     */
    fun decide(target: String, home: String): LinkDecision {
        val scheme = target.substringBefore(':', missingDelimiterValue = "").lowercase()
        if (scheme !in EXTERNAL_SCHEMES) return LinkDecision.BLOCK
        if (scheme != "https") return LinkDecision.EXTERNAL
        val targetHost = runCatching { Url(target).host }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?: return LinkDecision.BLOCK
        val homeHost = runCatching { Url(home).host }.getOrNull()
        return if (targetHost.equals(homeHost, ignoreCase = true)) LinkDecision.IN_APP else LinkDecision.EXTERNAL
    }
}
