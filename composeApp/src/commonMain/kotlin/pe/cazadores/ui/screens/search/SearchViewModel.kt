package pe.cazadores.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.cazadores.core.result.AppResult
import pe.cazadores.core.result.DataError
import pe.cazadores.domain.model.Catalogs
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.SearchFilters
import pe.cazadores.domain.model.SortOrder
import pe.cazadores.domain.usecase.GetCatalogsUseCase
import pe.cazadores.domain.usecase.SearchCriminalsUseCase

data class SearchUiState(
    /** Texto tal como lo escribe el usuario (la búsqueda usa `filters.query`, con debounce). */
    val queryInput: String = "",
    val filters: SearchFilters = SearchFilters(),
    val items: List<Criminal> = emptyList(),
    val totalElements: Int? = null,
    val page: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: DataError? = null,
    val loadMoreFailed: Boolean = false,
    val cachedAtMillis: Long? = null,
    val catalogs: Catalogs? = null,
    /** Borrador de la hoja de filtros y su conteo previo ("Ver N resultados"). */
    val draft: SearchFilters? = null,
    val draftCount: Int? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && items.isEmpty()
}

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val search: SearchCriminalsUseCase,
    private val getCatalogs: GetCatalogsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var draftJob: Job? = null

    init {
        _state.map { it.queryInput }
            .distinctUntilChanged()
            .drop(1)
            .debounce(QUERY_DEBOUNCE_MS)
            .onEach { query -> applyFilters(_state.value.filters.copy(query = query.trim())) }
            .launchIn(viewModelScope)

        viewModelScope.launch { _state.update { it.copy(catalogs = getCatalogs()) } }
        reload()
    }

    fun onQueryChange(text: String) = _state.update { it.copy(queryInput = text) }

    fun onClearQuery() {
        _state.update { it.copy(queryInput = "") }
        applyFilters(_state.value.filters.copy(query = ""))
    }

    fun onSortChange(sort: SortOrder) = applyFilters(_state.value.filters.copy(sort = sort))

    fun onRemoveDepartment() = applyFilters(_state.value.filters.copy(departamentoId = null, provinciaId = null))

    fun onRemoveProvince() = applyFilters(_state.value.filters.copy(provinciaId = null))

    fun onRemoveCrime() = applyFilters(_state.value.filters.copy(delitoId = null))

    fun onClearFilters() = applyFilters(SearchFilters(query = _state.value.filters.query, sort = _state.value.filters.sort))

    fun onRetry() = reload()

    fun onRefresh() = reload(refreshing = true)

    /** Se llama cuando el usuario llega a 5 ítems del final. */
    fun onLoadMore() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || s.endReached || s.error != null || s.cachedAtMillis != null) return
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
            when (val result = search(s.filters, s.page + 1)) {
                is AppResult.Success -> _state.update { current ->
                    val known = current.items.mapTo(HashSet()) { it.hash }
                    current.copy(
                        items = current.items + result.data.items.filterNot { it.hash in known },
                        page = result.data.page,
                        endReached = result.data.isLast,
                        totalElements = result.data.totalElements,
                        isLoadingMore = false,
                    )
                }
                is AppResult.Failure -> _state.update { it.copy(isLoadingMore = false, loadMoreFailed = true) }
            }
        }
    }

    // --- Hoja de filtros ---

    fun onOpenFilters() {
        _state.update { it.copy(draft = it.filters, draftCount = it.totalElements) }
    }

    fun onDraftChange(draft: SearchFilters) {
        _state.update { it.copy(draft = draft, draftCount = null) }
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            val result = search(draft, 1)
            if (result is AppResult.Success) _state.update { it.copy(draftCount = result.data.totalElements) }
        }
    }

    fun onApplyDraft() {
        val draft = _state.value.draft ?: return
        _state.update { it.copy(draft = null) }
        applyFilters(draft.copy(query = _state.value.filters.query, sort = _state.value.filters.sort))
    }

    fun onDismissFilters() {
        draftJob?.cancel()
        _state.update { it.copy(draft = null, draftCount = null) }
    }

    private fun applyFilters(filters: SearchFilters) {
        if (filters == _state.value.filters && _state.value.error == null && !_state.value.isLoading) return
        _state.update { it.copy(filters = filters) }
        reload()
    }

    private fun reload(refreshing: Boolean = false) {
        loadJob?.cancel()
        val filters = _state.value.filters
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = !refreshing,
                    isRefreshing = refreshing,
                    error = null,
                    loadMoreFailed = false,
                    items = if (refreshing) it.items else emptyList(),
                )
            }
            when (val result = search(filters, 1)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        items = result.data.items,
                        page = result.data.page,
                        totalElements = result.data.totalElements,
                        endReached = result.data.isLast,
                        cachedAtMillis = result.cachedAtMillis,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
                is AppResult.Failure -> _state.update {
                    it.copy(error = result.error, isLoading = false, isRefreshing = false, items = emptyList(), totalElements = null)
                }
            }
        }
    }

    companion object {
        const val QUERY_DEBOUNCE_MS = 400L
        const val LOAD_MORE_THRESHOLD = 5
    }
}
