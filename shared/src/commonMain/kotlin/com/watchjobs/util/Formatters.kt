package com.watchjobs.util

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Clock

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

private fun Int.pad(): String = toString().padStart(2, '0')

fun LocalDate.toBr(): String = "${day.pad()}/${month.number.pad()}/$year"

fun LocalDateTime.toBr(): String = "${date.toBr()} ${hour.pad()}:${minute.pad()}"

// Ex.: "Qui 09:30", usado no cartão da candidatura.
fun LocalDateTime.toShortWeekday(): String {
    val weekday = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "Seg"
        DayOfWeek.TUESDAY -> "Ter"
        DayOfWeek.WEDNESDAY -> "Qua"
        DayOfWeek.THURSDAY -> "Qui"
        DayOfWeek.FRIDAY -> "Sex"
        DayOfWeek.SATURDAY -> "Sáb"
        DayOfWeek.SUNDAY -> "Dom"
    }
    return "$weekday ${date.day.pad()}/${date.month.number.pad()} ${hour.pad()}:${minute.pad()}"
}

private val DATE_REGEX = Regex("""^(\d{1,2})/(\d{1,2})/(\d{4})$""")
private val DATE_TIME_REGEX = Regex("""^(\d{1,2})/(\d{1,2})/(\d{4})\s+(\d{1,2}):(\d{2})$""")

// "dd/mm/aaaa" -> LocalDate. Devolve null se o texto não for uma data válida.
fun parseBrDate(text: String): LocalDate? {
    val match = DATE_REGEX.matchEntire(text.trim()) ?: return null
    val (day, month, year) = match.destructured
    return runCatching { LocalDate(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
}

// "dd/mm/aaaa hh:mm" -> LocalDateTime. Devolve null se o texto não for válido.
fun parseBrDateTime(text: String): LocalDateTime? {
    val match = DATE_TIME_REGEX.matchEntire(text.trim()) ?: return null
    val (day, month, year, hour, minute) = match.destructured
    return runCatching {
        LocalDateTime(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt())
    }.getOrNull()
}

// Aceita "8000", "8.000", "8000,50" e "8.000,50". Devolve null se não for um número.
fun parseMoney(text: String): Double? {
    val clean = text.trim().replace(" ", "")
    if (clean.isEmpty()) return null
    val normalized = when {
        clean.contains(',') -> clean.replace(".", "").replace(',', '.')
        Regex("""^\d{1,3}(\.\d{3})+$""").matches(clean) -> clean.replace(".", "")
        else -> clean
    }
    return normalized.toDoubleOrNull()
}

// 8000.5 -> "8000,50" (para editar no formulário).
fun Double.toInput(): String {
    val cents = (abs(this) * 100).roundToLong()
    val integer = cents / 100
    val fraction = cents % 100
    val sign = if (this < 0) "-" else ""
    return if (fraction == 0L) "$sign$integer" else "$sign$integer,${fraction.toString().padStart(2, '0')}"
}

// 8000.0 -> "8.000"; 8000.5 -> "8.000,50".
fun Double.toMoney(): String {
    val cents = (abs(this) * 100).roundToLong()
    val integer = (cents / 100).toString().reversed().chunked(3).joinToString(".").reversed()
    val fraction = cents % 100
    val sign = if (this < 0) "-" else ""
    return if (fraction == 0L) "$sign$integer" else "$sign$integer,${fraction.toString().padStart(2, '0')}"
}

fun currencySymbol(currency: String?): String = when (currency?.uppercase()) {
    null, "", "BRL" -> "R$"
    "USD" -> "US$"
    "EUR" -> "€"
    "GBP" -> "£"
    else -> currency.uppercase()
}

fun salaryRange(min: Double?, max: Double?, currency: String?): String? {
    val symbol = currencySymbol(currency)
    return when {
        min != null && max != null -> "$symbol ${min.toMoney()}–${max.toMoney()}"
        min != null -> "a partir de $symbol ${min.toMoney()}"
        max != null -> "até $symbol ${max.toMoney()}"
        else -> null
    }
}
