package pe.cazadores.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE hash = :hash)")
    fun observeIsFavorite(hash: String): Flow<Boolean>

    @Query("SELECT * FROM favorites WHERE hash = :hash")
    suspend fun get(hash: String): FavoriteEntity?

    @Upsert
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE hash = :hash")
    suspend fun delete(hash: String)
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM search_cache WHERE searchKey = :key ORDER BY position")
    suspend fun searchEntries(key: String): List<SearchCacheEntity>

    @Query("SELECT * FROM search_cache_meta WHERE searchKey = :key")
    suspend fun searchMeta(key: String): SearchCacheMetaEntity?

    @Upsert
    suspend fun upsertSearchEntries(entries: List<SearchCacheEntity>)

    @Upsert
    suspend fun upsertSearchMeta(meta: SearchCacheMetaEntity)

    @Query("DELETE FROM search_cache WHERE searchKey = :key")
    suspend fun deleteSearch(key: String)

    @Query("DELETE FROM search_cache_meta WHERE searchKey = :key")
    suspend fun deleteSearchMeta(key: String)

    @Query("DELETE FROM search_cache WHERE cachedAt < :olderThan")
    suspend fun deleteSearchOlderThan(olderThan: Long)

    @Query("DELETE FROM search_cache_meta WHERE cachedAt < :olderThan")
    suspend fun deleteSearchMetaOlderThan(olderThan: Long)

    /** Claves más antiguas que exceden el máximo de búsquedas guardadas. */
    @Query("SELECT searchKey FROM search_cache_meta ORDER BY cachedAt DESC LIMIT -1 OFFSET :keep")
    suspend fun searchKeysBeyond(keep: Int): List<String>

    @Query("SELECT COUNT(*) FROM search_cache")
    suspend fun searchRowCount(): Int

    /** Reemplaza la primera página de una búsqueda y poda lo viejo, todo en una transacción. */
    @Transaction
    suspend fun replaceSearch(
        key: String,
        entries: List<SearchCacheEntity>,
        meta: SearchCacheMetaEntity,
        expiredBefore: Long,
        maxKeys: Int,
    ) {
        deleteSearchOlderThan(expiredBefore)
        deleteSearchMetaOlderThan(expiredBefore)
        deleteSearch(key)
        upsertSearchEntries(entries)
        upsertSearchMeta(meta)
        searchKeysBeyond(maxKeys).forEach {
            deleteSearch(it)
            deleteSearchMeta(it)
        }
    }

    @Query("SELECT * FROM detail_cache WHERE hash = :hash")
    suspend fun detail(hash: String): DetailCacheEntity?

    @Upsert
    suspend fun upsertDetail(entity: DetailCacheEntity)

    @Query("DELETE FROM detail_cache WHERE cachedAt < :olderThan")
    suspend fun deleteDetailsOlderThan(olderThan: Long)

    @Query("SELECT * FROM catalog_cache WHERE `key` = :key")
    suspend fun catalog(key: String): CatalogCacheEntity?

    @Upsert
    suspend fun upsertCatalog(entity: CatalogCacheEntity)
}

@Dao
interface PadronDao {
    @Query("SELECT * FROM padron_meta WHERE id = 0")
    suspend fun meta(): PadronMetaEntity?

    @Upsert
    suspend fun upsertMeta(meta: PadronMetaEntity)

    @Query("UPDATE padron_meta SET checkedAt = :checkedAt WHERE id = 0")
    suspend fun markChecked(checkedAt: Long)

    @Query("SELECT * FROM padron_image WHERE path = :path")
    suspend fun image(path: String): PadronImageEntity?

    /** Emite de nuevo cada vez que cambia la tabla: una imagen que faltaba aparece al guardarse. */
    @Query("SELECT * FROM padron_image WHERE path = :path")
    fun observeImage(path: String): Flow<PadronImageEntity?>

    @Query("SELECT path FROM padron_image WHERE path LIKE :prefix || '%'")
    suspend fun imagePaths(prefix: String): List<String>

    @Upsert
    suspend fun upsertImages(images: List<PadronImageEntity>)

    @Query("DELETE FROM padron_image WHERE path IN (:paths)")
    suspend fun deleteImages(paths: List<String>)

    /** Fotos de ficha más antiguas que exceden el máximo guardado (las miniaturas no se podan así). */
    @Query("SELECT path FROM padron_image WHERE path LIKE 'photos/%' ORDER BY savedAt DESC LIMIT -1 OFFSET :keep")
    suspend fun photosBeyond(keep: Int): List<String>
}
