package pe.cazadores.platform

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

/** Intents implícitos desde el contexto de aplicación (por eso FLAG_ACTIVITY_NEW_TASK). */
class AndroidExternalActions(private val context: Context) : ExternalActions {

    override fun dial(telUri: String) {
        launch(Intent(Intent.ACTION_DIAL, Uri.parse(telUri)))
    }

    override fun email(to: String, subject: String, body: String) {
        // Gmail y otros clientes no decodifican "%40" en el mailto ni leen el destinatario de la URI:
        // va sin codificar en "mailto:" y además en EXTRA_EMAIL/SUBJECT/TEXT, que es lo que leen.
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$to")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        launch(intent)
    }

    override fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        launch(Intent.createChooser(send, null))
    }

    override fun shareImage(png: ByteArray, text: String) {
        val uri = try {
            // Un solo archivo que se sobrescribe: la caché no crece con cada cartel compartido.
            val file = File(context.cacheDir, "share/cartel.png")
            file.parentFile?.mkdirs()
            file.writeBytes(png)
            FileProvider.getUriForFile(context, context.packageName + SHARE_AUTHORITY_SUFFIX, file)
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        if (uri == null) {
            share(text)
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            // ClipData: el selector muestra la vista previa y el permiso de lectura llega a la app elegida.
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launch(Intent.createChooser(send, null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    override fun openUrl(url: String) {
        launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun openInAppBrowser(url: String) {
        // Custom Tabs sin la biblioteca androidx.browser: basta el extra de sesión (vacío). Sin navegador que los
        // soporte, el intent abre el navegador normal.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).putExtras(
            android.os.Bundle().apply { putBinder("android.support.customtabs.extra.SESSION", null) },
        )
        if (!launch(intent)) openUrl(url)
    }

    override fun openStoreReview() {
        val pkg = context.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        if (!launch(market)) launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
    }

    private fun launch(intent: Intent): Boolean =
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            // Sin app para manejarlo (p. ej. tablet sin teléfono): no hay nada más que hacer.
            false
        }

    private companion object {
        /** Debe coincidir con el `authorities` del FileProvider en el manifest de androidApp. */
        const val SHARE_AUTHORITY_SUFFIX = ".share"
    }
}
