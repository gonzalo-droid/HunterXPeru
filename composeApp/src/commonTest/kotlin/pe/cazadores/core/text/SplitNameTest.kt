package pe.cazadores.core.text

import kotlin.test.Test
import kotlin.test.assertEquals

class SplitNameTest {
    @Test fun splitsSurnamesAndGivenNames() {
        assertEquals(
            SplitName("MONTEVERDE BUSSALLEU", "Gonzalo Eduardo"),
            "MONTEVERDE BUSSALLEU, GONZALO EDUARDO".splitDisplayName(),
        )
    }

    @Test fun withoutCommaKeepsOneBlock() {
        assertEquals(SplitName("GONZALO EDUARDO MONTEVERDE", null), " GONZALO EDUARDO MONTEVERDE ".splitDisplayName())
    }

    @Test fun emptyPartsFallBack() {
        assertEquals(SplitName("TORRES", null), "TORRES, ".splitDisplayName())
        assertEquals(SplitName("Rodrigo", null), ", RODRIGO".splitDisplayName())
    }
}
