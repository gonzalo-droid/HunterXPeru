package pe.cazadores.core

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Fechas cortas en formato peruano: `16/11/2026` y `16/11 09:40`. */
object DateFormatter {
    private fun Int.two() = toString().padStart(2, '0')

    fun date(epochMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
        val t = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone)
        return "${t.day.two()}/${t.month.ordinal.plus(1).two()}/${t.year}"
    }

    fun dateTime(epochMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
        val t = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone)
        return "${t.day.two()}/${t.month.ordinal.plus(1).two()} ${t.hour.two()}:${t.minute.two()}"
    }
}
