package pe.cazadores.core

import pe.cazadores.core.text.normalizedForMatch
import pe.cazadores.domain.model.DangerLevel

/**
 * Peligrosidad **estimada por la app** según palabras clave del delito. No es un dato oficial:
 * la UI siempre lo aclara.
 */
object DangerLevelCalculator {
    private val extremo = listOf("TERRORISMO", "HOMICIDIO CALIFICADO", "ASESINATO", "SICARIATO", "FEMINICIDIO")
    private val muyAlto = listOf("ROBO AGRAVADO", "SECUESTRO", "TRATA DE PERSONAS", "VIOLACION SEXUAL", "EXTORSION")

    fun of(crimes: List<String>): DangerLevel {
        val normalized = crimes.map { it.normalizedForMatch() }
        return when {
            normalized.any { crime -> extremo.any { it in crime } } -> DangerLevel.EXTREMO
            normalized.any { crime -> muyAlto.any { it in crime } } -> DangerLevel.MUY_ALTO
            else -> DangerLevel.ALTO
        }
    }
}
