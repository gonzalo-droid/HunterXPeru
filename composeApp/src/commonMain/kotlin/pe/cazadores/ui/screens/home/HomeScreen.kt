package pe.cazadores.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.cazadores.ads.AdBanner
import pe.cazadores.ads.AdPlacement
import pe.cazadores.core.DateFormatter
import pe.cazadores.core.RewardFormatter
import pe.cazadores.core.config.AppConfig
import pe.cazadores.domain.model.Criminal
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.app_name
import pe.cazadores.resources.home_call_cd
import pe.cazadores.resources.home_call_overline
import pe.cazadores.resources.home_overline
import pe.cazadores.resources.home_rank
import pe.cazadores.resources.home_search_hint
import pe.cazadores.resources.home_see_all
import pe.cazadores.resources.home_stat_max
import pe.cazadores.resources.home_stat_total
import pe.cazadores.resources.home_top
import pe.cazadores.resources.home_updated
import pe.cazadores.resources.source_note
import pe.cazadores.ui.components.CriminalPhoto
import pe.cazadores.ui.components.ErrorState
import pe.cazadores.ui.components.OfflineBanner
import pe.cazadores.ui.components.RewardBadge
import pe.cazadores.ui.components.SkeletonBlock
import pe.cazadores.ui.components.StatusBarAppearance
import pe.cazadores.ui.components.TextAction
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

@Composable
fun HomeRoute(
    onOpenSearch: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
    actions: ExternalActions = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatusBarAppearance(lightIcons = true)
    HomeScreen(
        state = state,
        onOpenSearch = onOpenSearch,
        onOpenDetail = onOpenDetail,
        onCall = { actions.dial(AppConfig.REPORT_PHONE_URI) },
        onRetry = viewModel::onRetry,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenSearch: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onCall: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Cz.colors.bg).verticalScroll(rememberScrollState())) {
        Header(state.updatedAtMillis, onOpenSearch)
        Column(
            Modifier.padding(CazadoresDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            state.cachedAtMillis?.let { OfflineBanner(it) }
            CallCard(onCall)
            when {
                state.isLoading && state.top.isEmpty() -> HomeSkeleton()
                state.error != null -> ErrorState(state.error, onRetry)
                else -> {
                    Stats(state)
                    TopWanted(state.top, onOpenSearch, onOpenDetail)
                }
            }
            AdBanner(AdPlacement.HOME_BANNER, Modifier.fillMaxWidth())
            Text(stringResource(Res.string.source_note), style = Cz.type.caption, color = Cz.colors.inkMuted)
        }
    }
}

@Composable
private fun Header(updatedAtMillis: Long?, onOpenSearch: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Cz.colors.header).statusBarsPadding()
            .padding(start = CazadoresDimens.screenPadding, end = CazadoresDimens.screenPadding, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.home_overline).uppercase(), style = Cz.type.monoSmall, color = Cz.colors.onHeaderMuted)
                Text(
                    stringResource(Res.string.app_name).uppercase(),
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
private fun Stats(state: HomeUiState) {
    val stats = state.stats ?: return
    // FlowRow: con letra grande las dos cifras quedan una debajo de otra.
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 2,
    ) {
        StatCard(stringResource(Res.string.home_stat_total), stats.total.toString(), Modifier.weight(1f))
        StatCard(stringResource(Res.string.home_stat_max), RewardFormatter.format(stats.maxReward) ?: "—", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.background(Cz.colors.surface, CazadoresShapes.card).semantics(mergeDescendants = true) {}.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = Cz.type.caption, color = Cz.colors.inkMuted)
        Text(value, style = Cz.type.amount, color = Cz.colors.ink, maxLines = 1)
    }
}

@Composable
private fun TopWanted(top: List<Criminal>, onSeeAll: () -> Unit, onOpenDetail: (String) -> Unit) {
    if (top.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.home_top).uppercase(),
                style = Cz.type.title,
                color = Cz.colors.ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextAction(stringResource(Res.string.home_see_all), onSeeAll, color = Cz.colors.action)
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = CazadoresDimens.screenPadding),
        ) {
            itemsIndexed(top, key = { _, c -> c.hash }) { index, criminal ->
                TopCard(index + 1, criminal) { onOpenDetail(criminal.hash) }
            }
        }
    }
}

@Composable
private fun TopCard(rank: Int, criminal: Criminal, onClick: () -> Unit) {
    Column(
        Modifier.width(156.dp).background(Cz.colors.surface, CazadoresShapes.card)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {},
    ) {
        Box {
            CriminalPhoto(
                cacheKey = criminal.hash,
                base64 = criminal.photoBase64,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(176.dp),
                shape = CazadoresShapes.card,
            )
            Text(
                stringResource(Res.string.home_rank, rank),
                style = Cz.type.monoSmall,
                color = Cz.colors.onHeader,
                modifier = Modifier.padding(8.dp).background(Cz.colors.header, CazadoresShapes.badge).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(criminal.displayName, style = Cz.type.amountSmall, color = Cz.colors.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
            RewardBadge(criminal.reward)
        }
    }
}

@Composable
private fun HomeSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(Modifier.weight(1f).height(84.dp))
            SkeletonBlock(Modifier.weight(1f).height(84.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { SkeletonBlock(Modifier.width(156.dp).height(260.dp)) }
        }
    }
}
