package pe.cazadores.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
/** Base de la versión 1 (como la dejó la app publicada) que se abre con la versión actual. */
class PadronMigrationTest {
    private val path = NSTemporaryDirectory() + "migration-test.db"

    @AfterTest fun clean() {
        listOf("", "-wal", "-shm", "-journal").forEach { NSFileManager.defaultManager.removeItemAtPath(path + it, null) }
    }

    private fun createVersion1() {
        clean()
        val connection = BundledSQLiteDriver().open(path)
        listOf(
            "CREATE TABLE IF NOT EXISTS `favorites` (`hash` TEXT NOT NULL, `id` INTEGER NOT NULL, `displayName` TEXT NOT NULL, `apellidoPaterno` TEXT, `apellidoMaterno` TEXT, `nombres` TEXT, `sex` TEXT, `reward` REAL NOT NULL, `crimesJson` TEXT NOT NULL, `photoBase64` TEXT, `departamento` TEXT, `provincia` TEXT, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`hash`))",
            "CREATE TABLE IF NOT EXISTS `search_cache` (`searchKey` TEXT NOT NULL, `hash` TEXT NOT NULL, `position` INTEGER NOT NULL, `json` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`searchKey`, `hash`))",
            "CREATE INDEX IF NOT EXISTS `index_search_cache_searchKey_position` ON `search_cache` (`searchKey`, `position`)",
            "CREATE TABLE IF NOT EXISTS `search_cache_meta` (`searchKey` TEXT NOT NULL, `totalElements` INTEGER NOT NULL, `totalPages` INTEGER NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`searchKey`))",
            "CREATE TABLE IF NOT EXISTS `detail_cache` (`hash` TEXT NOT NULL, `json` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`hash`))",
            "CREATE TABLE IF NOT EXISTS `catalog_cache` (`key` TEXT NOT NULL, `json` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '90cdfcc8c2674e045b7defd5542efb2d')",
            "PRAGMA user_version = 1",
            "INSERT INTO favorites VALUES ('A', 1, 'PEREZ, JUAN', NULL, NULL, NULL, NULL, 50000.0, '[\"EXTORSION\"]', NULL, 'LIMA', NULL, 7)",
        ).forEach { connection.execSQL(it) }
        connection.close()
    }

    @Test fun version1KeepsFavoritesAndGainsPadronTables() = runTest {
        createVersion1()
        val db = Room.databaseBuilder<AppDatabase>(name = path).buildCazadoresDatabase()
        try {
            assertEquals("PEREZ, JUAN", assertNotNull(db.favoriteDao().get("A")).displayName)
            val padron = db.padronDao()
            assertNull(padron.meta())
            padron.upsertMeta(PadronMetaEntity(version = 3, json = "{}", generatedAt = 1, checkedAt = 2))
            padron.markChecked(9)
            assertEquals(9, padron.meta()?.checkedAt)
        } finally {
            db.close()
        }
    }

    @Test fun padronImagesPruneOnlyOldPhotos() = runTest {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>().buildCazadoresDatabase()
        try {
            val padron = db.padronDao()
            padron.upsertImages(
                listOf(
                    PadronImageEntity("thumbs/a.jpg", byteArrayOf(1), savedAt = 1),
                    PadronImageEntity("photos/old.jpg", byteArrayOf(2), savedAt = 2),
                    PadronImageEntity("photos/new.jpg", byteArrayOf(3), savedAt = 3),
                ),
            )
            assertEquals(listOf("thumbs/a.jpg"), padron.imagePaths("thumbs/"))
            assertEquals(listOf("photos/old.jpg"), padron.photosBeyond(keep = 1))
            padron.deleteImages(listOf("photos/old.jpg"))
            assertNull(padron.image("photos/old.jpg"))
            assertEquals(3, padron.image("photos/new.jpg")?.bytes?.single()?.toInt())
        } finally {
            db.close()
        }
    }
}
