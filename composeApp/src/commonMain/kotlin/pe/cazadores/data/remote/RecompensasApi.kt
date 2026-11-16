package pe.cazadores.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.json.JsonObject
import pe.cazadores.data.remote.dto.CriminalDetailDto
import pe.cazadores.data.remote.dto.CriminalSummaryDto
import pe.cazadores.data.remote.dto.DelitoDto
import pe.cazadores.data.remote.dto.PageDto
import pe.cazadores.data.remote.dto.SearchRequestDto
import pe.cazadores.data.remote.dto.UbigeoDto

/** Endpoints de la API pública. Lanza excepciones de Ktor; el repositorio las traduce a [pe.cazadores.core.result.DataError]. */
class RecompensasApi(private val client: HttpClient) {

    suspend fun search(request: SearchRequestDto): PageDto<CriminalSummaryDto> =
        client.post("requisitoriados/pageandfilter") { setBody(request) }.body()

    suspend fun top5(): List<CriminalSummaryDto> =
        client.post("requisitoriados/top5") { setBody(JsonObject(emptyMap())) }.body()

    suspend fun detail(hash: String): CriminalDetailDto =
        client.get("requisitoriados/${hash.encodeURLPathPart()}").body()

    suspend fun delitos(): List<DelitoDto> = client.get("delitos").body()

    suspend fun departamentos(): List<UbigeoDto> = client.get("ubigeo/departamentos").body()

    suspend fun provincias(departamentoId: String): List<UbigeoDto> =
        client.get("ubigeo/provincias/departamento/${departamentoId.encodeURLPathPart()}").body()
}
