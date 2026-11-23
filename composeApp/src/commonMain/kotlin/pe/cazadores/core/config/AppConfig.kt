package pe.cazadores.core.config

/** Constantes únicas de la app. Cambiar aquí, nunca en las pantallas. */
object AppConfig {
    const val API_BASE_URL = "https://sispasvehapp.mininter.gob.pe/api-recompensas"
    const val API_HOST = "sispasvehapp.mininter.gob.pe"

    /** Línea gratuita del Programa de Recompensas (principal). */
    const val REPORT_PHONE = "0-800-40-007"
    const val REPORT_PHONE_URI = "tel:080040007"

    /** Línea alternativa, confirmada como vigente. */
    const val REPORT_PHONE_ALT = "1818"
    const val REPORT_PHONE_ALT_URI = "tel:1818"

    /** Correo de denuncias, confirmado como vigente. */
    const val REPORT_EMAIL = "denuncias@mininter.gob.pe"

    /** ID numérico de App Store Connect (Información de la app → ID de Apple). */
    const val IOS_APP_STORE_ID = "6836196201"

    /** Páginas legales propias; se abren dentro de la app. */
    const val PRIVACY_POLICY_URL = "https://nekodojo.app/apps/peruxhunter/privacy"
    const val TERMS_URL = "https://nekodojo.app/apps/peruxhunter/terms"

    /** Enlaces de descarga que acompañan a lo que se comparte (cartel y "Compartir la app"). */
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.nekodojo.peruxhunter"
    const val APP_STORE_URL = "https://apps.apple.com/app/id$IOS_APP_STORE_ID"

    const val SOURCE_URL = "https://recompensas.pe"
    const val MININTER_URL = "https://www.gob.pe/mininter"
    const val DEEP_LINK_BASE = "cazadores://criminal/"

    const val PAGE_SIZE = 20

    /**
     * Padrón semanal que publica `sync/sync_requisitoriados.py` (docs/DECISIONES.md D15/D20).
     * Cualquier archivo: `$STORAGE_BASE_URL/o/<ruta con / como %2F>?alt=media`.
     */
    const val STORAGE_BASE_URL = "https://firebasestorage.googleapis.com/v0/b/peruxhunter.firebasestorage.app"
    const val MANIFEST_PATH = "data/manifest.json"
}
