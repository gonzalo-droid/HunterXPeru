package pe.cazadores.core.config

/** Constantes únicas de la app. Cambiar aquí, nunca en las pantallas. */
object AppConfig {
    const val API_BASE_URL = "https://sispasvehapp.mininter.gob.pe/api-recompensas"
    const val API_HOST = "sispasvehapp.mininter.gob.pe"

    /** Línea gratuita del Programa de Recompensas. La app Flutter también mostraba "1818": sin confirmar, no se usa. */
    const val REPORT_PHONE = "0-800-40-007"
    const val REPORT_PHONE_URI = "tel:080040007"

    /** TODO(confirmar): la app Flutter usaba este correo sin verificar que el programa lo atienda. */
    const val REPORT_EMAIL = "denuncias@mininter.gob.pe"

    /** TODO(Gonzalo): correo propio para comentarios (no usar @recompensas.pe, que es de terceros). */
    const val FEEDBACK_EMAIL = "TODO-feedback@example.com"

    /** TODO(Gonzalo): URLs propias publicadas antes de subir a tienda (Play las exige con AdMob). */
    const val PRIVACY_POLICY_URL = "https://example.com/cazadores/privacidad"
    const val TERMS_URL = "https://example.com/cazadores/terminos"

    const val SOURCE_URL = "https://recompensas.pe"
    const val DEEP_LINK_BASE = "cazadores://criminal/"

    const val PAGE_SIZE = 20
}
