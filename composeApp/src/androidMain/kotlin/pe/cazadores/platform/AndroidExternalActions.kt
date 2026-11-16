package pe.cazadores.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Intents implícitos desde el contexto de aplicación (por eso FLAG_ACTIVITY_NEW_TASK). */
class AndroidExternalActions(private val context: Context) : ExternalActions {

    override fun dial(telUri: String) = launch(Intent(Intent.ACTION_DIAL, Uri.parse(telUri)))

    override fun email(to: String, subject: String, body: String) {
        val uri = Uri.parse("mailto:" + Uri.encode(to)).buildUpon()
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build()
        launch(Intent(Intent.ACTION_SENDTO, uri))
    }

    override fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        launch(Intent.createChooser(send, null))
    }

    override fun openUrl(url: String) = launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun launch(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // Sin app para manejarlo (p. ej. tablet sin teléfono): no hay nada más que hacer.
        }
    }
}
