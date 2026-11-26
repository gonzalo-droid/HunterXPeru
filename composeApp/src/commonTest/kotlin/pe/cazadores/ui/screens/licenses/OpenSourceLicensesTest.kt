package pe.cazadores.ui.screens.licenses

import kotlin.test.Test
import kotlin.test.assertTrue

class OpenSourceLicensesTest {
    @Test fun everyEntryHasTextAndAnHttpsLink() {
        (OPEN_SOURCE_LICENSES + THIRD_PARTY_SDKS).forEach {
            assertTrue(it.name.isNotBlank() && it.author.isNotBlank() && it.license.isNotBlank(), it.toString())
            assertTrue(it.url.startsWith("https://"), it.url)
        }
    }
}
