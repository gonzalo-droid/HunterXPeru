package pe.cazadores.data.repository

import pe.cazadores.domain.model.SortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Valores verificados contra producción el 2026-11-18 (ver el KDoc de [PageTranslation.sortField]). */
class PageTranslationTest {
    @Test fun sortFieldsAcceptedByTheApi() {
        assertEquals("nombreCompleto" to "asc", PageTranslation.sortField(SortOrder.NAME))
        assertEquals("montoRecompensa" to "desc", PageTranslation.sortField(SortOrder.REWARD))
        assertEquals("montoRecompensa" to "asc", PageTranslation.sortField(SortOrder.REWARD_ASC))
    }

    @Test fun pagesAreOneIndexedInRequestAndZeroIndexedInResponse() {
        assertEquals(1, PageTranslation.toRequestPage(1))
        assertEquals(1, PageTranslation.toUiPage(0))
        assertFailsWith<IllegalArgumentException> { PageTranslation.toRequestPage(0) }
    }
}
