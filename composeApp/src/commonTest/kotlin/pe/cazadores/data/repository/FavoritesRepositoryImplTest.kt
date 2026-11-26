package pe.cazadores.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import pe.cazadores.data.local.FavoriteDao
import pe.cazadores.data.local.FavoriteEntity
import pe.cazadores.data.padron.Padron
import pe.cazadores.data.remote.ApiJson
import pe.cazadores.domain.repository.TimeProvider
import pe.cazadores.testing.criminal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoritesRepositoryImplTest {
    private val dao = FakeFavoriteDao()
    private var now = 100L
    private val repo = FavoritesRepositoryImpl(dao, TimeProvider { now }, ApiJson)

    private fun padron(vararg people: pe.cazadores.domain.model.Criminal) = Padron(version = 2, criminals = people.toList(), generatedAtMillis = 0)

    @Test fun newPadronUpdatesRewardAndKeepsSavedAt() = runTest {
        repo.save(criminal("A"))
        now = 500
        repo.reconcile(padron(criminal("A").copy(reward = 80_000.0, crimes = listOf("SICARIATO"))))
        val saved = repo.observeAll().first().single()
        assertEquals(80_000.0, saved.reward)
        assertEquals(listOf("SICARIATO"), saved.crimes)
        assertEquals(100L, dao.rows.getValue("A").savedAt)
        assertFalse(saved.delisted)
    }

    @Test fun personMissingFromPadronIsMarkedNotDeleted() = runTest {
        repo.save(criminal("A"))
        now = 500
        repo.reconcile(padron(criminal("B")))
        val saved = repo.observeAll().first().single()
        assertTrue(saved.delisted)
        assertEquals(500L, dao.rows.getValue("A").delistedAt)
        // Una segunda carga no cambia la fecha en que dejó de figurar.
        now = 900
        repo.reconcile(padron(criminal("B")))
        assertEquals(500L, dao.rows.getValue("A").delistedAt)
    }

    @Test fun personBackInPadronLosesTheMark() = runTest {
        repo.save(criminal("A"))
        repo.reconcile(padron())
        repo.reconcile(padron(criminal("A")))
        assertFalse(repo.observeAll().first().single().delisted)
    }

    @Test fun undoAfterRemovingDelistedKeepsTheMark() = runTest {
        repo.save(criminal("A"))
        repo.reconcile(padron())
        val delisted = repo.observeAll().first().single()
        repo.remove("A")
        repo.save(delisted)
        assertTrue(repo.observeAll().first().single().delisted)
    }

    @Test fun unchangedFavoritesAreNotRewritten() = runTest {
        repo.save(criminal("A"))
        val writes = dao.writes
        repo.reconcile(padron(criminal("A")))
        assertEquals(writes, dao.writes)
    }
}

private class FakeFavoriteDao : FavoriteDao {
    private val state = MutableStateFlow<Map<String, FavoriteEntity>>(emptyMap())
    val rows: Map<String, FavoriteEntity> get() = state.value
    var writes = 0

    override fun observeAll(): Flow<List<FavoriteEntity>> = state.map { it.values.sortedByDescending { e -> e.savedAt } }
    override fun observeIsFavorite(hash: String): Flow<Boolean> = state.map { hash in it }
    override suspend fun all(): List<FavoriteEntity> = state.value.values.toList()
    override suspend fun get(hash: String): FavoriteEntity? = state.value[hash]
    override suspend fun upsert(entity: FavoriteEntity) {
        writes++
        state.value = state.value + (entity.hash to entity)
    }
    override suspend fun delete(hash: String) {
        state.value = state.value - hash
    }
}
