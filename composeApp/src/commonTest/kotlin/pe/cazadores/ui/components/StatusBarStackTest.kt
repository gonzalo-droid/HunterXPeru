package pe.cazadores.ui.components

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatusBarStackTest {
    private val base = Any()
    private val screen = Any()

    @AfterTest fun clear() {
        StatusBarStack.remove(base)
        StatusBarStack.remove(screen)
    }

    @Test fun themeChangeOnBaseDoesNotOverrideVisibleScreen() {
        StatusBarStack.push(base, true) // App en oscuro
        StatusBarStack.push(screen, true) // Cabecera oscura de "Sobre este proyecto"
        // Cambio a claro: la base pasa a íconos oscuros, pero manda la pantalla de arriba.
        assertEquals(true, StatusBarStack.update(base, false))
        // Al salir de la pantalla queda la base ya actualizada.
        assertEquals(false, StatusBarStack.remove(screen))
    }

    @Test fun updateOfTopEntryAppliesNewValue() {
        StatusBarStack.push(base, false)
        assertEquals(true, StatusBarStack.update(base, true))
    }

    @Test fun updateOfUnknownTokenIsIgnored() {
        assertNull(StatusBarStack.update(Any(), true))
    }
}
