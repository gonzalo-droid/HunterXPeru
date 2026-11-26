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

    /** Correo de soporte de la app (el mismo que publica nekodojo.app/apps/peruxhunter/support). */
    const val SUPPORT_EMAIL = "contacto@gondroid.dev"

    /** ID numérico de App Store Connect (Información de la app → ID de Apple). */
    const val IOS_APP_STORE_ID = "6836196201"

    /** Páginas legales propias; se abren dentro de la app. */
    const val PRIVACY_POLICY_URL = "https://nekodojo.app/apps/peruxhunter/privacy"
    const val TERMS_URL = "https://nekodojo.app/apps/peruxhunter/terms"

    /**
     * Enlace único de descarga que acompaña a lo que se comparte (cartel y "Compartir la app"): la web
     * manda cada teléfono a su tienda y a la landing si esa tienda aún no está publicada (neko-dojo #35).
     */
    const val DOWNLOAD_URL = "https://nekodojo.app/apps/peruxhunter/download"

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
