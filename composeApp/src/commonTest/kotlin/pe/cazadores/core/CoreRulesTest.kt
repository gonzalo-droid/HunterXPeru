package pe.cazadores.core

import pe.cazadores.core.text.titleCase
import pe.cazadores.domain.model.DangerLevel
import pe.cazadores.domain.model.RewardClass
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DangerLevelCalculatorTest {
    @Test fun extremoWithAndWithoutAccents() {
        assertEquals(DangerLevel.EXTREMO, DangerLevelCalculator.of(listOf("HOMICIDIO CALIFICADO - ASESINATO")))
        assertEquals(DangerLevel.EXTREMO, DangerLevelCalculator.of(listOf("Sicariato")))
        assertEquals(DangerLevel.EXTREMO, DangerLevelCalculator.of(listOf("ESTAFA", "TERRORISMO")))
    }

    @Test fun muyAltoNormalizesAccents() {
        assertEquals(DangerLevel.MUY_ALTO, DangerLevelCalculator.of(listOf("VIOLACIÓN SEXUAL DE MENOR DE EDAD")))
        assertEquals(DangerLevel.MUY_ALTO, DangerLevelCalculator.of(listOf("extorsión")))
        assertEquals(DangerLevel.MUY_ALTO, DangerLevelCalculator.of(listOf("EXTORSION AGRAVADA")))
    }

    @Test fun altoByDefault() {
        assertEquals(DangerLevel.ALTO, DangerLevelCalculator.of(listOf("TRÁFICO ILÍCITO DE DROGAS")))
        assertEquals(DangerLevel.ALTO, DangerLevelCalculator.of(emptyList()))
    }
}

class RewardClassCalculatorTest {
    @Test fun bordersAreInclusive() {
        assertEquals(RewardClass.S, RewardClassCalculator.of(300_000.0))
        assertEquals(RewardClass.A, RewardClassCalculator.of(299_999.0))
        assertEquals(RewardClass.A, RewardClassCalculator.of(100_000.0))
        assertEquals(RewardClass.B, RewardClassCalculator.of(50_000.0))
        assertEquals(RewardClass.C, RewardClassCalculator.of(49_999.0))
        assertEquals(RewardClass.C, RewardClassCalculator.of(1.0))
    }

    @Test fun noRewardHasNoClass() {
        assertNull(RewardClassCalculator.of(0.0))
        assertNull(RewardClassCalculator.of(-5.0))
    }
}

class RewardFormatterTest {
    private val nbsp = ' '

    @Test fun groupsThousandsWithSpace() {
        assertEquals("S/${nbsp}50${nbsp}000", RewardFormatter.format(50_000.0))
        assertEquals("S/${nbsp}1${nbsp}000${nbsp}000", RewardFormatter.format(1_000_000.0))
        assertEquals("S/${nbsp}500", RewardFormatter.format(500.0))
    }

    @Test fun zeroMeansNoReward() {
        assertNull(RewardFormatter.format(0.0))
    }
}

class PhotoDecoderTest {
    @Test fun stripsDataUrlPrefix() {
        val plain = PhotoDecoder.decode("/9j/4A==")
        val prefixed = PhotoDecoder.decode("data:image/jpeg;base64,/9j/4A==")
        assertContentEquals(plain, prefixed)
        assertEquals(0xFF.toByte(), plain?.first())
    }

    @Test fun toleratesMissingPaddingAndNewlines() {
        assertContentEquals(PhotoDecoder.decode("/9j/4A=="), PhotoDecoder.decode("/9j/\n4A"))
    }

    @Test fun invalidOrEmptyIsNull() {
        assertNull(PhotoDecoder.decode(null))
        assertNull(PhotoDecoder.decode(""))
        assertNull(PhotoDecoder.decode("¿no es base64?"))
    }
}

class TextFormatTest {
    @Test fun titleCaseKeepsConnectorsLowercase() {
        assertEquals("Coronel Portillo", "CORONEL PORTILLO".titleCase())
        assertEquals("Madre de Dios", "MADRE DE DIOS".titleCase())
    }
}
