package pe.cazadores.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/** WCAG 2.x: texto ≥ 4.5:1; bordes de control ≥ 3:1. Se verifica en ambos temas. */
class ContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun assertRatio(name: String, fg: Color, bg: Color, minimum: Double) {
        val r = ratio(fg, bg)
        assertTrue(r >= minimum, "$name: ${(r * 100).toInt() / 100.0}:1 < $minimum:1")
    }

    private fun check(c: CazadoresColors, theme: String) {
        listOf(
            "ink/bg" to (c.ink to c.bg),
            "ink/surface" to (c.ink to c.surface),
            "inkMuted/bg" to (c.inkMuted to c.bg),
            "inkMuted/surface" to (c.inkMuted to c.surface),
            "onAction/action" to (c.onAction to c.action),
            "onReward/reward" to (c.onReward to c.reward),
            "onHeader/header" to (c.onHeader to c.header),
            "onHeaderMuted/header" to (c.onHeaderMuted to c.header),
            "onHeaderMuted/headerField" to (c.onHeaderMuted to c.headerField),
            "onLevelExtremo" to (c.onLevelExtremo to c.levelExtremo),
            "onLevelMuyAlto" to (c.onLevelMuyAlto to c.levelMuyAlto),
            "onLevelAlto" to (c.onLevelAlto to c.levelAlto),
            "action/bg (texto rojo)" to (c.action to c.bg),
            "snackbar texto" to (c.bg to c.ink),
            "snackbar acción" to ((if (c.isDark) c.levelExtremo else c.reward) to c.ink),
        ).forEach { (name, pair) -> assertRatio("$theme $name", pair.first, pair.second, 4.5) }

        assertRatio("$theme lineStrong/surface", c.lineStrong, c.surface, 3.0)
        assertRatio("$theme lineStrong/bg", c.lineStrong, c.bg, 3.0)
    }

    @Test fun lightTheme() = check(LightCazadoresColors, "claro")

    @Test fun darkTheme() = check(DarkCazadoresColors, "oscuro")
}
