package pe.cazadores.core

import kotlin.math.roundToLong

/** `S/ 50 000` con espacio fino como separador de miles (formato de la API y del programa). */
object RewardFormatter {
    private const val THOUSANDS_SEPARATOR = ' '

    fun format(amount: Double): String? {
        if (amount <= 0.0) return null
        val digits = amount.roundToLong().toString()
        val grouped = digits.reversed().chunked(3).joinToString(THOUSANDS_SEPARATOR.toString()).reversed()
        return "S/$THOUSANDS_SEPARATOR$grouped"
    }

    /** Para lectores de pantalla: "50000 soles". */
    fun spoken(amount: Double): String = "${amount.roundToLong()} soles"
}
