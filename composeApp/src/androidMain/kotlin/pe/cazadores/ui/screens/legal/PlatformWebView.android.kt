package pe.cazadores.ui.screens.legal

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import pe.cazadores.ui.theme.Cz

@Composable
internal actual fun PlatformWebView(
    url: String,
    isDark: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onError: (WebLoadError) -> Unit,
    onExternalLink: (String) -> Unit,
    modifier: Modifier,
) {
    val loadingChange by rememberUpdatedState(onLoadingChange)
    val error by rememberUpdatedState(onError)
    val external by rememberUpdatedState(onExternalLink)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    val background = Cz.colors.bg.toArgb()
    // La vista web no sigue la escala de letra del sistema por sí sola.
    val textZoom = (LocalDensity.current.fontScale * 100).toInt()

    // El gesto atrás recorre primero el historial de la vista web y solo luego cierra la pantalla.
    BackHandler(enabled = canGoBack) { webView?.goBack() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(background)
                settings.apply {
                    // Las páginas legales se sirven ya renderizadas: no necesitan JavaScript.
                    javaScriptEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    domStorageEnabled = false
                    setGeolocationEnabled(false)
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    this.textZoom = textZoom
                }
                webViewClient = object : WebViewClient() {
                    private var failed = false
                    private var mainUrl: String? = null

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame) return false
                        val target = request.url.toString()
                        return when (WebLinkPolicy.decide(target, url)) {
                            LinkDecision.IN_APP -> false
                            LinkDecision.EXTERNAL -> {
                                external(target)
                                true
                            }
                            LinkDecision.BLOCK -> true
                        }
                    }

                    override fun onPageStarted(view: WebView, pageUrl: String?, favicon: Bitmap?) {
                        mainUrl = pageUrl
                        if (!failed) loadingChange(true)
                    }

                    override fun onPageFinished(view: WebView, pageUrl: String?) {
                        canGoBack = view.canGoBack()
                        if (!failed) loadingChange(false)
                    }

                    override fun doUpdateVisitedHistory(view: WebView, historyUrl: String?, isReload: Boolean) {
                        canGoBack = view.canGoBack()
                    }

                    override fun onReceivedError(view: WebView, request: WebResourceRequest, err: WebResourceError) {
                        if (!request.isForMainFrame) return
                        val offline = err.errorCode in OFFLINE_CODES
                        fail(if (offline) WebLoadError.OFFLINE else WebLoadError.OTHER)
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        response: WebResourceResponse,
                    ) {
                        // Un 404 o 5xx mostraría la página de error del sitio dentro de la app.
                        if (request.isForMainFrame && response.statusCode >= 400) fail(WebLoadError.OTHER)
                    }

                    @SuppressLint("WebViewClientOnReceivedSslError")
                    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, sslError: SslError) {
                        handler.cancel()
                        if (sslError.url == mainUrl) fail(WebLoadError.OTHER)
                    }

                    private fun fail(reason: WebLoadError) {
                        if (failed) return
                        failed = true
                        error(reason)
                    }
                }
                loadUrl(url)
                webView = this
            }
        },
        update = { it.settings.textZoom = textZoom },
        onRelease = {
            webView = null
            it.stopLoading()
            it.destroy()
        },
    )
}

private val OFFLINE_CODES = setOf(
    WebViewClient.ERROR_HOST_LOOKUP,
    WebViewClient.ERROR_CONNECT,
    WebViewClient.ERROR_TIMEOUT,
)
