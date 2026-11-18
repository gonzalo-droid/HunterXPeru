package pe.cazadores.ui.navigation

import kotlinx.serialization.json.Json
import pe.cazadores.ui.screens.legal.LegalPage
import kotlin.test.Test
import kotlin.test.assertEquals

class LegalRouteTest {
    @Test
    fun pageRoundTripsThroughSerialization() {
        LegalPage.entries.forEach { page ->
            val encoded = Json.encodeToString(Route.Legal.serializer(), Route.Legal(page))
            assertEquals(page, Json.decodeFromString(Route.Legal.serializer(), encoded).page)
        }
    }

    @Test
    fun unknownPageFallsBackToPrivacy() {
        assertEquals(LegalPage.PRIVACY, Route.Legal("NOPE").page)
    }
}
