package pe.cazadores.core

import pe.cazadores.domain.model.RewardClass

/**
 * Rango de recompensa **estimado por la app** según el monto en soles. No es un dato oficial:
 * los cortes son propios y la UI debe aclararlo.
 */
object RewardClassCalculator {
    private const val S_MIN = 300_000.0
    private const val A_MIN = 100_000.0
    private const val B_MIN = 50_000.0

    /** Devuelve `null` si no hay recompensa (monto <= 0). Los cortes son inclusivos. */
    fun of(reward: Double): RewardClass? = when {
        reward <= 0.0 -> null
        reward >= S_MIN -> RewardClass.S
        reward >= A_MIN -> RewardClass.A
        reward >= B_MIN -> RewardClass.B
        else -> RewardClass.C
    }
}
