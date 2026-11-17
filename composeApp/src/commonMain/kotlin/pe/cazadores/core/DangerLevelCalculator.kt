package pe.cazadores.core

import pe.cazadores.core.text.normalizedForMatch
import pe.cazadores.domain.model.DangerLevel

/**
 * Resultado de estimar la peligrosidad: el nivel final, el delito que lo determina y si algún
 * delito coincidió con las palabras clave (si no, el nivel es el de por defecto).
 */
data class DangerAssessment(
    val level: DangerLevel,
    val headlineCrime: String?,
    val matched: Boolean,
)

/**
 * Peligrosidad **estimada por la app** según palabras clave del delito. No es un dato oficial:
 * la UI siempre lo aclara.
 */
object DangerLevelCalculator {
    private val extremo = listOf("TERRORISMO", "HOMICIDIO CALIFICADO", "ASESINATO", "SICARIATO", "FEMINICIDIO")
    private val muyAlto = listOf("ROBO AGRAVADO", "SECUESTRO", "TRATA DE PERSONAS", "VIOLACION SEXUAL", "EXTORSION")

    fun of(crimes: List<String>): DangerLevel = assess(crimes).level

    /**
     * Delito determinante: el primero, en el orden de la API, cuyo nivel individual iguala el
     * nivel final. Sin coincidencias, el primer delito; sin delitos, `null`.
     */
    fun assess(crimes: List<String>): DangerAssessment {
        val levels = crimes.map { levelOf(it.normalizedForMatch()) }
        val level = when {
            DangerLevel.EXTREMO in levels -> DangerLevel.EXTREMO
            DangerLevel.MUY_ALTO in levels -> DangerLevel.MUY_ALTO
            else -> DangerLevel.ALTO
        }
        val matched = levels.any { it != null }
        val headline = if (matched) crimes[levels.indexOfFirst { it == level }] else crimes.firstOrNull()
        return DangerAssessment(level, headline, matched)
    }

    /** Nivel de un delito ya normalizado, o `null` si no coincide con ninguna palabra clave. */
    private fun levelOf(normalized: String): DangerLevel? = when {
        extremo.any { it in normalized } -> DangerLevel.EXTREMO
        muyAlto.any { it in normalized } -> DangerLevel.MUY_ALTO
        else -> null
    }
}
