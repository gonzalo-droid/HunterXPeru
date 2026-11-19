package pe.cazadores.ui.screens.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.back
import pe.cazadores.resources.error_network
import pe.cazadores.resources.error_title
import pe.cazadores.resources.loading_cd
import pe.cazadores.resources.retry
import pe.cazadores.resources.settings_legal_error
import pe.cazadores.resources.settings_legal_open_browser
import pe.cazadores.ui.components.AdaptiveWidth
import pe.cazadores.ui.components.MessageState
import pe.cazadores.ui.components.PrimaryButton
import pe.cazadores.ui.components.SecondaryButton
import pe.cazadores.ui.components.StatusBarAppearance
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

/** Motivo de un fallo de carga de la página principal (los recursos secundarios no cuentan). */
internal enum class WebLoadError { OFFLINE, OTHER }

/**
 * Vista web nativa (Android: `WebView`; iOS: `WKWebView`) sin JavaScript ni acceso a archivos.
 * Las navegaciones pasan por [WebLinkPolicy]: el mismo host queda dentro, el resto va a [onExternalLink].
 * [onLoadingChange] avisa inicio (true) y fin (false) de cada carga; tras [onError] no se informa más.
 */
@Composable
internal expect fun PlatformWebView(
    url: String,
    javaScript: Boolean,
    isDark: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onError: (WebLoadError) -> Unit,
    onExternalLink: (String) -> Unit,
    modifier: Modifier,
)

@Composable
fun LegalRoute(page: LegalPage, onBack: () -> Unit, actions: ExternalActions = koinInject()) {
    LegalScreen(
        title = stringResource(page.title),
        url = page.url,
        javaScript = page.needsJavaScript,
        onBack = onBack,
        onOpenExternal = actions::openUrl,
    )
}

/** Política de privacidad o términos dentro de la app. Cabecera oscura como las demás pantallas secundarias. */
@Composable
fun LegalScreen(title: String, url: String, onBack: () -> Unit, onOpenExternal: (String) -> Unit, javaScript: Boolean = false) {
    StatusBarAppearance(lightIcons = true)
    // Cada intento recrea la vista web desde cero (Reintentar).
    var attempt by rememberSaveable { mutableIntStateOf(0) }
    var loading by remember(attempt) { mutableStateOf(true) }
    var shown by remember(attempt) { mutableStateOf(false) }
    var error by remember(attempt) { mutableStateOf<WebLoadError?>(null) }

    AdaptiveWidth(Modifier.fillMaxSize().background(Cz.colors.bg)) { gutter, _ ->
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(Cz.colors.header).statusBarsPadding()
                .padding(horizontal = max(4.dp, gutter - 16.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(CzIcons.Back, stringResource(Res.string.back), tint = Cz.colors.onHeader)
            }
            Text(
                title.uppercase(),
                style = Cz.type.title,
                color = Cz.colors.onHeader,
                modifier = Modifier.weight(1f).padding(end = 16.dp, top = 8.dp, bottom = 8.dp)
                    .semantics { heading() },
            )
        }

        // La página trae sus propios márgenes: solo se recorta el sobrante en pantallas anchas.
        Box(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding().padding(horizontal = gutter - CazadoresDimens.screenPadding)) {
            val currentError = error
            if (currentError != null) {
                LegalError(
                    error = currentError,
                    onRetry = { attempt++ },
                    onOpenInBrowser = { onOpenExternal(url) },
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                key(attempt) {
                    PlatformWebView(
                        url = url,
                        javaScript = javaScript,
                        isDark = Cz.colors.isDark,
                        onLoadingChange = { isLoading ->
                            loading = isLoading
                            if (!isLoading) shown = true
                        },
                        onError = { error = it },
                        onExternalLink = onOpenExternal,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (loading && !shown) {
                    // Primera carga: la vista web aún está vacía, se muestra un indicador centrado.
                    val description = stringResource(Res.string.loading_cd)
                    Box(Modifier.fillMaxSize().background(Cz.colors.bg), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Cz.colors.ink,
                            modifier = Modifier.size(32.dp).semantics { contentDescription = description },
                        )
                    }
                } else if (loading) {
                    // Navegación dentro de la página ya visible: barra fina bajo la cabecera.
                    LinearProgressIndicator(
                        color = Cz.colors.ink,
                        trackColor = Cz.colors.line,
                        modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun LegalError(
    error: WebLoadError,
    onRetry: () -> Unit,
    onOpenInBrowser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offline = error == WebLoadError.OFFLINE
    Box(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
        MessageState(
            icon = if (offline) CzIcons.WifiOff else CzIcons.Alert,
            title = stringResource(Res.string.error_title),
            body = stringResource(if (offline) Res.string.error_network else Res.string.settings_legal_error),
            modifier = Modifier.widthIn(max = 480.dp),
        ) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PrimaryButton(
                    text = stringResource(Res.string.retry),
                    onClick = onRetry,
                    icon = CzIcons.Refresh,
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = stringResource(Res.string.settings_legal_open_browser),
                    onClick = onOpenInBrowser,
                    icon = CzIcons.External,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
