package pe.cazadores.ads

/** Dónde se muestran anuncios (02-DISENO-UX §5). Nunca en Ficha, Reportar ni Onboarding. */
enum class AdPlacement { HOME_BANNER, SEARCH_INLINE, SAVED_INLINE }

/** Un anuncio cada 8 ítems en las listas. */
const val AD_EVERY_N_ITEMS = 8
