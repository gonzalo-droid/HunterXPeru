package pe.cazadores.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pe.cazadores.core.result.AppResult
import pe.cazadores.core.result.DataError
import pe.cazadores.data.remote.ApiJson
import pe.cazadores.data.remote.RecompensasApi
import pe.cazadores.data.remote.createHttpClient
import pe.cazadores.domain.model.SearchFilters
import pe.cazadores.domain.model.SortOrder
import pe.cazadores.domain.repository.TimeProvider
import pe.cazadores.testing.ApiFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CriminalRepositoryImplTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private var now = 1_000_000_000L
    private val dao = FakeCacheDao()
    private var online = true
    private val requests = mutableListOf<String>()

    private fun repo(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { ok(ApiFixtures.SEARCH_PAGE_1) }) =
        CriminalRepositoryImpl(
            api = RecompensasApi(
                createHttpClient(
                    MockEngine { request ->
                        requests += request.body.toByteArray().decodeToString()
                        if (!online) throw IOException("sin red")
                        handler(request)
                    },
                    userAgent = "test", debug = false,
                ),
            ),
            cache = dao,
            time = TimeProvider { now },
            json = ApiJson,
        )

    private fun MockRequestHandleScope.ok(body: String) = respond(body, HttpStatusCode.OK, jsonHeaders)

    @Test fun firstUiPageRequestsApiPage1AndTranslatesResponse() = runTest {
        val result = repo().search(SearchFilters(), page = 1)
        val page = assertIs<AppResult.Success<*>>(result).data as pe.cazadores.domain.model.SearchPage
        assertEquals(1, page.page, "number=0 de la API es la página 1 de la UI")
        assertEquals(649, page.totalElements)
        assertEquals("NEMESIO AGUSTIN HUAYTA", page.items.first().displayName)
        val sentPage = Json.parseToJsonElement(requests.single()).jsonObject["pageInfo"]!!.jsonObject["page"]!!.jsonPrimitive.int
        assertEquals(1, sentPage)
    }

    @Test fun rewardSortSendsMontoRecompensaDesc() = runTest {
        repo().search(SearchFilters(sort = SortOrder.REWARD), page = 2)
        val info = Json.parseToJsonElement(requests.single()).jsonObject["pageInfo"]!!.jsonObject
        assertEquals("montoRecompensa", info["sortBy"]!!.jsonPrimitive.content)
        assertEquals("desc", info["direction"]!!.jsonPrimitive.content)
        assertEquals(2, info["page"]!!.jsonPrimitive.int)
    }

    @Test fun offlineFallsBackToCachedFirstPageWithDate() = runTest {
        val repository = repo()
        repository.search(SearchFilters(query = "atachi"), 1)
        val cachedAt = now
        now += 60_000
        online = false
        val result = assertIs<AppResult.Success<pe.cazadores.domain.model.SearchPage>>(repository.search(SearchFilters(query = "ATACHI "), 1))
        assertEquals(cachedAt, result.cachedAtMillis)
        assertEquals(2, result.data.items.size)
        assertEquals("NEMESIO AGUSTIN HUAYTA", result.data.items.first().displayName, "El nombre sobrevive a la caché (B3)")
    }

    @Test fun samePersonInTwoSearchesKeepsBothCaches() = runTest {
        val repository = repo()
        repository.search(SearchFilters(query = "a"), 1)
        repository.search(SearchFilters(query = "b"), 1)
        assertEquals(4, dao.searchRowCount(), "PK (searchKey, hash): sin choque entre búsquedas (B4)")
    }

    @Test fun offlineWithoutCacheIsNetworkError() = runTest {
        online = false
        val result = repo().search(SearchFilters(query = "nadie"), 1)
        assertEquals(DataError.Network, assertIs<AppResult.Failure>(result).error)
    }

    @Test fun expiredCacheIsNotUsed() = runTest {
        val repository = repo()
        repository.search(SearchFilters(), 1)
        now += CriminalRepositoryImpl.TTL_MILLIS + 1
        online = false
        assertIs<AppResult.Failure>(repository.search(SearchFilters(), 1))
    }

    @Test fun timeoutIsTimeoutError() = runTest {
        val result = repo { throw HttpRequestTimeoutException("x", 1) }.search(SearchFilters(query = "z"), 2)
        assertEquals(DataError.Timeout, assertIs<AppResult.Failure>(result).error)
    }

    @Test fun invalidHashIsNotFound() = runTest {
        val result = repo { respond(ApiFixtures.NOT_FOUND, HttpStatusCode.NotFound, jsonHeaders) }.detail("XXXX")
        assertEquals(DataError.NotFound, assertIs<AppResult.Failure>(result).error)
    }

    @Test fun detailIsCachedForOffline() = runTest {
        val repository = repo { ok(ApiFixtures.DETAIL) }
        assertNull(assertIs<AppResult.Success<*>>(repository.detail("D1321FADB4B9D70B17504891006156FC")).cachedAtMillis)
        online = false
        val cached = assertIs<AppResult.Success<pe.cazadores.domain.model.Criminal>>(repository.detail("D1321FADB4B9D70B17504891006156FC"))
        assertNotNull(cached.cachedAtMillis)
        assertEquals("UCAYALI", cached.data.departamento)
    }

    @Test fun homeStatsUsesRewardSortForMaximum() = runTest {
        val repository = repo { request ->
            val body = request.body.toByteArray().decodeToString()
            if ("montoRecompensa" in body) {
                ok("""{"content":[{"idRequisitoriado":1,"hashRequisitoriado":"A","montoRecompensa":500000.0}],"totalElements":649,"number":0}""")
            } else {
                ok("""{"content":[{"idRequisitoriado":2,"hashRequisitoriado":"B","montoRecompensa":20000.0}],"totalElements":649,"number":0}""")
            }
        }
        val stats = assertIs<AppResult.Success<pe.cazadores.domain.model.HomeStats>>(repository.homeStats()).data
        assertEquals(649, stats.total)
        assertEquals(500_000.0, stats.maxReward, "B2: no la recompensa del último registro")
    }

    @Test fun provinceIsOnlySentWithDepartment() {
        assertNull(SearchFilters(provinciaId = "01").toRequest(1, 20).search.idProvincia)
        assertEquals("01", SearchFilters(departamentoId = "15", provinciaId = "01").toRequest(1, 20).search.idProvincia)
    }
}
