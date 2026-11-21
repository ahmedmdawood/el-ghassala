package dev.ahmeddawood.elghassala

import dev.ahmeddawood.elghassala.domain.FinanceEngine
import dev.ahmeddawood.elghassala.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceEngineTest {

    /**
     * Mandatory acceptance test from the spec: fare 10 EGP, Ahmed and Hima ride.
     * Ahmed paid 20, Hima paid 10. Driver collected 30 for a 20 ride:
     * driver owes Ahmed exactly 10 EGP as an outstanding refund, Hima is clean,
     * and nothing is auto-settled.
     */
    @Test
    fun acceptance_driverRefund_ahmed10_himaClean() {
        val result = FinanceEngine.computeTransport(
            fareCents = 1000,
            paid = mapOf(1L to 2000L, 2L to 1000L),
        )
        assertEquals(mapOf(1L to 1000L), result.driverRefunds)
        assertTrue(result.driverOwed.isEmpty())
        // Ahmed's overpay was fully converted to a driver refund: no leftover.
        assertTrue(result.edges.isEmpty())
    }

    @Test
    fun exactPayment_noRefundsNoDebts() {
        val result = FinanceEngine.computeTransport(1000, mapOf(1L to 1000L, 2L to 1000L))
        assertTrue(result.driverRefunds.isEmpty())
        assertTrue(result.participantNet.isEmpty())
        assertTrue(result.edges.isEmpty())
    }

    @Test
    fun onePayerCoversAll_createsParticipantDebt() {
        val result = FinanceEngine.computeTransport(1000, mapOf(1L to 2000L, 2L to 0L))
        assertTrue(result.driverRefunds.isEmpty())
        assertEquals(1, result.edges.size)
        assertEquals(2L, result.edges[0].fromPersonId)
        assertEquals(1L, result.edges[0].toPersonId)
        assertEquals(1000L, result.edges[0].amountCents)
    }

    @Test
    fun underpayment_staysOwedToDriver() {
        val result = FinanceEngine.computeTransport(1000, mapOf(1L to 1000L, 2L to 500L))
        assertEquals(mapOf(2L to 500L), result.driverOwed)
        assertTrue(result.driverRefunds.isEmpty())
    }

    @Test
    fun distribute_isExactToThePiaster() {
        val shares = FinanceEngine.distribute(1000, mapOf(1L to 1L, 2L to 1L, 3L to 1L))
        assertEquals(1000L, shares.values.sum())
    }

    @Test
    fun equalShares_splitEvenly() {
        val shares = FinanceEngine.equalShares(1000, listOf(1L, 2L, 3L, 4L))
        assertEquals(250L, shares[1L])
        assertEquals(1000L, shares.values.sum())
    }

    @Test
    fun suggestSettlements_balancesCancel() {
        val edges = FinanceEngine.suggestSettlements(mapOf(1L to 500L, 2L to -500L))
        assertEquals(1, edges.size)
        assertEquals(2L, edges[0].fromPersonId)
        assertEquals(1L, edges[0].toPersonId)
        assertEquals(500L, edges[0].amountCents)
    }

    @Test
    fun parse_acceptsArabicDigitsAndDecimal() {
        assertEquals(1050L, Money.parse("١٠.٥٠"))
        assertEquals(1050L, Money.parse("10.50"))
        assertEquals(1000L, Money.parse("10"))
        assertEquals(null, Money.parse("abc"))
    }

    @Test
    fun format_roundTripsParse() {
        assertEquals(12345L, Money.parse(Money.format(12345L)))
    }

    @Test
    fun transfersNeverCountAsExpenses() {
        // Settlements only move balances; stats exclude them from spending.
        val stats = dev.ahmeddawood.elghassala.domain.BalanceCalculator.stats(
            startMillis = 0, endMillis = 10_000,
            transports = emptyList(),
            expenses = emptyList(),
            transfers = listOf(
                dev.ahmeddawood.elghassala.domain.BalanceCalculator.TransferRecord(
                    id = 1, dayId = null, fromPersonId = 1, toPersonId = 2,
                    amountCents = 5000, isSettlement = true, dateMillis = 5000,
                )
            ),
            refunds = emptyList(),
            dayLengthMillis = 1000,
        )
        assertEquals(0L, stats.actualExpensesCents)
        assertEquals(0L, stats.transferVolumeCents) // settlements are not transfer volume either
    }
}
