package pe.cazadores.ui.screens.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pe.cazadores.ads.AdPlacement
import pe.cazadores.ads.adAfterRow
import pe.cazadores.ads.InlineAdSlot
import pe.cazadores.domain.model.Criminal
import pe.cazadores.resources.Res
import pe.cazadores.resources.saved_empty_body
import pe.cazadores.resources.saved_empty_title
import pe.cazadores.resources.saved_go_search
import pe.cazadores.resources.saved_removed
import pe.cazadores.resources.saved_remove_hint
import pe.cazadores.resources.saved_title
import pe.cazadores.resources.undo
import pe.cazadores.ui.components.AdaptiveWidth
import pe.cazadores.ui.components.ListSkeleton
import pe.cazadores.ui.components.MessageState
import pe.cazadores.ui.components.PosterGridRow
import pe.cazadores.ui.components.posterColumns
import pe.cazadores.ui.components.PrimaryButton
import pe.cazadores.ui.components.ScreenTitle
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

@Composable
fun SavedRoute(
    onOpenDetail: (String) -> Unit,
    onGoSearch: () -> Unit,
    viewModel: SavedViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    SavedScreen(
        state = state,
        snackbar = snackbar,
        onOpenDetail = onOpenDetail,
        onGoSearch = onGoSearch,
        onRemove = { criminal ->
            viewModel.onRemove(criminal)
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(
                    message = getString(Res.string.saved_removed, criminal.displayName),
                    actionLabel = getString(Res.string.undo),
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.onUndoRemove(criminal)
            }
        },
    )
}

@Composable
fun SavedScreen(
    state: SavedUiState,
    snackbar: SnackbarHostState,
    onOpenDetail: (String) -> Unit,
    onGoSearch: () -> Unit,
    onRemove: (Criminal) -> Unit,
) {
    Scaffold(containerColor = Cz.colors.bg, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AdaptiveWidth(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).statusBarsPadding()) { gutter, contentWidth ->
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.padding(start = gutter, end = gutter, top = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ScreenTitle(stringResource(Res.string.saved_title))
                    if (state.items.isNotEmpty()) {
                        Text(stringResource(Res.string.saved_remove_hint), style = Cz.type.caption, color = Cz.colors.inkMuted)
                    }
                }
                when {
                    state.isLoading -> ListSkeleton(3, Modifier.padding(horizontal = gutter))
                    state.items.isEmpty() -> MessageState(
                        icon = CzIcons.Bookmark,
                        modifier = Modifier.padding(horizontal = gutter),
                        title = stringResource(Res.string.saved_empty_title),
                        body = stringResource(Res.string.saved_empty_body),
                        action = { PrimaryButton(stringResource(Res.string.saved_go_search), onGoSearch, icon = CzIcons.Search) },
                    )
                    else -> {
                        val perRow = posterColumns(contentWidth)
                        val rows = remember(state.items, perRow) { state.items.chunked(perRow) }
                        LazyColumn(
                            contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(rows, key = { _, row -> row.first().hash }) { index, row ->
                                Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PosterGridRow(row, perRow, onOpenDetail, onRemove = onRemove)
                                    // Un anuncio cada AD_EVERY_N_ITEMS carteles, con cualquier número de columnas.
                                    if (adAfterRow(index, perRow)) InlineAdSlot(AdPlacement.SAVED_INLINE)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
