package pe.cazadores.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pe.cazadores.data.remote.dto.PageInfoDto
import pe.cazadores.data.remote.dto.SearchDto
import pe.cazadores.data.remote.dto.SearchRequestDto
import pe.cazadores.testing.ApiFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecompensasApiTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(engine: MockEngine) =
        RecompensasApi(createHttpClient(engine, userAgent = "CazadoresPeru/test (JVM)", debug = false))

    @Test
    fun searchSendsExpectedBodyAndHonestHeaders() = runTest {
        var sentBody = ""
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api-recompensas/requisitoriados/pageandfilter", request.url.encodedPath)
            assertEquals("CazadoresPeru/test (JVM)", request.headers[HttpHeaders.UserAgent])
            assertNull(request.headers[HttpHeaders.Origin], "No se imita el Origin de recompensas.pe")
            assertNull(request.headers[HttpHeaders.Referrer])
            sentBody = request.body.toByteArray().decodeToString()
            respond(ApiFixtures.SEARCH_PAGE_1, HttpStatusCode.OK, jsonHeaders)
        }

        api(engine).search(
            SearchRequestDto(
                pageInfo = PageInfoDto(page = 1, size = 20, sortBy = "id", direction = "desc"),
                search = SearchDto(nombreCompleto = "atachi", idDepartamento = "15", idDelito = 10),
            ),
        )

        val body = Json.parseToJsonElement(sentBody).jsonObject
        val pageInfo = body.getValue("pageInfo").jsonObject
        assertEquals(1, pageInfo.getValue("page").jsonPrimitive.int)
        val search = body.getValue("search").jsonObject
        assertEquals("atachi", search.getValue("nombreCompleto").jsonPrimitive.content)
        assertEquals("15", search.getValue("idDepartamento").jsonPrimitive.content)
        assertEquals(10, search.getValue("idDelito").jsonPrimitive.int)
        assertEquals("F", search.getValue("tipoFilter").jsonPrimitive.content)
    }

    @Test
    fun parsesRealSearchPage() = runTest {
        val page = api(MockEngine { respond(ApiFixtures.SEARCH_PAGE_1, HttpStatusCode.OK, jsonHeaders) })
            .search(SearchRequestDto(PageInfoDto(1, 3, "id", "desc")))
        assertEquals(649, page.totalElements)
        assertEquals(0, page.number, "La API responde 0-indexed aunque se pida page=1")
        assertTrue(page.first)
        assertFalse(page.last)
        assertEquals("D1321FADB4B9D70B17504891006156FC", page.content.first().hashRequisitoriado)
        assertEquals(20_000.0, page.content.first().montoRecompensa)
    }

    @Test
    fun parsesDetailAndTop5WithNullPhoto() = runTest {
        val engine = MockEngine { request ->
            val body = if (request.url.encodedPath.endsWith("top5")) ApiFixtures.TOP5 else ApiFixtures.DETAIL
            respond(body, HttpStatusCode.OK, jsonHeaders)
        }
        val detail = api(engine).detail("D1321FADB4B9D70B17504891006156FC")
        assertEquals("nemesio", detail.nombres)
        assertEquals(listOf("VIOLACIÓN SEXUAL"), detail.delitos)
        assertEquals("UCAYALI", detail.departamento)

        val top = api(engine).top5()
        assertEquals(1, top.size)
        assertNull(top.first().foto)
    }

    @Test
    fun notFoundIsAClientError() = runTest {
        val engine = MockEngine { respond(ApiFixtures.NOT_FOUND, HttpStatusCode.NotFound, jsonHeaders) }
        val error = assertFailsWith<ClientRequestException> { api(engine).detail("XXXX") }
        assertEquals(HttpStatusCode.NotFound, error.response.status)
    }

    @Test
    fun retriesTransientErrorsThreeTimes() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            throw IOException("sin red")
        }
        assertFailsWith<IOException> { api(engine).top5() }
        assertEquals(4, calls, "1 intento + 3 reintentos")
    }

    @Test
    fun doesNotRetryHttpErrors() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond(ApiFixtures.NOT_FOUND, HttpStatusCode.NotFound, jsonHeaders)
        }
        assertFailsWith<ClientRequestException> { api(engine).detail("XXXX") }
        assertEquals(1, calls)
    }
}
