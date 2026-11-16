package pe.cazadores.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepLinksTest {
    @Test fun parsesValidLinks() {
        assertEquals("D1321FADB4B9D70B17504891006156FC", DeepLinks.parseCriminalHash("cazadores://criminal/D1321FADB4B9D70B17504891006156FC"))
        assertEquals("ABC123", DeepLinks.parseCriminalHash("cazadores://criminal/ABC123/?utm=x"))
    }

    @Test fun rejectsOtherOrUnsafeLinks() {
        assertNull(DeepLinks.parseCriminalHash("https://recompensas.pe/criminal/ABC"))
        assertNull(DeepLinks.parseCriminalHash("cazadores://criminal/"))
        assertNull(DeepLinks.parseCriminalHash("cazadores://criminal/../../etc"))
        assertNull(DeepLinks.parseCriminalHash(null))
    }

    @Test fun handleStoresPendingHashUntilConsumed() {
        assertTrue(DeepLinks.handle("cazadores://criminal/XYZ"))
        assertEquals("XYZ", DeepLinks.pendingHash.value)
        DeepLinks.consume()
        assertNull(DeepLinks.pendingHash.value)
        assertFalse(DeepLinks.handle("otro://x"))
    }
}
