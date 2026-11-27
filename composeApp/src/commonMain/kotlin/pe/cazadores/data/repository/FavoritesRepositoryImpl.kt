package pe.cazadores.data.repository

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import pe.cazadores.data.local.FavoriteDao
import pe.cazadores.data.local.FavoriteEntity
import pe.cazadores.data.padron.Padron
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.Sex
import pe.cazadores.domain.repository.FavoritesRepository
import pe.cazadores.domain.repository.PhotoRepository
import pe.cazadores.domain.repository.TimeProvider

class FavoritesRepositoryImpl(
    private val dao: FavoriteDao,
    private val time: TimeProvider,
    private val json: Json,
    private val photos: PhotoRepository? = null,
    padronUpdates: Flow<Padron?> = emptyFlow(),
    scope: CoroutineScope? = null,
) : FavoritesRepository {
    private val crimesSerializer = ListSerializer(String.serializer())

    init {
        scope?.launch { padronUpdates.filterNotNull().collect { reconcile(it) } }
    }

    /**
     * Pone los guardados al día con el padrón: monto, delitos, datos y miniatura nuevos; quien ya no
     * figura queda marcado (no se borra: la persona lo guardó). Solo escribe lo que cambió.
     */
    @OptIn(ExperimentalEncodingApi::class)
    internal suspend fun reconcile(padron: Padron) {
        val now = time.nowMillis()
        dao.all().forEach { saved ->
            val fresh = padron.byHash[saved.hash]
            val updated = when {
                fresh == null -> if (saved.delistedAt != null) saved else saved.copy(delistedAt = now)
                else -> {
                    // shortcut: sin la miniatura a mano (sin red) se queda la foto anterior hasta la próxima carga del padrón.
                    val photo = fresh.thumbKey?.let { photos?.image(it) }?.let { Base64.encode(it) } ?: saved.photoBase64
                    fresh.copy(photoBase64 = photo).toEntity(saved.savedAt)
                }
            }
            if (updated != saved) dao.upsert(updated)
        }
    }

    override fun observeAll(): Flow<List<Criminal>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeIsFavorite(hash: String): Flow<Boolean> = dao.observeIsFavorite(hash)

    /** Conserva la fecha original al completar un favorito con el detalle. */
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun save(criminal: Criminal) {
        val existing = dao.get(criminal.hash)
        val savedAt = existing?.savedAt ?: time.nowMillis()
        // Del padrón llega sin Base64: se guarda la miniatura para que el favorito tenga foto aunque
        // la persona salga del padrón y su miniatura se borre.
        val photo = criminal.photoBase64
            ?: criminal.thumbKey?.let { key -> photos?.image(key)?.let { Base64.encode(it) } }
            ?: existing?.photoBase64
        // "Deshacer" tras quitar uno que ya no figura: conserva la marca.
        val delistedAt = if (criminal.delisted) existing?.delistedAt ?: time.nowMillis() else null
        dao.upsert(criminal.copy(photoBase64 = photo).toEntity(savedAt).copy(delistedAt = delistedAt))
    }

    override suspend fun remove(hash: String) = dao.delete(hash)

    private fun Criminal.toEntity(savedAt: Long) = FavoriteEntity(
        hash = hash,
        id = id,
        displayName = displayName,
        apellidoPaterno = apellidoPaterno,
        apellidoMaterno = apellidoMaterno,
        nombres = nombres,
        sex = sex?.name,
        reward = reward,
        crimesJson = json.encodeToString(crimesSerializer, crimes),
        photoBase64 = photoBase64,
        departamento = departamento,
        provincia = provincia,
        savedAt = savedAt,
    )

    private fun FavoriteEntity.toDomain() = Criminal(
        id = id,
        hash = hash,
        displayName = displayName,
        apellidoPaterno = apellidoPaterno,
        apellidoMaterno = apellidoMaterno,
        nombres = nombres,
        sex = sex?.let { name -> Sex.entries.firstOrNull { it.name == name } },
        reward = reward,
        crimes = runCatching { json.decodeFromString(crimesSerializer, crimesJson) }.getOrDefault(emptyList()),
        photoBase64 = photoBase64,
        departamento = departamento,
        provincia = provincia,
        delisted = delistedAt != null,
    )
}
