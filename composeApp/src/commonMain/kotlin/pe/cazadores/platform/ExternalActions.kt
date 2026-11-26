package pe.cazadores.platform

/** Acciones que salen de la app. La app nunca envía datos por su cuenta: solo abre la app del sistema. */
interface ExternalActions {
    /** Abre el marcador con el número (DIAL en Android: no requiere CALL_PHONE). */
    fun dial(telUri: String)
    fun email(to: String, subject: String, body: String)
    fun share(text: String)

    /** Comparte una imagen PNG (el cartel) con [text] como acompañante en la hoja del sistema. */
    fun shareImage(png: ByteArray, text: String)
    fun openUrl(url: String)

    /** Abre un sitio ajeno sin salir de la app: Custom Tabs (Android) o SFSafariViewController (iOS). Base LNK-01. */
    fun openInAppBrowser(url: String)

    /** Abre la ficha de la app en su tienda (Play / App Store) para dejar reseña. */
    fun openStoreReview()
}
