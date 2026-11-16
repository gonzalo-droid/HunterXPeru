package pe.cazadores.core.text

private val accents = mapOf(
    'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n',
    'Á' to 'A', 'É' to 'E', 'Í' to 'I', 'Ó' to 'O', 'Ú' to 'U', 'Ü' to 'U', 'Ñ' to 'N',
)

/** Mayúsculas y sin tildes, para comparar textos de la API ("VIOLACIÓN" == "violacion"). */
fun String.normalizedForMatch(): String = map { accents[it] ?: it }.joinToString("").uppercase()

/** "tráfico ilícito de drogas" → "Tráfico ilícito de drogas" (la API manda todo en mayúsculas). */
fun String.sentenceCase(): String {
    val lower = trim().lowercase()
    return lower.replaceFirstChar { it.uppercaseChar() }
}

/** "CORONEL PORTILLO" → "Coronel Portillo" (lugares y nombres propios). */
fun String.titleCase(): String = trim().lowercase().split(' ').filter { it.isNotEmpty() }
    .joinToString(" ") { word -> if (word in LOWERCASE_WORDS) word else word.replaceFirstChar { it.uppercaseChar() } }
    .replaceFirstChar { it.uppercaseChar() }

private val LOWERCASE_WORDS = setOf("de", "del", "la", "las", "los", "y")
