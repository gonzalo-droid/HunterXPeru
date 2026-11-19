package pe.cazadores.ads

import kotlin.test.Test
import kotlin.test.assertEquals

class InlineAdCadenceTest {
    private fun rowsWithAd(perRow: Int, rows: Int = 9) = (0 until rows).filter { adAfterRow(it, perRow) }
    @Test fun oneColumn() = assertEquals(listOf(7), rowsWithAd(1, 9))
    @Test fun twoColumns() = assertEquals(listOf(3, 7), rowsWithAd(2))
    @Test fun threeColumns() = assertEquals(listOf(2, 5, 7), rowsWithAd(3))
}
