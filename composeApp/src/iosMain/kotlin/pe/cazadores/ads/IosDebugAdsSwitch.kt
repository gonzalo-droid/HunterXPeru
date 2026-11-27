package pe.cazadores.ads

import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults

/**
 * `-disableAds YES` al lanzar (Xcode) entra por el dominio de argumentos de NSUserDefaults. Maestro pasa
 * `disableAds true` sin guion, que NSUserDefaults no lee: por eso también se busca en los argumentos crudos.
 */
class IosDebugAdsSwitch : DebugAdsSwitch {
    private val defaults = NSUserDefaults.standardUserDefaults
    override var adsDisabled: Boolean
        get() = defaults.boolForKey(KEY) || KEY in NSProcessInfo.processInfo.arguments
        set(value) = defaults.setBool(value, forKey = KEY)

    private companion object {
        const val KEY = "disableAds"
    }
}
