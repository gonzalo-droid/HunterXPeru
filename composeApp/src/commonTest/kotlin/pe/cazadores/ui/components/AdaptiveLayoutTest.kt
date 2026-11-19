package pe.cazadores.ui.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveLayoutTest {
    @Test fun gutterIsScreenPaddingOnPhones() = assertEquals(20.dp, contentGutter(440.dp))
    @Test fun gutterCentersOnTablets() = assertEquals(156.dp, contentGutter(1032.dp))
    @Test fun gutterAtExactMax() = assertEquals(20.dp, contentGutter(760.dp))
    @Test fun phoneKeepsTwoColumns() = assertEquals(2, posterColumnsFor(400.dp, 1f))
    @Test fun thresholdGivesThreeColumns() = assertEquals(3, posterColumnsFor(560.dp, 1f))
    @Test fun justBelowThreshold() = assertEquals(2, posterColumnsFor(559.dp, 1f))
    @Test fun tabletGetsThreeColumns() = assertEquals(3, posterColumnsFor(720.dp, 1f))
    @Test fun largeFontIsOneColumnEvenOnTablet() = assertEquals(1, posterColumnsFor(720.dp, 2f))
}
