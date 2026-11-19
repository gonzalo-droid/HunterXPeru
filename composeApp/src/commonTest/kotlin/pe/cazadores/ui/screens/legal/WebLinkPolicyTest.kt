package pe.cazadores.ui.screens.legal

import kotlin.test.Test
import kotlin.test.assertEquals
import pe.cazadores.core.config.AppConfig

class WebLinkPolicyTest {
    private val home = "https://nekodojo.app/apps/peruxhunter/privacy"

    private fun decide(target: String) = WebLinkPolicy.decide(target, home)

    @Test
    fun sameHostOverHttpsStaysInApp() {
        assertEquals(LinkDecision.IN_APP, decide(home))
        assertEquals(LinkDecision.IN_APP, decide("https://nekodojo.app/apps/peruxhunter/terms"))
        assertEquals(LinkDecision.IN_APP, decide("https://NEKODOJO.app/en/apps/peruxhunter/privacy#contacto"))
    }

    @Test
    fun otherHostsGoToTheSystemBrowser() {
        assertEquals(LinkDecision.EXTERNAL, decide("https://policies.google.com/privacy"))
        assertEquals(LinkDecision.EXTERNAL, decide("https://nekodojo.app.evil.example/apps"))
        assertEquals(LinkDecision.EXTERNAL, decide("https://www.nekodojo.app/apps"))
        // Mismo host pero sin TLS: fuera de la vista web (el tráfico en claro está bloqueado).
        assertEquals(LinkDecision.EXTERNAL, decide("http://nekodojo.app/apps"))
    }

    @Test
    fun mailAndPhoneGoToTheirApps() {
        assertEquals(LinkDecision.EXTERNAL, decide("mailto:contacto@example.com"))
        assertEquals(LinkDecision.EXTERNAL, decide("tel:+5118000"))
    }

    @Test
    fun otherSchemesAreBlocked() {
        assertEquals(LinkDecision.BLOCK, decide("javascript:alert(1)"))
        assertEquals(LinkDecision.BLOCK, decide("intent://scan#Intent;scheme=zxing;end"))
        assertEquals(LinkDecision.BLOCK, decide("file:///etc/hosts"))
        assertEquals(LinkDecision.BLOCK, decide("about:blank"))
        assertEquals(LinkDecision.BLOCK, decide("no-scheme"))
    }

    @Test
    fun sourceSiteStaysInAppOnlyOnItsOwnHost() {
        val src = LegalPage.SOURCE.url
        assertEquals(LinkDecision.IN_APP, WebLinkPolicy.decide("https://recompensas.pe/recompensas", src))
        assertEquals(LinkDecision.EXTERNAL, WebLinkPolicy.decide("https://www.mininter.gob.pe/", src))
    }

    @Test
    fun pagesMapToConfiguredUrls() {
        assertEquals(AppConfig.PRIVACY_POLICY_URL, LegalPage.PRIVACY.url)
        assertEquals(AppConfig.TERMS_URL, LegalPage.TERMS.url)
        assertEquals(AppConfig.SOURCE_URL, LegalPage.SOURCE.url)
        LegalPage.entries.forEach { assertEquals(LinkDecision.IN_APP, WebLinkPolicy.decide(it.url, it.url)) }
    }
}
