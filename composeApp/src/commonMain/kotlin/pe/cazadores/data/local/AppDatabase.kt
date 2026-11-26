package pe.cazadores.data.local

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        FavoriteEntity::class,
        SearchCacheEntity::class,
        SearchCacheMetaEntity::class,
        DetailCacheEntity::class,
        CatalogCacheEntity::class,
        PadronMetaEntity::class,
        PadronImageEntity::class,
    ],
    // v2: padrón de Firebase Storage (solo tablas nuevas). v3: `favorites.delistedAt` (columna nullable).
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun cacheDao(): CacheDao
    abstract fun padronDao(): PadronDao

    companion object {
        const val FILE_NAME = "cazadores.db"
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/** Configuración común: driver SQLite empaquetado y consultas en IO. */
fun RoomDatabase.Builder<AppDatabase>.buildCazadoresDatabase(): AppDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
