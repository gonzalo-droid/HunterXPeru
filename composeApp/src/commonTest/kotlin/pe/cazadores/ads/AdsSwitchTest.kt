package pe.cazadores.ads

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdsSwitchTest {
    @Test fun releaseIgnoresTheDebugSwitch() = assertTrue(adsActive(builtWithAds = true, isDebug = false, debugSwitchOff = true))
    @Test fun debugSwitchTurnsAdsOff() = assertFalse(adsActive(builtWithAds = true, isDebug = true, debugSwitchOff = true))
    @Test fun buildWithoutAdsStaysOff() = assertFalse(adsActive(builtWithAds = false, isDebug = false, debugSwitchOff = false))
    @Test fun defaultDebugShowsAds() = assertTrue(adsActive(builtWithAds = true, isDebug = true, debugSwitchOff = false))
}
