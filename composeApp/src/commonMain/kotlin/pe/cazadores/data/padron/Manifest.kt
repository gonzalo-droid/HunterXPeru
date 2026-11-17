package pe.cazadores.data.padron

import kotlinx.serialization.Serializable
import pe.cazadores.data.mapper.buildDisplayName
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.Sex
import kotlin.time.Instant

/** `data/manifest.json` (schema 1), ver `sync/README.md`. */
@Serializable
data class ManifestDto(
    val schema: Int = 1,
    val version: Int,
    val dataHash: String = "",
    val generatedAt: String,
    val count: Int = 0,
    val thumbsZip: String? = null,
    val criminals: List<ManifestRecordDto> = emptyList(),
)

@Serializable
data class ManifestRecordDto(
    val id: Int? = null,
    val hash: String,
    val nombreCompleto: String? = null,
    val apellidoPaterno: String? = null,
    val apellidoMaterno: String? = null,
    val nombres: String? = null,
    val sexo: Int? = null,
    val montoRecompensa: Double = 0.0,
    val delito: String? = null,
    val delitos: List<String> = emptyList(),
    val departamento: String? = null,
    val provincia: String? = null,
    val thumb: String? = null,
    val photo: String? = null,
)

/** Versión de esquema que esta app sabe leer: una mayor se ignora y se sigue con la API. */
internal const val SUPPORTED_SCHEMA = 1

internal fun ManifestDto.generatedAtMillis(): Long = Instant.parse(generatedAt).toEpochMilliseconds()

internal fun ManifestRecordDto.toDomain(): Criminal = Criminal(
    id = id ?: 0,
    hash = hash,
    displayName = buildDisplayName(apellidoPaterno, apellidoMaterno, nombres, nombreCompleto),
    apellidoPaterno = apellidoPaterno?.ifBlank { null },
    apellidoMaterno = apellidoMaterno?.ifBlank { null },
    nombres = nombres?.ifBlank { null },
    sex = when (sexo) {
        1 -> Sex.MALE
        2 -> Sex.FEMALE
        else -> null
    },
    reward = montoRecompensa,
    crimes = delitos.filter { it.isNotBlank() }.ifEmpty { listOfNotNull(delito?.takeIf { it.isNotBlank() }) },
    departamento = departamento?.ifBlank { null },
    provincia = provincia?.ifBlank { null },
    thumbKey = thumb?.ifBlank { null },
    photoKey = photo?.ifBlank { null },
)

/** Padrón en memoria: todas las personas, ya mapeadas, y la fecha en que se publicó. */
data class Padron(
    val version: Int,
    val criminals: List<Criminal>,
    val generatedAtMillis: Long,
) {
    val byHash: Map<String, Criminal> by lazy { criminals.associateBy { it.hash } }
}
