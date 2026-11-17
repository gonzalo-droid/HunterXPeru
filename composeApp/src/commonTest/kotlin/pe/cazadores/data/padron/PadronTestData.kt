package pe.cazadores.data.padron

import pe.cazadores.data.local.PadronDao
import pe.cazadores.data.local.PadronImageEntity
import pe.cazadores.data.local.PadronMetaEntity

/** Arma un zip sin compresión (método 0), como lo publica `sync_requisitoriados.py`. */
internal fun storedZip(entries: Map<String, ByteArray>): ByteArray {
    val out = ArrayList<Byte>()
    fun u16(v: Int) { out += (v and 0xFF).toByte(); out += (v shr 8 and 0xFF).toByte() }
    fun u32(v: Long) { u16((v and 0xFFFF).toInt()); u16((v shr 16 and 0xFFFF).toInt()) }
    val central = ArrayList<Byte>()
    fun c16(v: Int) { central += (v and 0xFF).toByte(); central += (v shr 8 and 0xFF).toByte() }
    fun c32(v: Long) { c16((v and 0xFFFF).toInt()); c16((v shr 16 and 0xFFFF).toInt()) }
    entries.forEach { (name, data) ->
        val offset = out.size.toLong()
        val nameBytes = name.encodeToByteArray()
        u32(0x04034b50); u16(20); u16(0); u16(0); u16(0); u16(0); u32(0)
        u32(data.size.toLong()); u32(data.size.toLong()); u16(nameBytes.size); u16(0)
        out.addAll(nameBytes.toList()); out.addAll(data.toList())
        c32(0x02014b50); c16(20); c16(20); c16(0); c16(0); c16(0); c16(0); c32(0)
        c32(data.size.toLong()); c32(data.size.toLong()); c16(nameBytes.size); c16(0); c16(0); c16(0); c16(0); c32(0)
        c32(offset); central.addAll(nameBytes.toList())
    }
    val centralOffset = out.size.toLong()
    out.addAll(central)
    u32(0x06054b50); u16(0); u16(0); u16(entries.size); u16(entries.size)
    u32(central.size.toLong()); u32(centralOffset); u16(0)
    return out.toByteArray()
}

internal fun manifestJson(version: Int, vararg records: String, thumbsZip: String? = "data/thumbs-v$version.zip") = """
    {"schema":1,"version":$version,"generatedAt":"2026-11-10T15:00:00Z","count":${records.size},
     ${thumbsZip?.let { "\"thumbsZip\":\"$it\"," } ?: ""}
     "criminals":[${records.joinToString(",")}]}
""".trimIndent()

internal fun record(hash: String, reward: Double = 50_000.0, name: String = "perez rojas, juan") =
    """{"id":${hash.hashCode() and 0xFFFF},"hash":"$hash","nombreCompleto":"$name","montoRecompensa":$reward,
        "delitos":["extorsion"],"departamento":"lima","provincia":"lima",
        "thumb":"thumbs/$hash.jpg","photo":"photos/$hash.jpg"}"""

internal class FakePadronDao : PadronDao {
    var meta: PadronMetaEntity? = null
    val images = LinkedHashMap<String, PadronImageEntity>()

    override suspend fun meta() = meta
    override suspend fun upsertMeta(meta: PadronMetaEntity) { this.meta = meta }
    override suspend fun markChecked(checkedAt: Long) { meta = meta?.copy(checkedAt = checkedAt) }
    override suspend fun image(path: String) = images[path]
    override suspend fun imagePaths(prefix: String) = images.keys.filter { it.startsWith(prefix) }
    override suspend fun upsertImages(images: List<PadronImageEntity>) { images.forEach { this.images[it.path] = it } }
    override suspend fun deleteImages(paths: List<String>) { paths.forEach { images.remove(it) } }
    override suspend fun photosBeyond(keep: Int) =
        images.values.filter { it.path.startsWith("photos/") }.sortedByDescending { it.savedAt }.drop(keep).map { it.path }
}
