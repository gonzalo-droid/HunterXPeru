package pe.cazadores.ui.screens

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.cazadores.core.result.AppResult
import pe.cazadores.core.result.DataError
import pe.cazadores.domain.model.SortOrder
import pe.cazadores.domain.repository.TimeProvider
import pe.cazadores.domain.usecase.GetCatalogsUseCase
import pe.cazadores.domain.usecase.GetCriminalDetailUseCase
import pe.cazadores.domain.usecase.GetHomeStatsUseCase
import pe.cazadores.domain.usecase.GetTopWantedUseCase
import pe.cazadores.domain.usecase.IsFavoriteUseCase
import pe.cazadores.domain.usecase.ObserveFavoritesUseCase
import pe.cazadores.domain.usecase.SearchCriminalsUseCase
import pe.cazadores.domain.usecase.ToggleFavoriteUseCase
import pe.cazadores.testing.FakeCatalogRepository
import pe.cazadores.testing.FakeCriminalRepository
import pe.cazadores.testing.FakeFavoritesRepository
import pe.cazadores.testing.criminal
import pe.cazadores.ui.screens.detail.DetailContent
import pe.cazadores.ui.screens.detail.DetailViewModel
import pe.cazadores.ui.screens.home.HomeViewModel
import pe.cazadores.ui.screens.saved.SavedViewModel
import pe.cazadores.ui.screens.search.SearchViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val criminals = FakeCriminalRepository()
    private val favorites = FakeFavoritesRepository()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.toggle() = ToggleFavoriteUseCase(favorites, criminals, backgroundScope)

    @Test fun savedRemoveThenUndoRestoresItem() = runTest(dispatcher) {
        val person = criminal("A", "TORRES, RODRIGO")
        favorites.save(person)
        val vm = SavedViewModel(ObserveFavoritesUseCase(favorites), toggle())
        vm.state.test {
            assertEquals(listOf("A"), awaitItem().let { if (it.isLoading) awaitItem() else it }.items.map { it.hash })
            vm.onRemove(person)
            assertTrue(awaitItem().items.isEmpty())
            vm.onUndoRemove(person)
            assertEquals("TORRES, RODRIGO", awaitItem().items.single().displayName)
        }
    }

    @Test fun searchDebouncesQueryAndPaginatesWithoutDuplicates() = runTest(dispatcher) {
        val vm = SearchViewModel(SearchCriminalsUseCase(criminals), GetCatalogsUseCase(FakeCatalogRepository()))
        advanceUntilIdle()
        assertEquals(20, vm.state.value.items.size)
        assertEquals(1, criminals.searchCalls.size)

        vm.onQueryChange("a")
        vm.onQueryChange("at")
        vm.onQueryChange("atachi")
        advanceTimeBy(SearchViewModel.QUERY_DEBOUNCE_MS - 1)
        assertEquals(1, criminals.searchCalls.size, "No busca mientras se escribe")
        advanceUntilIdle()
        assertEquals("atachi" to 1, criminals.searchCalls.last().first.query to criminals.searchCalls.last().second)

        vm.onLoadMore()
        advanceUntilIdle()
        assertEquals(2, criminals.searchCalls.last().second)
        assertEquals(40, vm.state.value.items.map { it.hash }.toSet().size)
    }

    @Test fun searchErrorThenRetryRecovers() = runTest(dispatcher) {
        criminals.searchResponder = { _, _ -> AppResult.Failure(DataError.Network) }
        val vm = SearchViewModel(SearchCriminalsUseCase(criminals), GetCatalogsUseCase(FakeCatalogRepository()))
        advanceUntilIdle()
        assertEquals(DataError.Network, vm.state.value.error)

        criminals.searchResponder = { _, page -> AppResult.Success(pe.cazadores.domain.model.SearchPage(listOf(criminal("x")), page, 1, 1, true)) }
        vm.onRetry()
        advanceUntilIdle()
        assertEquals(null, vm.state.value.error)
        assertTrue(vm.state.value.endReached)
    }

    @Test fun sortChangeReloadsFromFirstPage() = runTest(dispatcher) {
        val vm = SearchViewModel(SearchCriminalsUseCase(criminals), GetCatalogsUseCase(FakeCatalogRepository()))
        advanceUntilIdle()
        vm.onSortChange(SortOrder.REWARD)
        advanceUntilIdle()
        assertEquals(SortOrder.REWARD to 1, criminals.searchCalls.last().first.sort to criminals.searchCalls.last().second)
    }

    @Test fun detailNotFoundAndFavoriteToggle() = runTest(dispatcher) {
        criminals.detailResult = AppResult.Failure(DataError.NotFound)
        val notFound = DetailViewModel("X", GetCriminalDetailUseCase(criminals, favorites), IsFavoriteUseCase(favorites), toggle(), TimeProvider { 0 })
        notFound.state.test {
            skipItems(1)
            advanceUntilIdle()
            assertEquals(DetailContent.Error(DataError.NotFound), expectMostRecentItem().content)
        }

        criminals.detailResult = AppResult.Success(criminal("D", crimes = listOf("SICARIATO")))
        val vm = DetailViewModel("D", GetCriminalDetailUseCase(criminals, favorites), IsFavoriteUseCase(favorites), toggle(), TimeProvider { 0 })
        vm.state.test {
            advanceUntilIdle()
            val loaded = expectMostRecentItem()
            assertIs<DetailContent.Loaded>(loaded.content)
            assertEquals(pe.cazadores.domain.model.DangerLevel.EXTREMO, (loaded.content as DetailContent.Loaded).danger)
            vm.onToggleFavorite()
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().isFavorite)
        }
    }

    @Test fun homeKeepsTopWhenStatsFail() = runTest(dispatcher) {
        criminals.statsResult = AppResult.Failure(DataError.Timeout)
        val vm = HomeViewModel(GetTopWantedUseCase(criminals), GetHomeStatsUseCase(criminals), TimeProvider { 42 })
        advanceUntilIdle()
        with(vm.state.value) {
            assertEquals(2, top.size)
            assertEquals(null, stats)
            assertEquals(null, error)
            assertEquals(42L, updatedAtMillis)
        }
    }
}
