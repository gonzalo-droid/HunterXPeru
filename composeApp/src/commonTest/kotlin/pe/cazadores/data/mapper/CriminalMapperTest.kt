package pe.cazadores.data.mapper

import pe.cazadores.data.remote.dto.CriminalDetailDto
import pe.cazadores.data.remote.dto.CriminalSummaryDto
import pe.cazadores.domain.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CriminalMapperTest {
    @Test fun displayNameFromSurnames() {
        assertEquals("AGUSTIN HUAYTA, NEMESIO", buildDisplayName("agustin", "huayta", "nemesio", null))
    }

    @Test fun displayNameFallsBackToFullNameAndDropsDot() {
        assertEquals("BRANKO DRAGICEVIC", buildDisplayName("", "", "", "branko dragicevic ."))
        assertEquals("RODRIGO TORRES", buildDisplayName(null, null, null, "rodrigo  torres ."))
    }

    @Test fun summaryKeepsNameAndIsIncomplete() {
        val c = CriminalSummaryDto(idRequisitoriado = 1, hashRequisitoriado = "H", nombreCompleto = "josimar atachi felix", delito = "SICARIATO").toDomain()
        assertEquals("JOSIMAR ATACHI FELIX", c.displayName)
        assertEquals(listOf("SICARIATO"), c.crimes)
        assertFalse(c.isComplete)
    }

    @Test fun detailMapsEverything() {
        val c = CriminalDetailDto(
            idRequisitoriado = 6229, hashRequisitoriado = "H", apellidoPaterno = "agustin", apellidoMaterno = "huayta",
            nombres = "nemesio", sexo = 1, montoRecompensa = 20_000.0, delitos = listOf("VIOLACIÓN SEXUAL"),
            departamento = "UCAYALI", provincia = "CORONEL PORTILLO", foto = "",
        ).toDomain()
        assertEquals("AGUSTIN HUAYTA, NEMESIO", c.displayName)
        assertEquals(Sex.MALE, c.sex)
        assertEquals("UCAYALI", c.departamento)
        assertNull(c.photoBase64)
        assertTrue(c.isComplete)
    }
}
