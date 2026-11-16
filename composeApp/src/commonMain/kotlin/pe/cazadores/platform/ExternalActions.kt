package pe.cazadores.platform

/** Acciones que salen de la app. La app nunca envía datos por su cuenta: solo abre la app del sistema. */
interface ExternalActions {
    /** Abre el marcador con el número (DIAL en Android: no requiere CALL_PHONE). */
    fun dial(telUri: String)
    fun email(to: String, subject: String, body: String)
    fun share(text: String)
    fun openUrl(url: String)
}
