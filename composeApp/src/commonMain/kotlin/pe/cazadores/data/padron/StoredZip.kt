package pe.cazadores.data.padron

/**
 * Lee un zip **sin compresión** (método 0, "stored"), que es como `sync_requisitoriados.py` arma
 * `thumbs-vN.zip`: los JPEG ya vienen comprimidos. Usa el directorio central, así que los tamaños
 * son fiables aunque la cabecera local no los traiga. Entradas comprimidas o dañadas se saltan.
 */
internal object StoredZip {
    private const val END_OF_CENTRAL_DIR = 0x06054b50
    private const val CENTRAL_ENTRY = 0x02014b50
    private const val LOCAL_HEADER = 0x04034b50

    fun read(bytes: ByteArray): Map<String, ByteArray> {
        val end = findEndOfCentralDirectory(bytes) ?: return emptyMap()
        val entries = bytes.u16(end + 10)
        var offset = bytes.u32(end + 16).toInt()
        val result = LinkedHashMap<String, ByteArray>(entries)
        repeat(entries) {
            if (offset + 46 > bytes.size || bytes.u32(offset) != CENTRAL_ENTRY.toLong()) return result
            val method = bytes.u16(offset + 10)
            val size = bytes.u32(offset + 20).toInt()
            val nameLength = bytes.u16(offset + 28)
            val extraLength = bytes.u16(offset + 30)
            val commentLength = bytes.u16(offset + 32)
            val localOffset = bytes.u32(offset + 42).toInt()
            val name = bytes.decodeToString(offset + 46, offset + 46 + nameLength)
            offset += 46 + nameLength + extraLength + commentLength

            if (method != 0 || name.endsWith("/")) return@repeat
            if (localOffset + 30 > bytes.size || bytes.u32(localOffset) != LOCAL_HEADER.toLong()) return@repeat
            val dataStart = localOffset + 30 + bytes.u16(localOffset + 26) + bytes.u16(localOffset + 28)
            if (size <= 0 || dataStart + size > bytes.size) return@repeat
            result[name] = bytes.copyOfRange(dataStart, dataStart + size)
        }
        return result
    }

    private fun findEndOfCentralDirectory(bytes: ByteArray): Int? {
        // El registro final mide 22 bytes más un comentario de hasta 64 KB.
        val lowest = maxOf(0, bytes.size - 22 - 0xFFFF)
        for (i in bytes.size - 22 downTo lowest) {
            if (bytes.u32(i) == END_OF_CENTRAL_DIR.toLong()) return i
        }
        return null
    }

    private fun ByteArray.u16(at: Int): Int = (this[at].toInt() and 0xFF) or ((this[at + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.u32(at: Int): Long = (u16(at).toLong()) or (u16(at + 2).toLong() shl 16)
}
