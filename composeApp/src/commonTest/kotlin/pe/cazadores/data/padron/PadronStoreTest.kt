package pe.cazadores.data.padron

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import pe.cazadores.data.remote.createStorageClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pe.cazadores.domain.repository.NetworkMonitor
import pe.cazadores.domain.repository.TimeProvider
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PadronStoreTest {
    private val dao = FakePadronDao()
    private val requests = mutableListOf<String>()
    private val files = mutableMapOf<String, ByteArray>()
    private val online = MutableStateFlow(true)
    private var now = 1_000_000L

    private fun TestScope.store() = PadronStore(
        // El motor corre en el reloj virtual del test: si no, withTimeout salta mientras espera la "red".
        client = createStorageClient(
            MockEngine(
                MockEngineConfig().apply {
                    dispatcher = StandardTestDispatcher(testScheduler)
                    addHandler { request ->
                        val key = request.url.encodedPath.substringAfter("/o/").replace("%2F", "/")
                        requests += key
                        // Como Storage real: un GET con Content-Type da 403 SignatureDoesNotMatch.
                        if (request.headers[HttpHeaders.ContentType] != null || request.body.contentType != null) {
                            return@addHandler respondError(HttpStatusCode.Forbidden)
                        }
                        files[key]?.let { respond(it) } ?: respondError(HttpStatusCode.NotFound)
                    }
                },
            ),
            userAgent = "test",
        ),
        dao = dao,
        json = Json { ignoreUnknownKeys = true },
        time = TimeProvider { now },
        network = object : NetworkMonitor { override val isOnline = online },
        scope = backgroundScope,
    )

    /** Corre lo pendiente en `backgroundScope` (advanceUntilIdle no espera esas tareas). */
    private fun TestScope.settle() = runCurrent()

    private fun publish(version: Int, vararg hashes: String) {
        files["data/manifest.json"] = manifestJson(version, *hashes.map { record(it) }.toTypedArray()).encodeToByteArray()
        files["data/thumbs-v$version.zip"] = storedZip(hashes.associate { "thumbs/$it.jpg" to byteArrayOf(version.toByte()) })
    }

    @Test fun firstLoadDownloadsManifestAndThumbs() = runTest {
        publish(1, "a", "b")
        val store = store()
        val padron = assertNotNull(store.current())
        assertEquals(listOf("a", "b"), padron.criminals.map { it.hash })
        settle()
        assertEquals(setOf("thumbs/a.jpg", "thumbs/b.jpg"), dao.images.keys)
        assertEquals(1, requests.count { it == "data/thumbs-v1.zip" })
    }

    @Test fun localCopyAnswersWithoutNetworkUntilCheckIsDue() = runTest {
        publish(1, "a")
        store().current()
        settle()
        requests.clear()

        val reopened = store()
        assertEquals(1, reopened.current()?.version)
        settle()
        assertTrue(requests.isEmpty(), "Dentro del intervalo no se consulta Firebase: $requests")

        publish(2, "b")
        now += PadronStore.CHECK_INTERVAL_MS
        assertEquals(1, reopened.current()?.version, "Responde con lo guardado y revisa en segundo plano")
        settle()
        assertEquals(2, reopened.current()?.version)
        assertEquals(setOf("thumbs/b.jpg"), dao.images.keys, "Se borran las miniaturas de quien ya no está")
    }

    @Test fun sameVersionOnlyMarksChecked() = runTest {
        publish(1, "a")
        store().current()
        settle()
        now += PadronStore.CHECK_INTERVAL_MS
        val reopened = store()
        reopened.current()
        settle()
        assertEquals(now, dao.meta?.checkedAt)
        assertEquals(1, requests.count { it == "data/thumbs-v1.zip" })
    }

    @Test fun offlineWithoutLocalCopyFallsBackToApi() = runTest {
        online.value = false
        assertNull(store().current())
        assertTrue(requests.isEmpty())
    }

    @Test fun brokenManifestKeepsPrevious() = runTest {
        publish(1, "a")
        store().current()
        settle()
        files["data/manifest.json"] = "{no es json".encodeToByteArray()
        now += PadronStore.CHECK_INTERVAL_MS
        val reopened = store()
        reopened.current()
        settle()
        assertEquals(1, reopened.current()?.version)
    }

    @Test fun photosAreCachedAndPruned() = runTest {
        val store = store()
        repeat(PadronStore.MAX_PHOTOS + 2) { i ->
            files["photos/$i.jpg"] = byteArrayOf(i.toByte())
            now += 1
            assertContentEquals(byteArrayOf(i.toByte()), store.image("photos/$i.jpg"))
        }
        assertEquals(PadronStore.MAX_PHOTOS, dao.images.size)
        assertNull(dao.images["photos/0.jpg"], "Se borran las más antiguas")
        online.value = false
        assertNotNull(store.image("photos/${PadronStore.MAX_PHOTOS + 1}.jpg"), "La guardada se lee sin red")
        assertNull(store.image("photos/0.jpg"))
    }

    @Test fun imagesSyncingWhileThumbsZipDownloads() = runTest {
        publish(1, "a")
        val store = store()
        store.current()
        assertTrue(store.imagesSyncing.value, "Con el manifest guardado, la miniatura todavía puede llegar")
        settle()
        assertEquals(false, store.imagesSyncing.value)
    }

    @Test fun observeImageEmitsOnceThumbsAreSaved() = runTest {
        publish(1, "a")
        val store = store()
        val seen = mutableListOf<ByteArray?>()
        backgroundScope.launch { store.observeImage("thumbs/a.jpg").collect { seen += it } }
        store.current()
        settle()
        assertNull(seen.first(), "Antes del zip no hay miniatura")
        assertContentEquals(byteArrayOf(1), seen.last())
    }

    @Test fun missingThumbsAreRetriedWhenNetworkReturns() = runTest {
        publish(1, "a")
        val zip = files.remove("data/thumbs-v1.zip")!!
        val store = store()
        store.current()
        settle()
        assertTrue(dao.images.isEmpty(), "El zip falló")
        assertEquals(false, store.imagesSyncing.value, "Sin nada que esperar, la tarjeta puede mostrar la silueta")

        files["data/thumbs-v1.zip"] = zip
        online.value = false
        settle()
        online.value = true
        settle()
        assertEquals(setOf("thumbs/a.jpg"), dao.images.keys)
    }
}

