package pe.cazadores.ads

import android.content.Context

class AndroidDebugAdsSwitch(context: Context) : DebugAdsSwitch {
    private val prefs = context.getSharedPreferences("debug_flags", Context.MODE_PRIVATE)
    override var adsDisabled: Boolean
        get() = prefs.getBoolean(KEY, false)
        set(value) = prefs.edit().putBoolean(KEY, value).apply()

    companion object { const val KEY = "disableAds" }
}
