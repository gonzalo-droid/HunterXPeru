package pe.cazadores.data.padron

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StoredZipTest {
    @Test fun readsStoredEntries() {
        val zip = storedZip(mapOf("thumbs/a.jpg" to byteArrayOf(1, 2, 3), "thumbs/b.jpg" to ByteArray(70_000) { it.toByte() }))
        val entries = StoredZip.read(zip)
        assertEquals(setOf("thumbs/a.jpg", "thumbs/b.jpg"), entries.keys)
        assertContentEquals(byteArrayOf(1, 2, 3), entries["thumbs/a.jpg"])
        assertEquals(70_000, entries["thumbs/b.jpg"]?.size)
    }

    @Test fun garbageOrTruncatedGivesWhatItCan() {
        assertTrue(StoredZip.read(byteArrayOf(1, 2, 3)).isEmpty())
        val zip = storedZip(mapOf("thumbs/a.jpg" to byteArrayOf(9)))
        assertTrue(StoredZip.read(zip.copyOf(zip.size - 5)).isEmpty(), "Sin registro final no hay directorio que leer")
    }
}
