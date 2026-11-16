package pe.cazadores.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import pe.cazadores.data.local.FavoriteDao
import pe.cazadores.data.local.FavoriteEntity
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.Sex
import pe.cazadores.domain.repository.FavoritesRepository
import pe.cazadores.domain.repository.TimeProvider

class FavoritesRepositoryImpl(
    private val dao: FavoriteDao,
    private val time: TimeProvider,
    private val json: Json,
) : FavoritesRepository {
    private val crimesSerializer = ListSerializer(String.serializer())

    override fun observeAll(): Flow<List<Criminal>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeIsFavorite(hash: String): Flow<Boolean> = dao.observeIsFavorite(hash)

    /** Conserva la fecha original al completar un favorito con el detalle. */
    override suspend fun save(criminal: Criminal) {
        val savedAt = dao.get(criminal.hash)?.savedAt ?: time.nowMillis()
        dao.upsert(criminal.toEntity(savedAt))
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
    )
}
