package pe.cazadores.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deep links `cazadores://criminal/{hash}`. Android (Intent) e iOS (onOpenURL) entregan la URL aquí;
 * AppNavHost la consume y abre la ficha. Funciona en arranque en frío y con la app abierta.
 */
object DeepLinks {
    private val _pendingHash = MutableStateFlow<String?>(null)
    val pendingHash: StateFlow<String?> = _pendingHash.asStateFlow()

    /** Devuelve `true` si la URL era un deep link válido de la app. */
    fun handle(url: String?): Boolean {
        val hash = parseCriminalHash(url) ?: return false
        _pendingHash.value = hash
        return true
    }

    fun consume() {
        _pendingHash.value = null
    }

    /** `cazadores://criminal/ABC123` → `ABC123`. Solo letras y números (los hashes de la API son hex). */
    fun parseCriminalHash(url: String?): String? {
        if (url == null) return null
        val prefix = "$DEEP_LINK_SCHEME://criminal/"
        if (!url.startsWith(prefix, ignoreCase = true)) return null
        val hash = url.substring(prefix.length).substringBefore('?').substringBefore('#').trimEnd('/')
        return hash.takeIf { it.isNotEmpty() && it.length <= 64 && it.all(Char::isLetterOrDigit) }
    }
}
