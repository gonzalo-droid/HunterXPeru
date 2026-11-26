package pe.cazadores.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.cazadores.ads.AD_EVERY_N_ITEMS
import pe.cazadores.ads.AdPlacement
import pe.cazadores.ads.InlineAdSlot
import pe.cazadores.core.config.AppConfig
import pe.cazadores.domain.model.SortOrder
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.clear_filters
import pe.cazadores.resources.empty_search_body
import pe.cazadores.resources.empty_search_body_no_query
import pe.cazadores.resources.empty_search_title
import pe.cazadores.resources.end_of_list
import pe.cazadores.resources.load_more_error
import pe.cazadores.resources.retry
import pe.cazadores.ui.components.CriminalCard
import pe.cazadores.ui.components.ErrorState
import pe.cazadores.ui.components.ListSkeleton
import pe.cazadores.ui.components.MessageState
import pe.cazadores.ui.components.SecondaryButton
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

@Composable
fun SearchRoute(
    focusSearch: Boolean,
    sortByReward: Boolean,
    onOpenDetail: (String) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
    actions: ExternalActions = koinInject(),
) {
    // "Ver todos" del tablero Clase S: lista por mayor recompensa, sin abrir el teclado.
    // Aplicar una sola vez por entrada: al volver de una ficha no debe pisar el orden elegido a mano.
    var sortHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(sortByReward) {
        if (sortByReward && !sortHandled) {
            viewModel.onSortChange(SortOrder.REWARD)
            sortHandled = true
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        focusSearch = focusSearch,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onOpenFilters = viewModel::onOpenFilters,
        onSortChange = viewModel::onSortChange,
        onRemoveDepartment = viewModel::onRemoveDepartment,
        onRemoveProvince = viewModel::onRemoveProvince,
        onRemoveCrime = viewModel::onRemoveCrime,
        onClearFilters = viewModel::onClearFilters,
        onRetry = viewModel::onRetry,
        onCall = { actions.dial(AppConfig.REPORT_PHONE_URI) },
        onRefresh = viewModel::onRefresh,
        onLoadMore = viewModel::onLoadMore,
        onOpenDetail = onOpenDetail,
    )
    state.draft?.let { draft ->
        FiltersSheet(
            draft = draft,
            count = state.draftCount,
            catalogs = state.catalogs,
            onChange = viewModel::onDraftChange,
            onApply = viewModel::onApplyDraft,
            onDismiss = viewModel::onDismissFilters,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    focusSearch: Boolean,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onOpenFilters: () -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onRemoveDepartment: () -> Unit,
    onRemoveProvince: () -> Unit,
    onRemoveCrime: () -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onCall: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LoadMoreEffect(listState, state.items.size, onLoadMore)

    Column(Modifier.fillMaxSize().background(Cz.colors.bg).statusBarsPadding()) {
        SearchHeader(state, focusSearch, onQueryChange, onClearQuery, onOpenFilters, onSortChange, onRemoveDepartment, onRemoveProvince, onRemoveCrime)

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.isLoading -> ListSkeleton(modifier = Modifier.padding(horizontal = CazadoresDimens.screenPadding))
                state.error != null -> ErrorState(state.error, onRetry, onCall)
                state.isEmpty -> MessageState(
                    icon = CzIcons.Search,
                    title = stringResource(Res.string.empty_search_title),
                    body = if (state.filters.query.isNotBlank()) {
                        stringResource(Res.string.empty_search_body, state.filters.query)
                    } else {
                        stringResource(Res.string.empty_search_body_no_query)
                    },
                    action = if (state.filters.activeCount > 0 || state.filters.query.isNotBlank()) {
                        {
                            SecondaryButton(stringResource(Res.string.clear_filters), onClick = {
                                onClearQuery()
                                onClearFilters()
                            })
                        }
                    } else {
                        null
                    },
                )
                else -> ResultsList(state, listState, onOpenDetail, onLoadMore)
            }
        }
    }
}

@Composable
private fun LoadMoreEffect(listState: LazyListState, itemCount: Int, onLoadMore: () -> Unit) {
    LaunchedEffect(listState, itemCount) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .filter { last -> itemCount > 0 && last >= listState.layoutInfo.totalItemsCount - SearchViewModel.LOAD_MORE_THRESHOLD }
            .collect { onLoadMore() }
    }
}

@Composable
private fun ResultsList(
    state: SearchUiState,
    listState: LazyListState,
    onOpenDetail: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = CazadoresDimens.screenPadding, end = CazadoresDimens.screenPadding, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(state.items, key = { _, c -> c.hash }) { index, criminal ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CriminalCard(criminal, onClick = { onOpenDetail(criminal.hash) })
                if ((index + 1) % AD_EVERY_N_ITEMS == 0) InlineAdSlot(AdPlacement.SEARCH_INLINE)
            }
        }
        item(key = "footer") { ListFooter(state, onLoadMore) }
    }
}

@Composable
private fun ListFooter(state: SearchUiState, onLoadMore: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        when {
            state.isLoadingMore -> CircularProgressIndicator(color = Cz.colors.ink, modifier = Modifier.size(28.dp))
            state.loadMoreFailed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(Res.string.load_more_error), style = Cz.type.bodySmall, color = Cz.colors.inkMuted)
                TextButton(onClick = onLoadMore) { Text(stringResource(Res.string.retry), style = Cz.type.bodyStrong, color = Cz.colors.ink) }
            }
            state.endReached && state.items.size > AD_EVERY_N_ITEMS ->
                Text(stringResource(Res.string.end_of_list), style = Cz.type.caption, color = Cz.colors.inkMuted)
        }
    }
}

