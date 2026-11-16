package pe.cazadores.data.local

import androidx.room.Room
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import pe.cazadores.data.remote.ApiJson
import pe.cazadores.data.repository.FavoritesRepositoryImpl
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.repository.TimeProvider
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Room real (driver empaquetado) en memoria. */
class AppDatabaseTest {
    private val db = Room.inMemoryDatabaseBuilder<AppDatabase>().buildCazadoresDatabase()
    private var now = 1_000L
    private val favorites = FavoritesRepositoryImpl(db.favoriteDao(), TimeProvider { now }, ApiJson)

    private fun criminal(hash: String, complete: Boolean = false) = Criminal(
        id = 1, hash = hash, displayName = "ATACHI FELIX, JOSIMAR", reward = 50_000.0,
        crimes = listOf("HOMICIDIO CALIFICADO", "SICARIATO"),
        departamento = if (complete) "LIMA" else null,
        nombres = if (complete) "JOSIMAR" else null,
    )

    @AfterTest fun close() = db.close()

    @Test fun favoriteRoundTripKeepsDisplayNameAndCrimes() = runTest {
        favorites.save(criminal("A"))
        favorites.observeAll().test {
            val saved = awaitItem().single()
            assertEquals("ATACHI FELIX, JOSIMAR", saved.displayName)
            assertEquals(listOf("HOMICIDIO CALIFICADO", "SICARIATO"), saved.crimes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun completingFavoriteKeepsOriginalSavedAt() = runTest {
        favorites.save(criminal("A"))
        now = 5_000L
        favorites.save(criminal("B"))
        now = 9_000L
        favorites.save(criminal("A", complete = true))
        favorites.observeAll().test {
            val list = awaitItem()
            assertEquals(listOf("B", "A"), list.map { it.hash }, "Orden por fecha de guardado, no de actualización")
            assertEquals("LIMA", list.last().departamento)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun isFavoriteIsReactive() = runTest {
        favorites.observeIsFavorite("A").test {
            assertFalse(awaitItem())
            favorites.save(criminal("A"))
            assertTrue(awaitItem())
            favorites.remove("A")
            assertFalse(awaitItem())
        }
    }

    @Test fun samePersonInTwoSearchesDoesNotCollide() = runTest {
        val dao = db.cacheDao()
        fun entry(key: String) = SearchCacheEntity(key, "A", 0, "{}", now)
        dao.replaceSearch("k1", listOf(entry("k1")), SearchCacheMetaEntity("k1", 1, 1, now), expiredBefore = 0, maxKeys = 20)
        dao.replaceSearch("k2", listOf(entry("k2")), SearchCacheMetaEntity("k2", 1, 1, now), expiredBefore = 0, maxKeys = 20)
        assertEquals(2, dao.searchRowCount())
        assertEquals(1, dao.searchEntries("k1").size)
    }

    @Test fun replaceSearchPrunesExpiredAndOldestKeys() = runTest {
        val dao = db.cacheDao()
        listOf("old" to 10L, "k1" to 100L, "k2" to 200L).forEach { (key, at) ->
            dao.replaceSearch(key, listOf(SearchCacheEntity(key, "H$key", 0, "{}", at)), SearchCacheMetaEntity(key, 1, 1, at), expiredBefore = 0, maxKeys = 20)
        }
        dao.replaceSearch("k3", listOf(SearchCacheEntity("k3", "H", 0, "{}", 300)), SearchCacheMetaEntity("k3", 1, 1, 300), expiredBefore = 50, maxKeys = 2)
        assertEquals(null, dao.searchMeta("old"), "TTL vencido")
        assertEquals(null, dao.searchMeta("k1"), "Solo se conservan las 2 búsquedas más recientes")
        assertEquals(2, dao.searchRowCount())
    }
}
