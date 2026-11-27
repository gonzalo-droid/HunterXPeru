package pe.cazadores.ui.screens.legal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDataNotAllowed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorInternationalRoamingOff
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut
import platform.Foundation.NSURLRequest
import platform.UIKit.UIUserInterfaceStyle
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun PlatformWebView(
    url: String,
    isDark: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onError: (WebLoadError) -> Unit,
    onExternalLink: (String) -> Unit,
    modifier: Modifier,
) {
    val loadingChange = rememberUpdatedState(onLoadingChange)
    val error = rememberUpdatedState(onError)
    val external = rememberUpdatedState(onExternalLink)
    // WKWebView guarda el delegado como referencia débil: se retiene aquí mientras la vista viva.
    val delegate = remember(url) { LegalNavigationDelegate(url, loadingChange, error, external) }
    val style = if (isDark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight

    UIKitView(
        modifier = modifier,
        factory = {
            val configuration = WKWebViewConfiguration().apply {
                // Solo páginas propias ya renderizadas (D28); los sitios ajenos van al navegador in-app (D42).
                defaultWebpagePreferences.allowsContentJavaScript = false
                // Sin cookies ni caché persistentes del sitio.
                websiteDataStore = WKWebsiteDataStore.nonPersistentDataStore()
            }
            WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
                navigationDelegate = delegate
                allowsBackForwardNavigationGestures = true
                overrideUserInterfaceStyle = style
                NSURL.URLWithString(url)?.let { loadRequest(NSURLRequest.requestWithURL(it)) }
            }
        },
        update = { it.overrideUserInterfaceStyle = style },
        onRelease = {
            it.stopLoading()
            it.navigationDelegate = null
        },
    )
}

private class LegalNavigationDelegate(
    private val home: String,
    private val onLoadingChange: State<(Boolean) -> Unit>,
    private val onError: State<(WebLoadError) -> Unit>,
    private val onExternalLink: State<(String) -> Unit>,
) : NSObject(), WKNavigationDelegateProtocol {
    private var failed = false

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val target = decidePolicyForNavigationAction.request.URL?.absoluteString
        val mainFrame = decidePolicyForNavigationAction.targetFrame?.mainFrame
        val decision = target?.let { WebLinkPolicy.decide(it, home) } ?: LinkDecision.BLOCK
        when {
            decision == LinkDecision.IN_APP && mainFrame == null -> {
                // target="_blank" del mismo sitio: se abre en esta misma vista.
                webView.loadRequest(decidePolicyForNavigationAction.request)
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            }
            decision == LinkDecision.IN_APP -> decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            decision == LinkDecision.EXTERNAL && mainFrame != false -> {
                target?.let { onExternalLink.value(it) }
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            }
            else -> decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        }
    }

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationResponse: WKNavigationResponse,
        decisionHandler: (WKNavigationResponsePolicy) -> Unit,
    ) {
        val status = (decidePolicyForNavigationResponse.response as? NSHTTPURLResponse)?.statusCode ?: 0L
        if (decidePolicyForNavigationResponse.forMainFrame && status >= 400) {
            // Un 404 o 5xx mostraría la página de error del sitio dentro de la app.
            fail(WebLoadError.OTHER)
            decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyCancel)
        } else {
            decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow)
        }
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        if (!failed) onLoadingChange.value(true)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        if (!failed) onLoadingChange.value(false)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
        handle(withError)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
        handle(withError)
    }

    private fun handle(error: NSError) {
        val code = error.code
        // Cancelada por otra navegación o por la política (enlace externo, respuesta ≥ 400): no es un fallo nuevo.
        if (error.domain == NSURLErrorDomain && code == NSURLErrorCancelled) return
        if (error.domain == WEBKIT_ERROR_DOMAIN && code == FRAME_LOAD_INTERRUPTED_BY_POLICY_CHANGE) {
            if (!failed) onLoadingChange.value(false)
            return
        }
        val offline = error.domain == NSURLErrorDomain && code in OFFLINE_CODES
        fail(if (offline) WebLoadError.OFFLINE else WebLoadError.OTHER)
    }

    private fun fail(reason: WebLoadError) {
        if (failed) return
        failed = true
        onError.value(reason)
    }
}

private const val WEBKIT_ERROR_DOMAIN = "WebKitErrorDomain"
private const val FRAME_LOAD_INTERRUPTED_BY_POLICY_CHANGE = 102L

private val OFFLINE_CODES = setOf(
    NSURLErrorNotConnectedToInternet,
    NSURLErrorNetworkConnectionLost,
    NSURLErrorCannotFindHost,
    NSURLErrorCannotConnectToHost,
    NSURLErrorDNSLookupFailed,
    NSURLErrorTimedOut,
    NSURLErrorDataNotAllowed,
    NSURLErrorInternationalRoamingOff,
)
