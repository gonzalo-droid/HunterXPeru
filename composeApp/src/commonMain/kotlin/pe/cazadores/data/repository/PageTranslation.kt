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

    /**
     * Verificado contra producción (2026-11-18): `nombreCompleto` y `montoRecompensa` aceptan `asc` y `desc`.
     * `nombreCompleto` es el mismo texto que muestran las tarjetas de la API; `apellidoPaterno` o `nombres`
     * responden HTTP 500 ("No property ... found for type RequisitoriadosView").
     */
    fun sortField(sort: SortOrder): Pair<String, String> = when (sort) {
        SortOrder.NAME -> "nombreCompleto" to "asc"
        SortOrder.REWARD -> "montoRecompensa" to "desc"
        SortOrder.REWARD_ASC -> "montoRecompensa" to "asc"
    }
}
