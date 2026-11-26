package pe.cazadores.ui.screens.detail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import pe.cazadores.platform.encodePng
import pe.cazadores.ads.AdsController
import pe.cazadores.ads.allowsShare
import pe.cazadores.ads.showRewardedForShare
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pe.cazadores.core.RewardClassCalculator
import pe.cazadores.core.RewardFormatter
import pe.cazadores.core.config.AppConfig
import pe.cazadores.core.result.DataError
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.back
import pe.cazadores.resources.detail_not_found_title
import pe.cazadores.resources.detail_removed_msg
import pe.cazadores.resources.detail_saved_msg
import pe.cazadores.resources.error_not_found
import pe.cazadores.resources.reward_none
import pe.cazadores.resources.share_text
import pe.cazadores.ui.components.ErrorState
import pe.cazadores.ui.components.MessageState
import pe.cazadores.ui.components.SecondaryButton
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
                        ErrorState(content.error, onRetry, onCall)
                    }
                is DetailContent.Loaded -> DetailBody(content, onOpenLegend, onOpenRegime)
            }
        }
    }
}
