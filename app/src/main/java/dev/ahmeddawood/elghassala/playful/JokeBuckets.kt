package dev.ahmeddawood.elghassala.playful

import java.util.Calendar

/** Chooses which joke bucket fits an action. Pure, so it is unit-tested. */
object JokeBuckets {

    /** Buckets that fit a saved trip; the generic one is always among them so it keeps being used. */
    fun tripCandidates(riders: Int, farePerPersonCents: Long, hourOfDay: Int, dayOfWeek: Int = -1): List<String> {
        val out = mutableListOf("trip_added")
        if (hourOfDay in 4..7) out += "trip_early"
        if (hourOfDay >= 22 || hourOfDay < 4) out += "trip_late"
        if (riders >= 5) out += "trip_big_group"
        if (riders == 1) out += "trip_solo"
        if (riders == 2) out += "trip_pair"
        if (riders in 3..4) out += "trip_mid_group"
        if (dayOfWeek == Calendar.FRIDAY) out += "trip_weekend"
        if (farePerPersonCents >= 3000) out += "trip_high_fare"
        if (farePerPersonCents in 1..1000) out += "trip_cheap"
        return out
    }

    fun dayOfWeekOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)

    fun hourOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

    private val food = listOf("اكل", "أكل", "غدا", "غداء", "عشا", "عشاء", "فطار", "فطور", "كشري", "برجر", "بيتزا", "شاورما", "طعمية", "فول", "سندوتش", "ساندوتش", "مطعم", "food", "lunch", "dinner", "breakfast", "pizza", "burger", "sandwich")
    private val coffee = listOf("قهوة", "قهوه", "شاي", "نسكافيه", "كابتشينو", "كافيه", "كوفي", "coffee", "tea", "cafe", "latte")

    private val transport = listOf("بنزين", "تاكسي", "اوبر", "أوبر", "اجرة", "أجرة", "مواصلات", "ميكروباص", "توكتوك", "تذكرة", "جراج", "كارير", "taxi", "uber", "fuel", "gas", "fare", "ticket", "parking", "careem")
    private val shopping = listOf("هدوم", "ملابس", "سوبرماركت", "مول", "شوبينج", "تسوق", "مشتريات", "جزمة", "حذاء", "shopping", "mall", "clothes", "groceries", "market")

    fun expenseCandidates(title: String, totalCents: Long = 0): List<String> {
        val t = title.lowercase()
        val out = mutableListOf("expense_added")
        if (coffee.any { it in t }) out += "expense_coffee"
        else if (food.any { it in t }) out += "expense_food"
        else if (transport.any { it in t }) out += "expense_transport"
        else if (shopping.any { it in t }) out += "expense_shopping"
        if (totalCents >= 50000) out += "expense_big"
        if (totalCents in 1..2000) out += "expense_small"
        return out
    }

    /** Day-start bucket by hour: morning, evening or the generic one. */
    fun dayStartBucket(hour: Int): String = when (hour) {
        in 4..11 -> "day_start_morning"
        in 18..23 -> "day_start_evening"
        else -> "day_start"
    }

    fun dayCloseBucket(hour: Int): String = if (hour >= 23 || hour < 4) "day_close_late" else "day_close"

    fun transferBucket(base: String, amountCents: Long): String = when {
        amountCents >= 100000 -> "transfer_big"
        amountCents in 1..3000 -> "transfer_small"
        else -> base
    }
}
