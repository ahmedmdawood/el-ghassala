package dev.ahmeddawood.elghassala

import dev.ahmeddawood.elghassala.domain.BalanceCalculator
import org.junit.Assert.*
import org.junit.Test

class TransportEmissionTest {
    @Test fun receivedSettlementClearsRatherThanDoublesDebt() {
        val trip = BalanceCalculator.TransportRecord(1L, 1L, 1700L, mapOf(1L to 2000L, 2L to 1500L), 0L)
        val payment = BalanceCalculator.TransferRecord(1L, null, 2L, 1L, 200L, true, 1L)
        val ledgers = BalanceCalculator.ledgers(listOf(trip), emptyList(), listOf(payment), emptyList(), { true })
        assertEquals(0L, ledgers.getValue(1L).netBalanceCents)
        assertEquals(0L, ledgers.getValue(2L).netBalanceCents)
    }

    /** Independent Room flows may deliver a trip before its participant rows. */
    @Test fun incompleteEmissionThenParticipantsArrive() {
        val trip = BalanceCalculator.TransportRecord(1L, 1L, 1700L, emptyMap(), 0L)
        fun calculate(t: BalanceCalculator.TransportRecord) = BalanceCalculator.ledgers(
            listOf(t), emptyList(), emptyList(), emptyList(), { true })
        assertTrue(calculate(trip).isEmpty())
        val complete = calculate(trip.copy(paid = mapOf(1L to 2000L, 2L to 1500L)))
        assertEquals(200L, complete.getValue(1L).netBalanceCents)
        assertEquals(-200L, complete.getValue(2L).netBalanceCents)
    }
}
