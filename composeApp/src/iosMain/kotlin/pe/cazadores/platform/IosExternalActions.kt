package pe.cazadores.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import pe.cazadores.core.config.AppConfig
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

class IosExternalActions : ExternalActions {

    override fun dial(telUri: String) = open(telUri)

    override fun email(to: String, subject: String, body: String) {
        open("mailto:$to?subject=${subject.encoded()}&body=${body.encoded()}")
    }

    override fun share(text: String) = present(listOf(text))

    override fun shareImage(png: ByteArray, text: String) {
        val image = UIImage.imageWithData(png.toNSData())
        if (image == null) {
            share(text)
            return
        }
        present(listOf(image, text))
    }

    private fun present(items: List<Any>) {
        val presenter = topViewController() ?: return
        val controller = UIActivityViewController(activityItems = items, applicationActivities = null)
        // iPad: la hoja necesita un ancla.
        controller.popoverPresentationController?.sourceView = presenter.view
        presenter.presentViewController(controller, animated = true, completion = null)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ByteArray.toNSData(): NSData =
        if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }

    override fun openUrl(url: String) = open(url)

    override fun openInAppBrowser(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        val presenter = topViewController() ?: return open(url)
        presenter.presentViewController(SFSafariViewController(uRL = nsUrl), animated = true, completion = null)
    }

    override fun openStoreReview() {
        if (AppConfig.IOS_APP_STORE_ID.isNotEmpty()) {
            open("itms-apps://apps.apple.com/app/id${AppConfig.IOS_APP_STORE_ID}?action=write-review")
            return
        }
        val scene = UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>().firstOrNull() ?: return
        SKStoreReviewController.requestReviewInScene(scene)
    }

    private fun open(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun String.encoded(): String =
        (this as NSString).stringByAddingPercentEncodingWithAllowedCharacters(
            NSCharacterSet.URLQueryAllowedCharacterSet,
        )?.replace("&", "%26")?.replace("+", "%2B") ?: this

    private fun topViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
        var top = window?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        return top
    }
}
