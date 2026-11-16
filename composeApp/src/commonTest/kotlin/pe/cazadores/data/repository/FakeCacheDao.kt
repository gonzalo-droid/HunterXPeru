package pe.cazadores.data.repository

import pe.cazadores.data.local.CacheDao
import pe.cazadores.data.local.CatalogCacheEntity
import pe.cazadores.data.local.DetailCacheEntity
import pe.cazadores.data.local.SearchCacheEntity
import pe.cazadores.data.local.SearchCacheMetaEntity

/** CacheDao en memoria para probar repositorios en la JVM (Room real: AppDatabaseTest en iOS). */
class FakeCacheDao : CacheDao {
    val search = mutableMapOf<Pair<String, String>, SearchCacheEntity>()
    val meta = mutableMapOf<String, SearchCacheMetaEntity>()
    val details = mutableMapOf<String, DetailCacheEntity>()
    val catalogs = mutableMapOf<String, CatalogCacheEntity>()

    override suspend fun searchEntries(key: String) = search.values.filter { it.searchKey == key }.sortedBy { it.position }
    override suspend fun searchMeta(key: String) = meta[key]
    override suspend fun upsertSearchEntries(entries: List<SearchCacheEntity>) = entries.forEach { search[it.searchKey to it.hash] = it }
    override suspend fun upsertSearchMeta(meta: SearchCacheMetaEntity) { this.meta[meta.searchKey] = meta }
    override suspend fun deleteSearch(key: String) { search.keys.removeAll { it.first == key } }
    override suspend fun deleteSearchMeta(key: String) { meta.remove(key) }
    override suspend fun deleteSearchOlderThan(olderThan: Long) { search.values.removeAll { it.cachedAt < olderThan } }
    override suspend fun deleteSearchMetaOlderThan(olderThan: Long) { meta.values.removeAll { it.cachedAt < olderThan } }
    override suspend fun searchKeysBeyond(keep: Int) = meta.values.sortedByDescending { it.cachedAt }.drop(keep).map { it.searchKey }
    override suspend fun searchRowCount() = search.size
    override suspend fun detail(hash: String) = details[hash]
    override suspend fun upsertDetail(entity: DetailCacheEntity) { details[entity.hash] = entity }
    override suspend fun deleteDetailsOlderThan(olderThan: Long) { details.values.removeAll { it.cachedAt < olderThan } }
    override suspend fun catalog(key: String) = catalogs[key]
    override suspend fun upsertCatalog(entity: CatalogCacheEntity) { catalogs[entity.key] = entity }
}
