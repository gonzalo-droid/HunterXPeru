package pe.cazadores.ads

/**
 * Dónde se muestran anuncios (02-DISENO-UX §5). En Ficha solo al final; nunca en Reportar ni Onboarding.
 * Aparte: el bonificado que el usuario acepta antes de Compartir (D33).
 */
enum class AdPlacement { HOME_BANNER, SEARCH_INLINE, SAVED_INLINE, DETAIL_BANNER }

/** Un anuncio cada 8 ítems en las listas. */
const val AD_EVERY_N_ITEMS = 8
