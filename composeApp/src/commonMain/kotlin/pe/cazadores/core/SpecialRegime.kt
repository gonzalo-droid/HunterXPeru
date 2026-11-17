package pe.cazadores.core

import pe.cazadores.core.text.normalizedForMatch

/**
 * Régimen especial de recompensas: aplica cuando algún delito es extorsión o sicariato.
 * Coincidencia por palabra clave normalizada (sin tildes ni mayúsculas).
 */
object SpecialRegime {
    private val keywords = listOf("EXTORSION", "SICARIATO")

    fun applies(crimes: List<String>): Boolean =
        crimes.map { it.normalizedForMatch() }.any { crime -> keywords.any { it in crime } }
}
