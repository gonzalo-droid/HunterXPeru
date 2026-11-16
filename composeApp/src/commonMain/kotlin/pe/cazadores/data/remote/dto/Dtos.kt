package pe.cazadores.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Contratos de la API del Programa de Recompensas, verificados con llamadas reales el 2026-11-16.
 * Todos los campos tienen valor por defecto: la API omite o cambia campos sin aviso.
 */
@Serializable
data class SearchRequestDto(
    val pageInfo: PageInfoDto,
    val search: SearchDto = SearchDto(),
)

/**
 * `page` es **1-indexed en la petición** (page 0 → HTTP 500 "Page index must not be less than zero"),
 * pero la respuesta trae `number` **0-indexed**. Ver [pe.cazadores.data.repository.PageTranslation].
 */
@Serializable
data class PageInfoDto(
    val page: Int,
    val size: Int,
    val sortBy: String,
    val direction: String,
)

/**
 * Filtros. Verificado: `nombreCompleto` (no distingue mayúsculas), `idDepartamento` "15",
 * `idProvincia` "01" (junto con el departamento) e `idDelito`.
 * `sexo` y `alias` devuelven siempre 0 resultados con cualquier formato: no se exponen en la UI.
 */
@Serializable
data class SearchDto(
    val nombreCompleto: String? = null,
    val tipoFilter: String = "F",
    val alias: String? = null,
    val idDepartamento: String? = null,
    val idProvincia: String? = null,
    val idDelito: Int? = null,
    val delito: String? = null,
    val sexo: String? = null,
)

@Serializable
data class PageDto<T>(
    val content: List<T> = emptyList(),
    val totalElements: Int = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val size: Int = 0,
    val last: Boolean = true,
    val first: Boolean = true,
    val numberOfElements: Int = 0,
    val empty: Boolean = true,
)

@Serializable
data class CriminalSummaryDto(
    val id: Int? = null,
    val idRequisitoriado: Int = 0,
    val hashRequisitoriado: String = "",
    val nombreCompleto: String = "",
    /** Base64 (JPEG o PNG), a veces con prefijo `data:image/...;base64,`. */
    val foto: String? = null,
    val montoRecompensa: Double = 0.0,
    val montoRecompensaSpace: String = "",
    val delito: String = "",
)

@Serializable
data class CriminalDetailDto(
    val idRequisitoriado: Int = 0,
    val hashRequisitoriado: String = "",
    val apellidoPaterno: String = "",
    val apellidoMaterno: String = "",
    val nombres: String = "",
    val sexo: Int = 1,
    val montoRecompensa: Double = 0.0,
    val montoRecompensaSpace: String = "",
    val foto: String? = null,
    val delitos: List<String> = emptyList(),
    val departamento: String = "",
    val provincia: String = "",
)

@Serializable
data class DelitoDto(
    val idDelito: Int = 0,
    val descripcion: String = "",
)

/** Departamento: provincia "00". Provincia: el código de 2 dígitos dentro del departamento. */
@Serializable
data class UbigeoDto(
    val departamento: String = "",
    val provincia: String = "",
    val descripcion: String = "",
)
