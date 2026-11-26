package pe.cazadores.data.local

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.Sex

/** Favorito: todos los campos de [Criminal], incluido `displayName` (bug B3 de Flutter). */
@Entity(tableName = "favorites", primaryKeys = ["hash"])
data class FavoriteEntity(
    val hash: String,
    val id: Int,
    val displayName: String,
    val apellidoPaterno: String?,
    val apellidoMaterno: String?,
    val nombres: String?,
    val sex: String?,
    val reward: Double,
    val crimesJson: String,
    val photoBase64: String?,
    val departamento: String?,
    val provincia: String?,
    val savedAt: Long,
    /** Cuándo dejó de figurar en el padrón; `null` mientras siga (v3). */
    val delistedAt: Long? = null,
)

/**
 * Primera página de cada búsqueda para el modo offline. PK compuesta `(searchKey, hash)`:
 * la misma persona puede aparecer en varias búsquedas (bug B4 de Flutter).
 */
@Entity(
    tableName = "search_cache",
    primaryKeys = ["searchKey", "hash"],
    indices = [Index("searchKey", "position")],
)
data class SearchCacheEntity(
    val searchKey: String,
    val hash: String,
    val position: Int,
    val json: String,
    val cachedAt: Long,
)

/** Totales de la búsqueda cacheada (para mostrar "N resultados" sin red). */
@Entity(tableName = "search_cache_meta", primaryKeys = ["searchKey"])
data class SearchCacheMetaEntity(
    val searchKey: String,
    val totalElements: Int,
    val totalPages: Int,
    val cachedAt: Long,
)

@Entity(tableName = "detail_cache", primaryKeys = ["hash"])
data class DetailCacheEntity(
    val hash: String,
    val json: String,
    val cachedAt: Long,
)

/** Catálogos (delitos, departamentos, provincias) como JSON, con TTL de 7 días. */
@Entity(tableName = "catalog_cache", primaryKeys = ["key"])
data class CatalogCacheEntity(
    val key: String,
    val json: String,
    val cachedAt: Long,
)

/** Forma serializable de [Criminal] para las cachés. */
@Serializable
data class CachedCriminal(
    val id: Int,
    val hash: String,
    val displayName: String,
    val apellidoPaterno: String? = null,
    val apellidoMaterno: String? = null,
    val nombres: String? = null,
    val sex: String? = null,
    val reward: Double = 0.0,
    val crimes: List<String> = emptyList(),
    val photoBase64: String? = null,
    val departamento: String? = null,
    val provincia: String? = null,
    val thumbKey: String? = null,
    val photoKey: String? = null,
)

fun Criminal.toCached() = CachedCriminal(
    id, hash, displayName, apellidoPaterno, apellidoMaterno, nombres, sex?.name, reward, crimes,
    photoBase64, departamento, provincia, thumbKey, photoKey,
)

fun CachedCriminal.toDomain() = Criminal(
    id = id,
    hash = hash,
    displayName = displayName,
    apellidoPaterno = apellidoPaterno,
    apellidoMaterno = apellidoMaterno,
    nombres = nombres,
    sex = sex?.let { name -> Sex.entries.firstOrNull { it.name == name } },
    reward = reward,
    crimes = crimes,
    photoBase64 = photoBase64,
    departamento = departamento,
    provincia = provincia,
    thumbKey = thumbKey,
    photoKey = photoKey,
)

/**
 * Padrón vigente (una sola fila): el `manifest.json` tal cual llegó de Firebase Storage.
 * [checkedAt] es la última vez que se consultó si había versión nueva.
 */
@Entity(tableName = "padron_meta", primaryKeys = ["id"])
data class PadronMetaEntity(
    val id: Int = 0,
    val version: Int,
    val json: String,
    val generatedAt: Long,
    val checkedAt: Long,
)

/** Imagen guardada por su ruta en Storage: miniaturas del padrón y fotos de ficha ya vistas. */
@Entity(tableName = "padron_image", primaryKeys = ["path"])
data class PadronImageEntity(
    val path: String,
    val bytes: ByteArray,
    val savedAt: Long,
) {
    override fun equals(other: Any?) = other is PadronImageEntity && path == other.path && savedAt == other.savedAt
    override fun hashCode() = path.hashCode()
}
