package pe.cazadores.domain.model

/**
 * Persona requisitoriada. [displayName] se calcula una sola vez al mapear y **siempre se persiste**:
 * la app Flutter lo reconstruía desde apellidos vacíos y mostraba nombres en blanco (bug B3).
 */
data class Criminal(
    val id: Int,
    val hash: String,
    val displayName: String,
    val apellidoPaterno: String? = null,
    val apellidoMaterno: String? = null,
    val nombres: String? = null,
    val sex: Sex? = null,
    val reward: Double,
    val crimes: List<String>,
    val photoBase64: String? = null,
    val departamento: String? = null,
    val provincia: String? = null,
    /** Rutas en Firebase Storage (solo si viene del padrón): miniatura de 240 px y foto de 720 px. */
    val thumbKey: String? = null,
    val photoKey: String? = null,
) {
    /** Primer delito: el que muestran las tarjetas. */
    val mainCrime: String get() = crimes.firstOrNull().orEmpty()

    /** Viene del detalle (tiene apellidos y ubicación) y no solo de la lista. */
    val isComplete: Boolean get() = !departamento.isNullOrBlank() || !nombres.isNullOrBlank()
}

/** La API manda 1 = masculino; 2 se asume femenino (no hay casos verificados con otro valor). */
enum class Sex { MALE, FEMALE }

enum class DangerLevel(val marks: Int) { EXTREMO(3), MUY_ALTO(2), ALTO(1) }

/** Rango de recompensa calculado por la app (no oficial): más estrellas, mayor recompensa. */
enum class RewardClass(val stars: Int) { S(4), A(3), B(2), C(1) }

enum class SortOrder { RECENT, REWARD, REGION }

data class SearchFilters(
    val query: String = "",
    val departamentoId: String? = null,
    val provinciaId: String? = null,
    val delitoId: Int? = null,
    /** Rango de recompensa calculado por la app; la API no lo filtra (solo el padrón local). */
    val rewardClass: RewardClass? = null,
    val sort: SortOrder = SortOrder.RECENT,
) {
    /** Filtros de la hoja (sin contar el texto ni el orden). */
    val activeCount: Int get() = listOfNotNull(departamentoId, provinciaId, delitoId, rewardClass).size

    /** Clave estable para la caché offline: una persona puede estar en varias búsquedas. */
    val cacheKey: String
        get() = listOf(query.trim().lowercase(), departamentoId, provinciaId, delitoId, rewardClass, sort.name)
            .joinToString("|") { it?.toString().orEmpty() }
}

/** Página 1-indexed (como la ve la UI). */
data class SearchPage(
    val items: List<Criminal>,
    val page: Int,
    val totalElements: Int,
    val totalPages: Int,
    val isLast: Boolean,
)

data class HomeStats(val total: Int, val maxReward: Double)

/**
 * Lugar de una recompensa en el padrón nacional (1 = la más alta). [tiedWith] es cuántas personas
 * más tienen el mismo monto: varios comparten S/ 500 000 y "#1" a secas engañaría (D16).
 */
data class RewardPosition(val rank: Int, val tiedWith: Int, val total: Int) {
    /** "Top X %" redondeado hacia arriba y nunca 0. */
    val topPercent: Int get() = maxOf(1, (rank * 100 + total - 1) / total)
}

data class Delito(val id: Int, val name: String)
data class Departamento(val id: String, val name: String)
data class Provincia(val departamentoId: String, val id: String, val name: String)

data class Catalogs(
    val delitos: List<Delito>,
    val departamentos: List<Departamento>,
    val provincias: List<Provincia>,
) {
    fun provinciasOf(departamentoId: String?): List<Provincia> =
        if (departamentoId == null) emptyList() else provincias.filter { it.departamentoId == departamentoId }
}
