package pe.cazadores.data.padron

import pe.cazadores.domain.model.Catalogs
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.Delito
import pe.cazadores.domain.model.Departamento
import pe.cazadores.domain.model.Provincia
import pe.cazadores.domain.model.RewardClass
import pe.cazadores.domain.model.SearchFilters
import pe.cazadores.domain.model.SortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PadronQueryTest {
    private fun c(id: Int, name: String, reward: Double, dep: String? = "lima", prov: String? = "lima", crimes: List<String> = listOf("extorsion")) =
        Criminal(id = id, hash = "h$id", displayName = name, reward = reward, crimes = crimes, departamento = dep, provincia = prov)

    private val padron = Padron(
        version = 1,
        generatedAtMillis = 0,
        criminals = listOf(
            c(1, "PÉREZ ROJAS, JUAN", 500_000.0),
            c(2, "QUISPE MAMANI, ROSA", 100_000.0, dep = "cusco", prov = "cusco", crimes = listOf("sicariato")),
            c(3, "ÑAUPA LÓPEZ, ANA", 100_000.0, dep = "áncash", prov = "huaraz"),
            c(4, "GÓMEZ, LUIS", 20_000.0, dep = null, prov = null),
        ),
    )
    private val catalogs = Catalogs(
        departamentos = listOf(Departamento("15", "LIMA"), Departamento("02", "ANCASH")),
        provincias = listOf(Provincia("02", "0201", "HUARAZ")),
        delitos = listOf(Delito(7, "SICARIATO")),
    )

    private fun search(filters: SearchFilters, page: Int = 1, size: Int = 20) = PadronQuery.search(padron, filters, catalogs, page, size)

    @Test fun textIgnoresAccentsAndWordOrder() {
        assertEquals(listOf(1), search(SearchFilters(query = "juan perez")).items.map { it.id })
        assertEquals(listOf(3), search(SearchFilters(query = "naupa")).items.map { it.id })
    }

    @Test fun catalogIdsMatchNamesFromManifest() {
        assertEquals(listOf(3), search(SearchFilters(departamentoId = "02", provinciaId = "0201")).items.map { it.id })
        assertEquals(listOf(2), search(SearchFilters(delitoId = 7)).items.map { it.id })
        assertTrue(search(SearchFilters(departamentoId = "99")).items.isEmpty(), "Id desconocido: sin resultados, como la API")
    }

    @Test fun rewardClassFilter() {
        assertEquals(listOf(1), search(SearchFilters(rewardClass = RewardClass.S)).items.map { it.id })
        assertEquals(listOf(3, 2), search(SearchFilters(rewardClass = RewardClass.A)).items.map { it.id })
    }

    @Test fun sortingAndPaging() {
        assertEquals(listOf(1, 3, 2, 4), search(SearchFilters(sort = SortOrder.REWARD)).items.map { it.id })
        assertEquals(listOf(3, 2, 1, 4), search(SearchFilters(sort = SortOrder.REGION)).items.map { it.id }, "Sin departamento al final")
        val page2 = search(SearchFilters(), page = 2, size = 3)
        assertEquals(listOf(1), page2.items.map { it.id })
        assertEquals(4, page2.totalElements)
        assertEquals(2, page2.totalPages)
        assertTrue(page2.isLast)
    }

    @Test fun positionCountsTies() {
        val top = PadronQuery.position(padron, "h1")!!
        assertEquals(1, top.rank)
        assertEquals(0, top.tiedWith)
        assertEquals(25, top.topPercent)
        val tied = PadronQuery.position(padron, "h2")!!
        assertEquals(2, tied.rank)
        assertEquals(1, tied.tiedWith)
        assertNull(PadronQuery.position(padron, "nadie"))
    }

    @Test fun statsAndTop() {
        assertEquals(4, PadronQuery.stats(padron).total)
        assertEquals(500_000.0, PadronQuery.stats(padron).maxReward)
        assertEquals(listOf(1, 3), PadronQuery.topWanted(padron, limit = 2).map { it.id })
    }
}
