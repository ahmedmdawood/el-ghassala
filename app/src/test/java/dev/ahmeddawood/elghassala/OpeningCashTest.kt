package dev.ahmeddawood.elghassala

import dev.ahmeddawood.elghassala.settings.OpeningCashStore
import org.junit.Assert.*
import org.junit.Test

class OpeningCashTest {
    @Test fun roundTripAndPool() {
        val cash = mapOf(1L to 2000L, 2L to 1500L, 3L to 0L)
        val days = mapOf(7L to cash)
        assertEquals(days, OpeningCashStore.parse(OpeningCashStore.encode(days)))
        assertEquals(3500L, OpeningCashStore.pool(cash))
        assertEquals(emptyMap<Long, Long>(), OpeningCashStore.parse(null)[8L].orEmpty())
    }
    @Test fun missingAndInvalidDoNotBreakOldDays() {
        assertTrue(OpeningCashStore.parse(null).isEmpty())
        assertTrue(OpeningCashStore.parse("broken").isEmpty())
        assertEquals(0L, OpeningCashStore.pool(emptyMap()))
    }
}
