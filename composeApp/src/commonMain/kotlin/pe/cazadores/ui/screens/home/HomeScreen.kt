package pe.cazadores.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.cazadores.ads.AdBanner
import pe.cazadores.ads.AdPlacement
import pe.cazadores.core.DateFormatter
import pe.cazadores.core.RewardClassCalculator
import pe.cazadores.core.config.AppConfig
import pe.cazadores.core.text.titleCase
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.Departamento
import pe.cazadores.domain.model.RewardClass
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.close
import pe.cazadores.resources.detail_regime_cta
import pe.cazadores.resources.home_call_cd
import pe.cazadores.resources.home_call_overline
import pe.cazadores.resources.home_class_s
import pe.cazadores.resources.home_near_change
import pe.cazadores.resources.home_near_clear
import pe.cazadores.resources.home_near_empty
import pe.cazadores.resources.home_near_error
import pe.cazadores.resources.home_near_in
import pe.cazadores.resources.home_near_pick
import pe.cazadores.resources.home_near_prompt
import pe.cazadores.resources.home_near_retry
import pe.cazadores.resources.home_near_sheet_title
import pe.cazadores.resources.home_near_sheet_unavailable
import pe.cazadores.resources.home_near_title
import pe.cazadores.resources.home_overline
import pe.cazadores.resources.home_regime_title
import pe.cazadores.resources.home_search_hint
import pe.cazadores.resources.home_see_all
import pe.cazadores.resources.home_title_count
import pe.cazadores.resources.home_title_neutral
import pe.cazadores.resources.home_top
import pe.cazadores.resources.home_updated
import pe.cazadores.resources.source_note
import pe.cazadores.ui.components.ErrorState
import pe.cazadores.ui.components.OfflineBanner
import pe.cazadores.ui.components.PosterGrid
import pe.cazadores.ui.components.SecondaryButton
import pe.cazadores.ui.components.SheetStatusBarAppearance
import pe.cazadores.ui.components.SkeletonBlock
import pe.cazadores.ui.components.StatusBarAppearance
import pe.cazadores.ui.components.TextAction
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

private const val BOARD_LIMIT = 6

@Composable
fun HomeRoute(
    onOpenSearch: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenRegime: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
    actions: ExternalActions = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatusBarAppearance(lightIcons = true)
    HomeScreen(
        state = state,
        onOpenSearch = onOpenSearch,
        onSeeAll = onSeeAll,
        onOpenDetail = onOpenDetail,
        onOpenRegime = onOpenRegime,
        onCall = { actions.dial(AppConfig.REPORT_PHONE_URI) },
        onRetry = viewModel::onRetry,
        onPickDepartamento = viewModel::onPickDepartamento,
        onRetryNear = viewModel::retryNear,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenSearch: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenRegime: () -> Unit,
    onCall: () -> Unit,
    onRetry: () -> Unit,
    onPickDepartamento: (String?) -> Unit,
    onRetryNear: () -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Cz.colors.bg)) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header(state.stats?.total, state.updatedAtMillis, onOpenSearch)
        Column(
            Modifier.padding(CazadoresDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            state.cachedAtMillis?.let { OfflineBanner(it) }
            CallCard(onCall)
            RegimeBanner(onOpenRegime)
            when {
                state.isLoading && state.top.isEmpty() -> HomeSkeleton()
                state.error != null -> ErrorState(state.error, onRetry, onCall)
                else -> ClassSBoard(state.top, onSeeAll, onOpenDetail)
            }
            NearSection(state, onOpenDetail, onChoose = { showPicker = true }, onRetry = onRetryNear)
            AdBanner(AdPlacement.HOME_BANNER, Modifier.fillMaxWidth())
            Text(stringResource(Res.string.source_note), style = Cz.type.caption, color = Cz.colors.inkMuted)
        }
    }
    // Franja fija bajo la barra de estado: los iconos claros siempre quedan sobre oscuro al desplazar.
    Spacer(Modifier.align(Alignment.TopCenter).fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(Cz.colors.header))
    }
    if (showPicker) {
        DepartamentoSheet(
            departamentos = state.departamentos,
            selectedId = state.nearId,
            onPick = {
                onPickDepartamento(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun Header(total: Int?, updatedAtMillis: Long?, onOpenSearch: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Cz.colors.header).statusBarsPadding()
            .padding(start = CazadoresDimens.screenPadding, end = CazadoresDimens.screenPadding, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.home_overline).uppercase(), style = Cz.type.monoSmall, color = Cz.colors.onHeaderMuted)
                Text(
                    if (total != null) {
                        stringResource(Res.string.home_title_count, total)
                    } else {
                        stringResource(Res.string.home_title_neutral).uppercase()
                    },
                    style = Cz.type.headline,
                    color = Cz.colors.onHeader,
                    modifier = Modifier.semantics { heading() },
                )
            }
            updatedAtMillis?.let {
                Text(stringResource(Res.string.home_updated, DateFormatter.dateTime(it)), style = Cz.type.mono, color = Cz.colors.onHeaderMuted)
            }
        }
        // Atajo a Buscar: se ve como campo, pero es un botón que abre Buscar con el teclado listo.
        Row(
            Modifier.fillMaxWidth().heightIn(min = CazadoresDimens.buttonHeight)
                .background(Cz.colors.headerField, CazadoresShapes.field)
                .clickable(role = Role.Button, onClick = onOpenSearch)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(CzIcons.Search, contentDescription = null, tint = Cz.colors.onHeaderMuted, modifier = Modifier.size(20.dp))
            Text(stringResource(Res.string.home_search_hint), style = Cz.type.body, color = Cz.colors.onHeaderMuted)
        }
    }
}

@Composable
private fun CallCard(onCall: () -> Unit) {
    val cd = stringResource(Res.string.home_call_cd, AppConfig.REPORT_PHONE)
    Row(
        Modifier.fillMaxWidth().background(Cz.colors.action, CazadoresShapes.cardLarge)
            .clickable(role = Role.Button, onClick = onCall)
            .semantics(mergeDescendants = true) { contentDescription = cd }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).background(Cz.colors.onAction.copy(alpha = 0.16f), CazadoresShapes.photo),
            contentAlignment = Alignment.Center,
        ) { Icon(CzIcons.Phone, contentDescription = null, tint = Cz.colors.onAction, modifier = Modifier.size(22.dp)) }
        Column {
            Text(stringResource(Res.string.home_call_overline), style = Cz.type.caption, color = Cz.colors.onAction)
            Text(AppConfig.REPORT_PHONE, style = Cz.type.amount, color = Cz.colors.onAction)
        }
    }
}

@Composable
private fun RegimeBanner(onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Cz.colors.posterRed, CazadoresShapes.cardLarge)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(Res.string.home_regime_title), style = Cz.type.title, color = Cz.colors.onPosterRed)
        Text(stringResource(Res.string.detail_regime_cta), style = Cz.type.bodySmall, color = Cz.colors.onPosterRed)
    }
}

@Composable
private fun SectionHeader(title: String, action: String?, onAction: () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), style = Cz.type.title, color = Cz.colors.ink, modifier = Modifier.semantics { heading() })
        if (action != null) TextAction(action, onAction, color = Cz.colors.action)
    }
}

/** Carteles clavados de Clase S; si ninguno del top lo es, el top tal cual. */
@Composable
private fun ClassSBoard(top: List<Criminal>, onSeeAll: () -> Unit, onOpenDetail: (String) -> Unit) {
    if (top.isEmpty()) return
    val classS = top.filter { RewardClassCalculator.of(it.reward) == RewardClass.S }
    val shown = classS.ifEmpty { top }.take(BOARD_LIMIT)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = if (classS.isEmpty()) stringResource(Res.string.home_top) else stringResource(Res.string.home_class_s),
            action = stringResource(Res.string.home_see_all),
            onAction = onSeeAll,
        )
        PosterGrid(shown, onOpenDetail)
    }
}

@Composable
private fun NearSection(state: HomeUiState, onOpenDetail: (String) -> Unit, onChoose: () -> Unit, onRetry: () -> Unit) {
    val name = state.nearDepartamento?.name?.titleCase()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = stringResource(Res.string.home_near_title),
            action = if (state.nearId != null) stringResource(Res.string.home_near_change) else null,
            onAction = onChoose,
        )
        when {
            !state.nearReady -> SkeletonBlock(Modifier.fillMaxWidth().height(160.dp))
            state.nearId == null -> Column(
                Modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(Res.string.home_near_prompt), style = Cz.type.body, color = Cz.colors.ink)
                SecondaryButton(stringResource(Res.string.home_near_pick), onChoose, Modifier.fillMaxWidth())
            }
            state.nearLoading -> SkeletonBlock(Modifier.fillMaxWidth().height(160.dp))
            state.nearFailed -> FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.home_near_error), style = Cz.type.body, color = Cz.colors.inkMuted)
                TextAction(stringResource(Res.string.home_near_retry), onRetry, color = Cz.colors.action)
            }
            state.near.isEmpty() -> Text(
                stringResource(Res.string.home_near_empty, name ?: ""),
                style = Cz.type.body,
                color = Cz.colors.inkMuted,
            )
            else -> {
                if (name != null) Text(stringResource(Res.string.home_near_in, name), style = Cz.type.caption, color = Cz.colors.inkMuted)
                PosterGrid(state.near, onOpenDetail)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartamentoSheet(
    departamentos: List<Departamento>,
    selectedId: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SheetStatusBarAppearance(sheetState)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Cz.colors.surface,
        contentColor = Cz.colors.ink,
        shape = CazadoresShapes.sheet,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Cz.colors.inkMuted) },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = CazadoresDimens.screenPadding).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.home_near_sheet_title).uppercase(),
                style = Cz.type.headline,
                color = Cz.colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            if (departamentos.isEmpty()) {
                Text(stringResource(Res.string.home_near_sheet_unavailable), style = Cz.type.body, color = Cz.colors.inkMuted)
            } else {
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(departamentos, key = { it.id }) { d ->
                        val selected = d.id == selectedId
                        Text(
                            d.name.titleCase(),
                            style = if (selected) Cz.type.bodyStrong else Cz.type.body,
                            color = Cz.colors.ink,
                            modifier = Modifier.fillMaxWidth().heightIn(min = CazadoresDimens.touchTarget)
                                .clickable(role = Role.Button) { onPick(d.id) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
            if (selectedId != null) {
                TextAction(stringResource(Res.string.home_near_clear), { onPick(null) })
            }
            SecondaryButton(stringResource(Res.string.close), onDismiss, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HomeSkeleton() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) { SkeletonBlock(Modifier.weight(1f).height(260.dp)) }
    }
}
