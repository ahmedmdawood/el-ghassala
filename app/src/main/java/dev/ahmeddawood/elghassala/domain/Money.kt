package dev.ahmeddawood.elghassala.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** Money is always stored as piasters (1/100 EGP) in a Long. Never floats. */
object Money {

    /** Privacy mode (وضع الستر): when true every amount shown in the UI is masked. Snapshot state, so screens recompose. */
    var hidden by mutableStateOf(false)

    const val MASK = "•••"

    /** For display in the UI: masked when privacy mode is on. Exports use [formatWithCurrency] directly. */
    fun shown(cents: Long, locale: Locale = Locale.getDefault()): String =
        if (hidden) MASK else formatWithCurrency(cents, locale)

    fun shownPlain(cents: Long, locale: Locale = Locale.getDefault()): String =
        if (hidden) MASK else format(cents, locale)

    /** Parses user input like "12.5", "١٢٫٥", "12,50" into piasters. Null when invalid. */
    fun parse(input: String?): Long? {
        var s = input?.trim() ?: return null
        if (s.isEmpty()) return null
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                in '٠'..'٩' -> sb.append('0' + (c - '٠'))
                in '۰'..'۹' -> sb.append('0' + (c - '۰'))
                '٫', ',' -> sb.append('.')
                '٬', ' ', ' ' -> {}
                else -> sb.append(c)
            }
        }
        s = sb.toString()
        if (s.count { it == '.' } > 1) return null
        val parts = s.split('.')
        val pounds = parts[0].toLongOrNull() ?: return null
        if (pounds < 0) return null
        var cents = 0L
        if (parts.size == 2) {
            val frac = parts[1]
            if (frac.isEmpty() || frac.length > 2 || frac.any { !it.isDigit() }) return null
            cents = frac.padEnd(2, '0').toLong()
        }
        return pounds * 100 + cents
    }

    /** Formats piasters for display, e.g. 1250 -> "12.50"; 1000 -> "10". */
    fun format(cents: Long, locale: Locale = Locale.getDefault()): String {
        val negative = cents < 0
        val absCents = abs(cents)
        val value = absCents / 100.0
        val nf = NumberFormat.getNumberInstance(locale)
        nf.minimumFractionDigits = if (absCents % 100 == 0L) 0 else 2
        nf.maximumFractionDigits = 2
        nf.isGroupingUsed = true
        val out = nf.format(value)
        return if (negative) "-$out" else out
    }

    /** Formats with the currency label. */
    fun formatWithCurrency(cents: Long, locale: Locale = Locale.getDefault()): String {
        val isArabic = locale.language == "ar"
        val label = if (isArabic) "ج.م" else "EGP"
        return if (isArabic) "${format(cents, locale)} $label" else "$label ${format(cents, locale)}"
    }
}
