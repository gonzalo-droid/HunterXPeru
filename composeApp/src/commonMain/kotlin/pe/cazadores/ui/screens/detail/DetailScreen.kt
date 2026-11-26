package pe.cazadores.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import pe.cazadores.platform.encodePng
import pe.cazadores.ads.AdsController
import pe.cazadores.ads.allowsShare
import pe.cazadores.ads.showRewardedForShare
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pe.cazadores.ads.AdBanner
import pe.cazadores.ads.AdPlacement
import pe.cazadores.core.DateFormatter
import pe.cazadores.core.RewardClassCalculator
import pe.cazadores.core.RewardFormatter
import pe.cazadores.core.config.AppConfig
import pe.cazadores.core.result.DataError
import pe.cazadores.core.text.sentenceCase
import pe.cazadores.core.text.titleCase
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.RewardPosition
import pe.cazadores.domain.model.Sex
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.back
import pe.cazadores.resources.call_line
import pe.cazadores.resources.detail_crimes
import pe.cazadores.resources.detail_regime_body
import pe.cazadores.resources.detail_regime_cta
import pe.cazadores.resources.detail_department
import pe.cazadores.resources.detail_if_you_see
import pe.cazadores.resources.detail_if_you_see_body
import pe.cazadores.resources.detail_not_found_title
import pe.cazadores.resources.detail_photo_cd
import pe.cazadores.resources.detail_province
import pe.cazadores.resources.detail_removed_msg
import pe.cazadores.resources.detail_report
import pe.cazadores.resources.detail_save
import pe.cazadores.resources.detail_saved
import pe.cazadores.resources.detail_saved_msg
import pe.cazadores.resources.detail_sex
import pe.cazadores.resources.detail_sex_female
import pe.cazadores.resources.detail_sex_male
import pe.cazadores.resources.detail_share
import pe.cazadores.resources.detail_source
import pe.cazadores.resources.detail_position
import pe.cazadores.resources.detail_position_rank
import pe.cazadores.resources.detail_position_tied
import pe.cazadores.resources.detail_position_tied_one
import pe.cazadores.resources.source_note
import pe.cazadores.resources.detail_unknown
import pe.cazadores.resources.error_not_found
import pe.cazadores.resources.reward_none
import pe.cazadores.resources.share_text
import pe.cazadores.ui.components.AdaptiveWidth
import pe.cazadores.ui.components.CallButton
import pe.cazadores.ui.components.CriminalPhoto
import pe.cazadores.ui.components.WantedPoster
import pe.cazadores.ui.components.ErrorState
import pe.cazadores.ui.components.MessageState
import pe.cazadores.ui.components.OfflineBanner
import pe.cazadores.ui.components.SecondaryButton
import pe.cazadores.ui.components.SectionLabel
import pe.cazadores.ui.components.SkeletonBlock
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.screens.legend.RankLegendSheet
import pe.cazadores.ui.theme.CzIcons

@Composable
fun DetailRoute(
    hash: String,
    onBack: () -> Unit,
    onOpenRegime: () -> Unit,
    viewModel: DetailViewModel = koinViewModel(key = hash) { parametersOf(hash) },
    actions: ExternalActions = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLegend by rememberSaveable { mutableStateOf(false) }
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
            AppConfig.PLAY_STORE_URL,
            AppConfig.APP_STORE_URL,
        )
    }

    // true mientras se dibuja y codifica la imagen: los toques extra en Compartir se ignoran.
    var sharing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Si la ficha deja de estar cargada (p. ej. Reintentar), el seguro de "compartiendo" no debe quedar activo.
    LaunchedEffect(loaded == null) { if (loaded == null) sharing = false }

    // D33: anuncio bonificado antes de compartir. Sin consentimiento o sin anuncio se comparte directo.
    val ads = koinInject<AdsController>()
    val adsReady by ads.canRequestAds.collectAsStateWithLifecycle()
    LaunchedEffect(adsReady) { if (adsReady) ads.preloadRewarded() }
    var askAd by remember { mutableStateOf(false) }
    var waitingAd by remember { mutableStateOf(false) }
    val onShare = {
        if (shareText != null && !waitingAd) {
            if (adsReady) askAd = true else sharing = true
        }
    }

    DetailScreen(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onShare = onShare,
        onToggleFavorite = viewModel::onToggleFavorite,
        onCall = { actions.dial(AppConfig.REPORT_PHONE_URI) },
        onReport = viewModel::onOpenReport,
        onOpenLegend = { showLegend = true },
        onOpenRegime = onOpenRegime,
        onRetry = viewModel::onRetry,
    )

    if (showLegend) RankLegendSheet(onDismiss = { showLegend = false })

    if (askAd) {
        ShareAdDialog(
            onConfirm = {
                askAd = false
                waitingAd = true
                scope.launch {
                    try {
                        if (ads.showRewardedForShare().allowsShare()) sharing = true
                    } finally {
                        waitingAd = false
                    }
                }
            },
            onDismiss = { askAd = false },
        )
    }

    if (sharing && loaded != null && shareText != null) {
        // Fuera de pantalla: mide 0 × 0 y no se ve; cae al texto solo si la imagen no se pudo generar.
        SharePosterCapture(loaded.criminal, loaded.danger, RewardClassCalculator.of(loaded.criminal.reward)) { bitmap ->
            scope.launch {
                try {
                    sharePoster(bitmap, shareText, actions, encode = { it.encodePng() })
                } finally {
                    sharing = false
                }
            }
        }
    }

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
    onOpenLegend: () -> Unit,
    onOpenRegime: () -> Unit,
    onRetry: () -> Unit,
) {
    val loaded = state.content as? DetailContent.Loaded
    AdaptiveWidth(Modifier.fillMaxSize()) { gutter, _ ->
    Scaffold(
        containerColor = Cz.colors.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopActions(
                showActions = loaded != null,
                isFavorite = state.isFavorite,
                gutter = gutter,
                onBack = onBack,
                onShare = onShare,
                onToggleFavorite = onToggleFavorite,
            )
        },
        bottomBar = { if (loaded != null) BottomActions(gutter, onCall, onReport) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val content = state.content) {
                DetailContent.Loading -> DetailSkeleton(gutter)
                is DetailContent.Error ->
                    if (content.error == DataError.NotFound) {
                        MessageState(
                            icon = CzIcons.Info,
                            title = stringResource(Res.string.detail_not_found_title),
                            body = stringResource(Res.string.error_not_found),
                            action = { SecondaryButton(stringResource(Res.string.back), onBack) },
                            modifier = Modifier.padding(horizontal = gutter),
                        )
                    } else {
                        ErrorState(content.error, onRetry, onCall, Modifier.padding(horizontal = gutter))
                    }
                is DetailContent.Loaded -> DetailBody(content, gutter, onOpenLegend, onOpenRegime)
            }
        }
    }
    }
}

@Composable
private fun TopActions(
    showActions: Boolean,
    isFavorite: Boolean,
    gutter: Dp,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding()
            // Los IconButton (48 dp) dejan el icono a 12 dp de su borde: así quedan alineados al contenido.
            .padding(horizontal = max(4.dp, gutter - 16.dp), vertical = 4.dp),
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
private fun BottomActions(gutter: Dp, onCall: () -> Unit, onReport: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Cz.colors.bg)) {
        HorizontalDivider(color = Cz.colors.line)
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = gutter, vertical = 12.dp),
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
private fun DetailBody(content: DetailContent.Loaded, gutter: Dp, onOpenLegend: () -> Unit, onOpenRegime: () -> Unit) {
    val c = content.criminal
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = gutter).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (content.fromCache) OfflineBanner(content.dataAtMillis)

        WantedPoster(
            criminal = c,
            assessment = content.danger,
            rewardClass = RewardClassCalculator.of(c.reward),
            onRankClick = onOpenLegend,
        ) {
            CriminalPhoto(
                criminal = c,
                contentDescription = stringResource(Res.string.detail_photo_cd, c.displayName),
                modifier = Modifier.fillMaxSize(),
                shape = CazadoresShapes.photo,
            )
        }

        content.position?.let { PositionCard(it) }

        if (content.specialRegime) RegimeNotice(onOpenRegime)

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
        Text(stringResource(Res.string.source_note), style = Cz.type.caption, color = Cz.colors.inkMuted)
        AdBanner(AdPlacement.DETAIL_BANNER, Modifier.fillMaxWidth())
    }
}

/** Puesto nacional por recompensa, sobre papel como el cartel (papel en ambos temas). */
@Composable
private fun PositionCard(position: RewardPosition) {
    Column(
        Modifier.fillMaxWidth().background(Cz.colors.paper, CazadoresShapes.card)
            .border(1.dp, Cz.colors.paperLine, CazadoresShapes.card).padding(14.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(Res.string.detail_position).uppercase(), style = Cz.type.overline, color = Cz.colors.onPaperMuted)
        Text(
            stringResource(
                Res.string.detail_position_rank,
                position.rank.toString(),
                position.total.toString(),
                position.topPercent,
            ),
            style = Cz.type.bodyStrong,
            color = Cz.colors.onPaper,
        )
        when (position.tiedWith) {
            0 -> Unit
            1 -> Text(stringResource(Res.string.detail_position_tied_one), style = Cz.type.bodySmall, color = Cz.colors.onPaperMuted)
            else -> Text(stringResource(Res.string.detail_position_tied, position.tiedWith), style = Cz.type.bodySmall, color = Cz.colors.onPaperMuted)
        }
    }
}

/** Aviso con borde punteado rojo: solo si algún delito es extorsión o sicariato. Abre la pantalla del régimen. */
@Composable
private fun RegimeNotice(onClick: () -> Unit) {
    val red = Cz.colors.posterRed
    val density = LocalDensity.current
    Column(
        Modifier.fillMaxWidth()
            .clip(CazadoresShapes.card)
            .background(Cz.colors.paper)
            .drawBehind {
                val stroke = with(density) { 2.dp.toPx() }
                drawRoundRect(
                    color = red,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(with(density) { 14.dp.toPx() }),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(Res.string.detail_regime_body), style = Cz.type.bodyStrong, color = Cz.colors.onPaper)
        Text(
            stringResource(Res.string.detail_regime_cta),
            style = Cz.type.bodySmall,
            color = Cz.colors.onPaper,
            textDecoration = TextDecoration.Underline,
        )
    }
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
private fun DetailSkeleton(gutter: Dp) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = gutter),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SkeletonBlock(Modifier.fillMaxWidth().height(420.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(72.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(80.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(150.dp))
    }
}
