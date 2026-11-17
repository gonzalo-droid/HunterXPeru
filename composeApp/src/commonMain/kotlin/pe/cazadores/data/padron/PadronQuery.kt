package pe.cazadores.data.padron

import pe.cazadores.core.RewardClassCalculator
import pe.cazadores.core.text.normalizedForMatch
import pe.cazadores.domain.model.Catalogs
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.HomeStats
import pe.cazadores.domain.model.RewardPosition
import pe.cazadores.domain.model.SearchFilters
import pe.cazadores.domain.model.SearchPage
import pe.cazadores.domain.model.SortOrder

/**
 * Búsqueda, orden y posición sobre el padrón local. Imita a la API donde coincide (orden "recientes" =
 * id descendente, top 5 = mayores recompensas) y la mejora donde fallaba: el texto ignora tildes y
 * busca cada palabra por separado.
 *
 * El manifest trae nombres (no ids) de ubicación y delito: los filtros se traducen con [catalogs].
 */
internal object PadronQuery {

    fun search(padron: Padron, filters: SearchFilters, catalogs: Catalogs, page: Int, pageSize: Int): SearchPage {
        require(page >= 1) { "Las páginas de la UI empiezan en 1" }
        val matches = filter(padron.criminals, filters, catalogs).sortedWith(comparator(filters.sort))
        val totalPages = (matches.size + pageSize - 1) / pageSize
        val items = matches.drop((page - 1) * pageSize).take(pageSize)
        return SearchPage(
            items = items,
            page = page,
            totalElements = matches.size,
            totalPages = totalPages,
            isLast = page >= totalPages,
        )
    }

    fun topWanted(padron: Padron, limit: Int = 5): List<Criminal> =
        padron.criminals.sortedWith(comparator(SortOrder.REWARD)).take(limit)

    fun stats(padron: Padron): HomeStats =
        HomeStats(total = padron.criminals.size, maxReward = padron.criminals.maxOfOrNull { it.reward } ?: 0.0)

    /** `null` si la persona no está en el padrón o no tiene recompensa. */
    fun position(padron: Padron, hash: String): RewardPosition? {
        val reward = padron.byHash[hash]?.reward?.takeIf { it > 0.0 } ?: return null
        val higher = padron.criminals.count { it.reward > reward }
        val same = padron.criminals.count { it.reward == reward }
        return RewardPosition(rank = higher + 1, tiedWith = same - 1, total = padron.criminals.size)
    }

    fun filter(criminals: List<Criminal>, filters: SearchFilters, catalogs: Catalogs): List<Criminal> {
        val words = filters.query.normalizedForMatch().split(' ').filter { it.isNotBlank() }
        val departamento = filters.departamentoId?.let { id -> catalogs.departamentos.firstOrNull { it.id == id }?.name?.normalizedForMatch() }
        val provincia = filters.provinciaId?.takeIf { filters.departamentoId != null }?.let { id ->
            catalogs.provincias.firstOrNull { it.departamentoId == filters.departamentoId && it.id == id }?.name?.normalizedForMatch()
        }
        val delito = filters.delitoId?.let { id -> catalogs.delitos.firstOrNull { it.id == id }?.name?.normalizedForMatch() }
        // Id que el catálogo no conoce: ningún resultado, como respondería la API.
        if (filters.departamentoId != null && departamento == null) return emptyList()
        if (filters.delitoId != null && delito == null) return emptyList()

        return criminals.filter { c ->
            (words.isEmpty() || c.searchText().let { text -> words.all { it in text } }) &&
                (departamento == null || c.departamento?.trim()?.normalizedForMatch() == departamento) &&
                (provincia == null || c.provincia?.trim()?.normalizedForMatch() == provincia) &&
                (delito == null || c.crimes.any { it.trim().normalizedForMatch() == delito }) &&
                (filters.rewardClass == null || RewardClassCalculator.of(c.reward) == filters.rewardClass)
        }
    }

    private fun Criminal.searchText(): String =
        listOfNotNull(displayName, nombres, apellidoPaterno, apellidoMaterno).joinToString(" ").normalizedForMatch()

    private fun comparator(sort: SortOrder): Comparator<Criminal> = when (sort) {
        SortOrder.RECENT -> compareByDescending { it.id }
        SortOrder.REWARD -> compareByDescending<Criminal> { it.reward }.thenByDescending { it.id }
        SortOrder.REGION -> compareBy<Criminal, String?>(nullsLast()) { it.departamento?.normalizedForMatch() }
            .thenByDescending { it.id }
    }
}
