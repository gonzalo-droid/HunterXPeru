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
