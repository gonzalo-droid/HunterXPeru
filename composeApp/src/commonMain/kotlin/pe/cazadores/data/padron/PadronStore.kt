package pe.cazadores.data.padron

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import pe.cazadores.core.config.AppConfig
import pe.cazadores.data.local.PadronDao
import pe.cazadores.data.local.PadronImageEntity
import pe.cazadores.data.local.PadronMetaEntity
import pe.cazadores.domain.repository.NetworkMonitor
import pe.cazadores.domain.repository.PhotoRepository
import pe.cazadores.domain.repository.TimeProvider

/** Origen del padrón para [pe.cazadores.data.repository.CriminalRepositoryImpl]. */
fun interface PadronSource {
    /** Padrón vigente, o `null` si nunca se pudo bajar (la app sigue con la API). */
    suspend fun current(): Padron?

    companion object {
        val Unavailable = PadronSource { null }
    }
}

/**
 * Padrón semanal publicado en Firebase Storage (D15/D20).
 *
 * - Primera vez: espera el manifest (300 KB) como máximo [firstLoadTimeoutMs]; si no llega, `null`.
 * - Después: responde al instante con lo guardado en Room y, cada [checkIntervalMs], consulta en segundo
 *   plano si hay versión nueva.
 * - Las miniaturas llegan en un zip aparte, en segundo plano: mientras falten, [image] las baja una a una.
 *   [imagesSyncing] avisa que siguen llegando y [observeImage] entrega cada una al guardarse. Si el zip falla,
 *   se vuelve a intentar cuando vuelve la red.
 */
class PadronStore(
    private val client: HttpClient,
    private val dao: PadronDao,
    private val json: Json,
    private val time: TimeProvider,
    private val network: NetworkMonitor,
    private val scope: CoroutineScope,
    private val baseUrl: String = AppConfig.STORAGE_BASE_URL,
    private val checkIntervalMs: Long = CHECK_INTERVAL_MS,
    private val firstLoadTimeoutMs: Long = FIRST_LOAD_TIMEOUT_MS,
) : PadronSource, PhotoRepository {

    private val memoryLock = Mutex()
    private val refreshLock = Mutex()
    private var memory: Padron? = null
    private var memoryLoaded = false
    private var checkedAt = 0L
    private var failedAt = Long.MIN_VALUE / 2
    private var backgroundRefresh: Job? = null
    private var thumbsJob: Job? = null
    private val syncing = MutableStateFlow(false)
    override val imagesSyncing: StateFlow<Boolean> = syncing.asStateFlow()

    init {
        // Vuelve la red (no el primer valor): reintenta las miniaturas que no llegaron.
        scope.launch {
            network.isOnline.distinctUntilChanged().drop(1).filter { it }.collect {
                val padron = memoryLock.withLock { memory } ?: return@collect
                if (thumbsJob?.isActive != true) startThumbs(padron, zipKey = null)
            }
        }
    }

    override suspend fun current(): Padron? {
        val local = loadLocal()
        if (local == null) {
            // Sin padrón y Firebase acaba de fallar: no hacer esperar a cada pantalla otra vez.
            if (time.nowMillis() - failedAt < RETRY_AFTER_FAILURE_MS) return null
            return withTimeoutOrNull(firstLoadTimeoutMs) { refresh() } ?: run {
                failedAt = time.nowMillis()
                null
            }
        }
        if (time.nowMillis() - checkedAt >= checkIntervalMs) refreshInBackground()
        return local
    }

    /** Consulta ya si hay versión nueva (al abrir la app). No falla: sin red no hace nada. */
    fun refreshInBackground() {
        if (backgroundRefresh?.isActive == true) return
        backgroundRefresh = scope.launch { refresh() }
    }

    override suspend fun image(key: String): ByteArray? {
        dao.image(key)?.let { return it.bytes }
        if (!isOnline()) return null
        val bytes = download(key) ?: return null
        dao.upsertImages(listOf(PadronImageEntity(key, bytes, time.nowMillis())))
        if (key.startsWith(PHOTOS_PREFIX)) dao.photosBeyond(MAX_PHOTOS).takeIf { it.isNotEmpty() }?.let { dao.deleteImages(it) }
        return bytes
    }

    override fun observeImage(key: String): Flow<ByteArray?> = dao.observeImage(key).map { it?.bytes }

    /** Marca [imagesSyncing] antes de lanzar, para que ninguna tarjeta muestre la silueta en el intervalo. */
    private fun startThumbs(padron: Padron, zipKey: String?) {
        thumbsJob?.cancel()
        syncing.value = true
        // LAZY: el job queda asignado antes de correr, así el `finally` sabe si sigue siendo el vigente.
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                syncThumbs(padron, zipKey)
            } finally {
                // Un job cancelado lo reemplaza otro que ya marcó `true`.
                if (thumbsJob == coroutineContext[Job]) syncing.value = false
            }
        }
        thumbsJob = job
        job.start()
    }

    private suspend fun loadLocal(): Padron? = memoryLock.withLock {
        if (!memoryLoaded) {
            memoryLoaded = true
            dao.meta()?.let { meta ->
                checkedAt = meta.checkedAt
                memory = parse(meta.json)
                // La app pudo cerrarse antes de terminar de guardar las miniaturas.
                memory?.let { startThumbs(it, zipKey = null) }
            }
        }
        memory
    }

    /** Devuelve el padrón vigente tras consultar Firebase (o el que había, si la consulta falla). */
    private suspend fun refresh(): Padron? = refreshLock.withLock {
        val previous = loadLocal()
        // Otro llamador ya consultó mientras se esperaba el candado.
        if (previous != null && time.nowMillis() - checkedAt < checkIntervalMs) return@withLock previous
        if (!isOnline()) return@withLock previous
        val raw = runCatchingIo { client.get(urlOf(AppConfig.MANIFEST_PATH)) { header(HttpHeaders.Accept, "*/*") }.bodyAsText() }
            ?: return@withLock previous
        val manifest = runCatchingIo { json.decodeFromString(ManifestDto.serializer(), raw) }
        val now = time.nowMillis()
        val fresh = manifest?.let { parse(it) }
        if (manifest == null || fresh == null) return@withLock previous

        if (previous != null && fresh.version <= previous.version) {
            dao.markChecked(now)
            checkedAt = now
            return@withLock previous
        }
        dao.upsertMeta(PadronMetaEntity(version = fresh.version, json = raw, generatedAt = fresh.generatedAtMillis, checkedAt = now))
        memoryLock.withLock { memory = fresh }
        checkedAt = now
        startThumbs(fresh, manifest.thumbsZip)
        fresh
    }

    /** Baja el zip de miniaturas si faltan y borra las de personas que ya no están. */
    private suspend fun syncThumbs(padron: Padron, zipKey: String?) {
        val wanted = padron.criminals.mapNotNull { it.thumbKey }.toSet()
        val stored = dao.imagePaths(THUMBS_PREFIX).toSet()
        (stored - wanted).takeIf { it.isNotEmpty() }?.let { gone -> gone.chunked(500).forEach { dao.deleteImages(it) } }
        val missing = wanted - stored
        if (missing.isEmpty()) return
        val key = zipKey ?: dao.meta()?.json?.let { runCatchingIo { json.decodeFromString(ManifestDto.serializer(), it).thumbsZip } } ?: return
        if (!isOnline()) return
        val zip = download(key) ?: return
        val now = time.nowMillis()
        StoredZip.read(zip).filterKeys { it in missing }.entries.chunked(100).forEach { chunk ->
            dao.upsertImages(chunk.map { (path, bytes) -> PadronImageEntity(path, bytes, now) })
        }
    }

    private fun parse(raw: String): Padron? =
        runCatchingIo { json.decodeFromString(ManifestDto.serializer(), raw) }?.let { parse(it) }

    /** Manifest vacío o de un esquema que no se conoce: se ignora (la sincronización no publica vacíos). */
    private fun parse(manifest: ManifestDto): Padron? {
        if (manifest.schema > SUPPORTED_SCHEMA || manifest.criminals.isEmpty()) return null
        val generatedAt = runCatchingIo { manifest.generatedAtMillis() } ?: return null
        return Padron(
            version = manifest.version,
            criminals = manifest.criminals.filter { it.hash.isNotBlank() }.map { it.toDomain() },
            generatedAtMillis = generatedAt,
        )
    }

    private suspend fun download(key: String): ByteArray? =
        runCatchingIo { client.get(urlOf(key)) { header(HttpHeaders.Accept, "*/*") }.bodyAsBytes() }?.takeIf { it.isNotEmpty() }

    private fun urlOf(key: String) = "$baseUrl/o/${key.encodeURLParameter()}?alt=media"

    private suspend fun isOnline(): Boolean =
        withTimeoutOrNull(NETWORK_CHECK_TIMEOUT_MS) { network.isOnline.first() } != false

    private inline fun <T> runCatchingIo(block: () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        println("Padrón: ${e::class.simpleName}: ${e.message}")
        null
    }

    companion object {
        const val CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000
        const val FIRST_LOAD_TIMEOUT_MS = 15_000L
        const val RETRY_AFTER_FAILURE_MS = 60_000L
        const val MAX_PHOTOS = 60
        private const val NETWORK_CHECK_TIMEOUT_MS = 500L
        private const val THUMBS_PREFIX = "thumbs/"
        private const val PHOTOS_PREFIX = "photos/"
    }
}
