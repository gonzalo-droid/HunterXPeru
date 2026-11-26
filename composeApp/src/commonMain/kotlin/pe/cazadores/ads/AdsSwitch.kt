package pe.cazadores.ads

/**
 * Interruptor guardado en el dispositivo para apagar los anuncios sin recompilar (base ADS-06), para capturas y
 * flujos de Maestro. Android lo enciende con el extra `disableAds` del intent; iOS, con `-disableAds YES`.
 */
interface DebugAdsSwitch {
    var adsDisabled: Boolean
}

/** Los anuncios corren si el build los trae y, solo en debug, nadie los apagó. En release el interruptor no cuenta. */
fun adsActive(builtWithAds: Boolean, isDebug: Boolean, debugSwitchOff: Boolean): Boolean =
    builtWithAds && !(isDebug && debugSwitchOff)
