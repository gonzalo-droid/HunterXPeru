package pe.cazadores.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pe.cazadores.core.DateFormatter
import pe.cazadores.core.RewardFormatter
import pe.cazadores.core.config.AppConfig
import pe.cazadores.core.result.DataError
import pe.cazadores.core.text.sentenceCase
import pe.cazadores.core.text.titleCase
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.Sex
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.back
import pe.cazadores.resources.call_line
import pe.cazadores.resources.detail_crimes
import pe.cazadores.resources.detail_danger
import pe.cazadores.resources.detail_danger_note
import pe.cazadores.resources.detail_department
import pe.cazadores.resources.detail_id
import pe.cazadores.resources.detail_if_you_see
import pe.cazadores.resources.detail_if_you_see_body
import pe.cazadores.resources.detail_not_found_title
import pe.cazadores.resources.detail_photo_cd
import pe.cazadores.resources.detail_province
import pe.cazadores.resources.detail_removed_msg
import pe.cazadores.resources.detail_report
import pe.cazadores.resources.detail_reward
import pe.cazadores.resources.detail_save
import pe.cazadores.resources.detail_saved
import pe.cazadores.resources.detail_saved_msg
import pe.cazadores.resources.detail_sex
import pe.cazadores.resources.detail_sex_female
import pe.cazadores.resources.detail_sex_male
import pe.cazadores.resources.detail_share
import pe.cazadores.resources.detail_source
import pe.cazadores.resources.detail_unknown
import pe.cazadores.resources.error_not_found
import pe.cazadores.resources.reward_none
import pe.cazadores.resources.share_text
import pe.cazadores.ui.components.CallButton
import pe.cazadores.ui.components.CriminalPhoto
import pe.cazadores.ui.components.DangerBadge
import pe.cazadores.ui.components.ErrorState
import pe.cazadores.ui.components.MessageState
import pe.cazadores.ui.components.OfflineBanner
import pe.cazadores.ui.components.RewardBadge
import pe.cazadores.ui.components.SecondaryButton
import pe.cazadores.ui.components.SectionLabel
import pe.cazadores.ui.components.SkeletonBlock
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

@Composable
fun DetailRoute(
    hash: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel(key = hash) { parametersOf(hash) },
    actions: ExternalActions = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is DetailEvent.FavoriteChanged -> snackbar.showSnackbar(
                    getString(if (event.saved) Res.string.detail_saved_msg else Res.string.detail_removed_msg),
                )
            }
        }
    }
    val loaded = state.content as? DetailContent.Loaded
    val shareText = loaded?.let {
        stringResource(
            Res.string.share_text,
            it.criminal.displayName,
            RewardFormatter.format(it.criminal.reward) ?: stringResource(Res.string.reward_none),
            AppConfig.REPORT_PHONE,
            AppConfig.DEEP_LINK_BASE + it.criminal.hash,
        )
    }

    DetailScreen(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onShare = { shareText?.let(actions::share) },
        onToggleFavorite = viewModel::onToggleFavorite,
        onCall = { actions.dial(AppConfig.REPORT_PHONE_URI) },
        onReport = viewModel::onOpenReport,
        onRetry = viewModel::onRetry,
    )

    if (state.showReport && loaded != null) {
        ReportSheet(
            criminal = loaded.criminal,
            onCall = { actions.dial(AppConfig.REPORT_PHONE_URI) },
            onCallAlt = { actions.dial(AppConfig.REPORT_PHONE_ALT_URI) },
            onSendEmail = { subject, body -> actions.email(AppConfig.REPORT_EMAIL, subject, body) },
            onDismiss = viewModel::onDismissReport,
        )
    }
}

@Composable
fun DetailScreen(
    state: DetailUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCall: () -> Unit,
    onReport: () -> Unit,
    onRetry: () -> Unit,
) {
    val loaded = state.content as? DetailContent.Loaded
    Scaffold(
        containerColor = Cz.colors.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopActions(
                showActions = loaded != null,
                isFavorite = state.isFavorite,
                onBack = onBack,
                onShare = onShare,
                onToggleFavorite = onToggleFavorite,
            )
        },
        bottomBar = { if (loaded != null) BottomActions(onCall, onReport) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val content = state.content) {
                DetailContent.Loading -> DetailSkeleton()
                is DetailContent.Error ->
                    if (content.error == DataError.NotFound) {
                        MessageState(
                            icon = CzIcons.Info,
                            title = stringResource(Res.string.detail_not_found_title),
                            body = stringResource(Res.string.error_not_found),
                            action = { SecondaryButton(stringResource(Res.string.back), onBack) },
                        )
                    } else {
                        ErrorState(content.error, onRetry)
                    }
                is DetailContent.Loaded -> DetailBody(content)
            }
        }
    }
}

@Composable
private fun TopActions(
    showActions: Boolean,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(CzIcons.Back, stringResource(Res.string.back), tint = Cz.colors.ink) }
        Spacer(Modifier.weight(1f))
        if (showActions) {
            IconButton(onClick = onShare) { Icon(CzIcons.Share, stringResource(Res.string.detail_share), tint = Cz.colors.ink) }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) CzIcons.BookmarkFilled else CzIcons.Bookmark,
                    contentDescription = stringResource(if (isFavorite) Res.string.detail_saved else Res.string.detail_save),
                    tint = Cz.colors.ink,
                )
            }
        }
    }
}

@Composable
private fun BottomActions(onCall: () -> Unit, onReport: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Cz.colors.bg)) {
        HorizontalDivider(color = Cz.colors.line)
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = CazadoresDimens.screenPadding, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CallButton(
                text = stringResource(Res.string.call_line, AppConfig.REPORT_PHONE),
                onClick = onCall,
                icon = CzIcons.Phone,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onReport,
                modifier = Modifier.size(56.dp)
                    .border(1.5.dp, Cz.colors.ink, CazadoresShapes.button)
                    .background(Cz.colors.surface, CazadoresShapes.button),
            ) { Icon(CzIcons.Edit, contentDescription = stringResource(Res.string.detail_report), tint = Cz.colors.ink) }
        }
    }
}

@Composable
private fun DetailBody(content: DetailContent.Loaded) {
    val c = content.criminal
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = CazadoresDimens.screenPadding).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (content.fromCache) OfflineBanner(content.dataAtMillis)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            CriminalPhoto(
                cacheKey = c.hash,
                base64 = c.photoBase64,
                contentDescription = stringResource(Res.string.detail_photo_cd, c.displayName),
                modifier = Modifier.size(width = 140.dp, height = 176.dp),
                shape = CazadoresShapes.card,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.detail_id, c.id.toString()), style = Cz.type.mono, color = Cz.colors.inkMuted)
                SectionLabel(stringResource(Res.string.detail_reward))
                RewardBadge(c.reward, style = Cz.type.amount)
            }
        }

        Text(
            c.displayName,
            style = Cz.type.display,
            color = Cz.colors.ink,
            modifier = Modifier.semantics { heading() },
        )

        InfoCard {
            // FlowRow: con letra grande la insignia baja de línea en vez de partir "PELIGROSIDAD".
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(stringResource(Res.string.detail_danger))
                DangerBadge(content.danger)
            }
            Text(stringResource(Res.string.detail_danger_note), style = Cz.type.caption, color = Cz.colors.inkMuted)
        }

        if (c.crimes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(Res.string.detail_crimes))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    c.crimes.forEach { crime ->
                        Text(
                            crime.sentenceCase(),
                            style = Cz.type.bodyStrong,
                            color = Cz.colors.ink,
                            modifier = Modifier.background(Cz.colors.surface, CazadoresShapes.badge)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        DataRows(c)

        Column(
            Modifier.fillMaxWidth().background(Cz.colors.header, CazadoresShapes.card).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(Res.string.detail_if_you_see).uppercase(),
                style = Cz.type.title,
                color = Cz.colors.onHeader,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(Res.string.detail_if_you_see_body), style = Cz.type.bodySmall, color = Cz.colors.onHeader)
        }

        Text(
            stringResource(Res.string.detail_source, DateFormatter.date(content.dataAtMillis)),
            style = Cz.type.caption,
            color = Cz.colors.inkMuted,
        )
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun DataRows(c: Criminal) {
    val unknown = stringResource(Res.string.detail_unknown)
    val rows = listOf(
        stringResource(Res.string.detail_department) to (c.departamento?.titleCase() ?: unknown),
        stringResource(Res.string.detail_province) to (c.provincia?.titleCase() ?: unknown),
        stringResource(Res.string.detail_sex) to when (c.sex) {
            Sex.MALE -> stringResource(Res.string.detail_sex_male)
            Sex.FEMALE -> stringResource(Res.string.detail_sex_female)
            null -> unknown
        },
    )
    Column(Modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card)) {
        rows.forEachIndexed { index, (label, value) ->
            // FlowRow: con letra grande el valor baja de línea en vez de cortarse.
            FlowRow(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = Cz.type.body, color = Cz.colors.inkMuted)
                Text(value, style = Cz.type.bodyStrong, color = Cz.colors.ink)
            }
            if (index < rows.lastIndex) HorizontalDivider(color = Cz.colors.line, modifier = Modifier.padding(horizontal = 14.dp))
        }
    }
}

@Composable
private fun DetailSkeleton() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = CazadoresDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            SkeletonBlock(Modifier.size(140.dp, 176.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SkeletonBlock(Modifier.width(80.dp).height(14.dp))
                SkeletonBlock(Modifier.width(140.dp).height(34.dp))
            }
        }
        SkeletonBlock(Modifier.fillMaxWidth().height(72.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(80.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(150.dp))
    }
}
