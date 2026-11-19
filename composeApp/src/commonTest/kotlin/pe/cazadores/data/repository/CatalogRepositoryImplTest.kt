package pe.cazadores.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import pe.cazadores.data.remote.ApiJson
import pe.cazadores.data.remote.RecompensasApi
import pe.cazadores.data.remote.createHttpClient
import pe.cazadores.domain.repository.TimeProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CatalogRepositoryImplTest {
    private val requests = mutableListOf<String>()
    private val bundled = mapOf(
        CatalogRepositoryImpl.DELITOS to """[{"idDelito":1,"descripcion":"ROBO"}]""",
        CatalogRepositoryImpl.DEPARTAMENTOS to """[{"departamento":"01","descripcion":"AMAZONAS"}]""",
        CatalogRepositoryImpl.PROVINCIAS to """[{"departamento":"01","provincia":"01","descripcion":"CHACHAPOYAS"}]""",
    )

    private val dao = FakeCacheDao()

    private fun TestScope.repo() = CatalogRepositoryImpl(
        api = RecompensasApi(
            createHttpClient(
                MockEngine(
                    MockEngineConfig().apply {
                        dispatcher = StandardTestDispatcher(testScheduler)
                        addHandler { request ->
                            requests += request.url.encodedPath
                            respond("""[{"idDelito":2,"descripcion":"EXTORSION"}]""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                        }
                    },
                ),
                userAgent = "test", debug = false,
            ),
        ),
        cache = dao,
        bundled = BundledCatalogSource { name -> bundled.getValue(name) },
        time = TimeProvider { 0L },
        json = ApiJson,
        scope = backgroundScope,
    )

    @Test fun ubigeoComesFromBundledFilesWithoutCallingApi() = runTest {
        val catalogs = repo().catalogs()
        assertEquals(listOf("AMAZONAS"), catalogs.departamentos.map { it.name })
        assertEquals(listOf("CHACHAPOYAS"), catalogs.provincias.map { it.name })
        assertEquals(emptyList(), requests.filter { "ubigeo" in it }, "Departamentos y provincias no se piden a la API")
    }

    @Test fun delitosAnswerFromBundledAndRefreshInBackground() = runTest {
        val repo = repo()
        assertEquals(listOf("ROBO"), repo.catalogs().delitos.map { it.name }, "No espera a la API")
        runCurrent()
        assertEquals(listOf("EXTORSION"), repo.catalogs().delitos.map { it.name }, "La API actualiza memoria")
        assertNotNull(dao.catalog(CatalogRepositoryImpl.DELITOS), "y la caché")

        requests.clear()
        repo().catalogs()
        runCurrent()
        assertEquals(emptyList(), requests, "Con caché fresca no se llama a la API")
    }
}

