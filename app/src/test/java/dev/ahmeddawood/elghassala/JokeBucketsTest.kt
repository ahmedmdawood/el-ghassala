package dev.ahmeddawood.elghassala

import dev.ahmeddawood.elghassala.playful.JokeBuckets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class JokeBucketsTest {

    @Test
    fun trip_genericAlwaysPresent() {
        assertEquals(listOf("trip_added"), JokeBuckets.tripCandidates(3, 2000, 14))
    }

    @Test
    fun trip_contextBuckets() {
        assertTrue("trip_early" in JokeBuckets.tripCandidates(2, 2000, 6))
        assertTrue("trip_late" in JokeBuckets.tripCandidates(2, 2000, 23))
        assertTrue("trip_late" in JokeBuckets.tripCandidates(2, 2000, 2))
        assertTrue("trip_big_group" in JokeBuckets.tripCandidates(5, 2000, 14))
        assertTrue("trip_solo" in JokeBuckets.tripCandidates(1, 2000, 14))
        assertTrue("trip_high_fare" in JokeBuckets.tripCandidates(2, 3500, 14))
        assertTrue("trip_cheap" in JokeBuckets.tripCandidates(2, 800, 14))
        assertFalse("trip_cheap" in JokeBuckets.tripCandidates(2, 0, 14))
    }

    @Test
    fun expense_byTitle() {
        assertTrue("expense_coffee" in JokeBuckets.expenseCandidates("قهوة الصبح"))
        assertTrue("expense_food" in JokeBuckets.expenseCandidates("غدا كشري"))
        assertEquals(listOf("expense_added"), JokeBuckets.expenseCandidates("تذكرة"))
    }

    /** Every bucket file must be valid JSON, unique across the whole corpus, free of em dashes, and every bucket the code can pick must exist. */
    @Test
    fun corpus_isClean() {
        val dir = File("src/main/assets/jokes")
        val files = dir.listFiles { f -> f.name.endsWith(".json") }!!.toList()
        assertTrue(files.isNotEmpty())
        val seen = HashMap<String, String>()
        var total = 0
        files.forEach { f ->
            val arr = org.json.JSONArray(f.readText(Charsets.UTF_8))
            val items = List(arr.length()) { arr.getString(it) }
            assertEquals("duplicates in ${f.name}", items.size, items.toSet().size)
            items.forEach { j ->
                assertTrue("blank joke in ${f.name}", j.isNotBlank())
                assertFalse("em dash in ${f.name}: $j", j.contains('\u2014'))
                val prev = seen.put(j, f.name)
                assertTrue("joke repeated in $prev and ${f.name}: $j", prev == null)
            }
            total += items.size
        }
        println("jokes total = $total")
        val names = files.map { it.name.removeSuffix(".json") }.toSet()
        listOf("trip_added", "trip_early", "trip_late", "trip_big_group", "trip_solo", "trip_high_fare", "trip_cheap",
            "expense_added", "expense_food", "expense_coffee", "trip_one_payer", "day_start", "day_close", "receipt", "spin")
            .forEach { assertTrue("missing bucket $it", it in names) }
    }
}
