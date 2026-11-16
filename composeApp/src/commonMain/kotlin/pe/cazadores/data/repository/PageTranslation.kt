package pe.cazadores.data.repository

import pe.cazadores.domain.model.SortOrder

/**
 * Verificado contra la API (docs/DECISIONES.md D1): la petición es 1-indexed y la respuesta
 * devuelve `number` 0-indexed. La UI trabaja 1-indexed.
 */
object PageTranslation {
    fun toRequestPage(uiPage: Int): Int {
        require(uiPage >= 1) { "Las páginas de la UI empiezan en 1" }
        return uiPage
    }

    fun toUiPage(responseNumber: Int): Int = responseNumber + 1

    fun sortField(sort: SortOrder): Pair<String, String> = when (sort) {
        SortOrder.RECENT -> "id" to "desc"
        SortOrder.REWARD -> "montoRecompensa" to "desc"
        SortOrder.REGION -> "departamento" to "asc"
    }
}
