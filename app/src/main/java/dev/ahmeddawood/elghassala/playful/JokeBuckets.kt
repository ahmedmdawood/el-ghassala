package dev.ahmeddawood.elghassala.playful

import java.util.Calendar

/** Chooses which joke bucket fits an action. Pure, so it is unit-tested. */
object JokeBuckets {

    /** Buckets that fit a saved trip; the generic one is always among them so it keeps being used. */
    fun tripCandidates(riders: Int, farePerPersonCents: Long, hourOfDay: Int): List<String> {
        val out = mutableListOf("trip_added")
        if (hourOfDay in 4..7) out += "trip_early"
        if (hourOfDay >= 22 || hourOfDay < 4) out += "trip_late"
        if (riders >= 5) out += "trip_big_group"
        if (riders == 1) out += "trip_solo"
        if (farePerPersonCents >= 3000) out += "trip_high_fare"
        if (farePerPersonCents in 1..1000) out += "trip_cheap"
        return out
    }

    fun hourOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

    private val food = listOf("اكل", "أكل", "غدا", "غداء", "عشا", "عشاء", "فطار", "فطور", "كشري", "برجر", "بيتزا", "شاورما", "طعمية", "فول", "سندوتش", "ساندوتش", "مطعم", "food", "lunch", "dinner", "breakfast", "pizza", "burger", "sandwich")
    private val coffee = listOf("قهوة", "قهوه", "شاي", "نسكافيه", "كابتشينو", "كافيه", "كوفي", "coffee", "tea", "cafe", "latte")

    fun expenseCandidates(title: String): List<String> {
        val t = title.lowercase()
        val out = mutableListOf("expense_added")
        if (coffee.any { it in t }) out += "expense_coffee"
        else if (food.any { it in t }) out += "expense_food"
        return out
    }
}
