package pe.cazadores.ads

import platform.Foundation.NSUserDefaults

/** `-disableAds YES` al lanzar (Xcode, Maestro) entra por el dominio de argumentos de NSUserDefaults. */
class IosDebugAdsSwitch : DebugAdsSwitch {
    private val defaults = NSUserDefaults.standardUserDefaults
    override var adsDisabled: Boolean
        get() = defaults.boolForKey("disableAds")
        set(value) = defaults.setBool(value, forKey = "disableAds")
}
