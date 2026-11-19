package pe.cazadores.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeAds(
    canRequest: Boolean,
    /** Estado al que pasa preloadRewarded desde IDLE (LOADING simula una carga que no termina). */
    private val loadsTo: RewardedState = RewardedState.READY,
    private val result: RewardedOutcome = RewardedOutcome.EARNED,
) : AdsController {
    override val canRequestAds = MutableStateFlow(canRequest)
    override val privacyOptionsRequired = MutableStateFlow(false)
    override val rewarded = MutableStateFlow(RewardedState.IDLE)
    var shown = 0
    override fun start() = Unit
    override fun showPrivacyOptions() = Unit
    override fun preloadRewarded() {
        if (canRequestAds.value && rewarded.value == RewardedState.IDLE) rewarded.value = loadsTo
    }
    override fun showRewarded(onResult: (RewardedOutcome) -> Unit) {
        shown++
        rewarded.value = RewardedState.IDLE
        onResult(result)
    }
}

class RewardedShareTest {
    @Test fun withoutConsentSharesDirectlyAndNeverShowsAd() = runTest {
        val ads = FakeAds(canRequest = false)
        assertEquals(RewardedOutcome.FAILED, ads.showRewardedForShare())
        assertEquals(0, ads.shown)
    }

    @Test fun loadFailureSharesDirectly() = runTest {
        val ads = FakeAds(canRequest = true, loadsTo = RewardedState.IDLE)
        assertEquals(RewardedOutcome.FAILED, ads.showRewardedForShare())
        assertEquals(0, ads.shown)
    }

    @Test fun slowLoadTimesOutAndSharesDirectly() = runTest {
        val ads = FakeAds(canRequest = true, loadsTo = RewardedState.LOADING)
        assertEquals(RewardedOutcome.FAILED, ads.showRewardedForShare())
        assertEquals(0, ads.shown)
    }

    @Test fun earnedRewardSharesAndPreloadsNext() = runTest {
        val ads = FakeAds(canRequest = true)
        assertEquals(RewardedOutcome.EARNED, ads.showRewardedForShare())
        assertEquals(1, ads.shown)
        assertEquals(RewardedState.READY, ads.rewarded.value)
    }

    @Test fun onlyClosingEarlyCancelsShare() = runTest {
        assertEquals(RewardedOutcome.DISMISSED, FakeAds(canRequest = true, result = RewardedOutcome.DISMISSED).showRewardedForShare())
        assertFalse(RewardedOutcome.DISMISSED.allowsShare())
        assertTrue(RewardedOutcome.EARNED.allowsShare())
        assertTrue(RewardedOutcome.FAILED.allowsShare())
    }
}
