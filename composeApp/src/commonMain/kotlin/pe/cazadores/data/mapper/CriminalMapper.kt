package pe.cazadores.data.mapper

import pe.cazadores.data.remote.dto.CriminalDetailDto
import pe.cazadores.data.remote.dto.CriminalSummaryDto
import pe.cazadores.data.remote.dto.DelitoDto
import pe.cazadores.data.remote.dto.UbigeoDto
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.Delito
import pe.cazadores.domain.model.Departamento
import pe.cazadores.domain.model.Provincia
import pe.cazadores.domain.model.Sex

/**
 * `"PATERNO MATERNO, NOMBRES"` si hay apellidos; si no, `nombreCompleto` en mayúsculas.
 * La API a veces manda "." como apellido materno ("branko dragicevic ."): se descarta.
 */
fun buildDisplayName(
    apellidoPaterno: String?,
    apellidoMaterno: String?,
    nombres: String?,
    nombreCompleto: String?,
): String {
    val surnames = listOfNotNull(apellidoPaterno, apellidoMaterno)
        .map { it.cleanNamePart() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
    val names = nombres?.cleanNamePart().orEmpty()
    return when {
        surnames.isNotEmpty() && names.isNotEmpty() -> "$surnames, $names"
        surnames.isNotEmpty() -> surnames
        else -> nombreCompleto.orEmpty().cleanNamePart()
    }.uppercase()
}

private fun String.cleanNamePart(): String =
    split(' ').filter { it.isNotBlank() && it != "." }.joinToString(" ").trim()

fun CriminalSummaryDto.toDomain(): Criminal = Criminal(
    id = idRequisitoriado,
    hash = hashRequisitoriado,
    displayName = buildDisplayName(null, null, null, nombreCompleto),
    reward = montoRecompensa,
    crimes = listOf(delito).filter { it.isNotBlank() },
    photoBase64 = foto?.takeIf { it.isNotBlank() },
)

fun CriminalDetailDto.toDomain(): Criminal = Criminal(
    id = idRequisitoriado,
    hash = hashRequisitoriado,
    displayName = buildDisplayName(apellidoPaterno, apellidoMaterno, nombres, null),
    apellidoPaterno = apellidoPaterno.ifBlank { null },
    apellidoMaterno = apellidoMaterno.ifBlank { null },
    nombres = nombres.ifBlank { null },
    sex = when (sexo) {
        1 -> Sex.MALE
        2 -> Sex.FEMALE
        else -> null
    },
    reward = montoRecompensa,
    crimes = delitos.filter { it.isNotBlank() },
    photoBase64 = foto?.takeIf { it.isNotBlank() },
    departamento = departamento.ifBlank { null },
    provincia = provincia.ifBlank { null },
)

fun DelitoDto.toDomain() = Delito(id = idDelito, name = descripcion.trim())

fun UbigeoDto.toDepartamento() = Departamento(id = departamento, name = descripcion.trim())

fun UbigeoDto.toProvincia() = Provincia(departamentoId = departamento, id = provincia, name = descripcion.trim())
