package dev.ahmeddawood.elghassala.domain

import kotlin.math.abs

/** A directed debt: [fromPersonId] owes [toPersonId] [amountCents]. */
data class DebtEdge(val fromPersonId: Long, val toPersonId: Long, val amountCents: Long)

/**
 * Result of one shared-transport record.
 *
 * Rules (mandatory): the fare is per participant. Money the driver collected
 * beyond the total fare is a driver refund owed to the overpayers — it never
 * disappears and never silently marks anything settled. Underpayments are
 * covered first by overpayers (participant-to-participant debt); whatever the
 * driver never received stays owed to the driver.
 */
data class TransportComputation(
    val totalFareCents: Long,
    val totalPaidCents: Long,
    val excessCents: Long,
    val participantNet: Map<Long, Long>,
    val driverRefunds: Map<Long, Long>,
    val driverOwed: Map<Long, Long>,
    val edges: List<DebtEdge>,
)

data class PersonBalance(
    val personId: Long,
    val netCents: Long,
)

object FinanceEngine {

    /** Distributes [amount] across (id -> weight) proportionally, exact to the piaster (largest remainder). */
    fun distribute(amount: Long, weights: Map<Long, Long>): Map<Long, Long> {
        if (amount <= 0 || weights.isEmpty()) return emptyMap()
        val positive = weights.filterValues { it > 0 }
        if (positive.isEmpty()) return emptyMap()
        val totalWeight = positive.values.sum()
        if (totalWeight <= 0) return emptyMap()
        val raw = positive.mapValues { (_, w) -> amount * w / totalWeight }
        var remainder = amount - raw.values.sum()
        val order = positive.entries.sortedWith(
            compareByDescending<Map.Entry<Long, Long>> { (amount * it.value) % totalWeight }
                .thenBy { it.key }
        )
        val result = raw.toMutableMap()
        var i = 0
        while (remainder > 0 && order.isNotEmpty()) {
            val key = order[i % order.size].key
            result[key] = (result[key] ?: 0) + 1
            remainder--
            i++
        }
        return result.filterValues { it > 0 }
    }

    /**
     * Core transport computation. [paid] maps every participant to what they
     * actually handed the driver (0 allowed). Everyone in [paid] rode and owes the fare.
     */
    fun computeTransport(fareCents: Long, paid: Map<Long, Long>): TransportComputation {
        require(fareCents >= 0) { "fare must be >= 0" }
        require(paid.isNotEmpty()) { "need at least one participant" }
        val totalFare = fareCents * paid.size
        val collected = paid.values.sum()
        val net = paid.mapValues { (_, p) -> p - fareCents }
        val excess = collected - totalFare

        val over = net.filterValues { it > 0 }
        val under = net.filterValues { it < 0 }.mapValues { (_, v) -> -v }

        val refunds: Map<Long, Long>
        val driverOwed: Map<Long, Long>
        val participantNet = mutableMapOf<Long, Long>()

        if (excess > 0) {
            refunds = distribute(excess, over)
            driverOwed = emptyMap()
            val remainingOver = over.mapValues { (id, o) -> o - (refunds[id] ?: 0) }
            over.forEach { (id, _) -> participantNet[id] = remainingOver[id] ?: 0 }
            under.forEach { (id, u) -> participantNet[id] = -u }
        } else {
            refunds = emptyMap()
            driverOwed = if (excess < 0) distribute(-excess, under) else emptyMap()
            over.forEach { (id, o) -> participantNet[id] = o }
            under.forEach { (id, u) -> participantNet[id] = -(u - (driverOwed[id] ?: 0)) }
        }

        val edges = greedyMatch(
            debtors = participantNet.filterValues { it < 0 }.mapValues { (_, v) -> -v },
            creditors = participantNet.filterValues { it > 0 },
        )

        return TransportComputation(
            totalFareCents = totalFare,
            totalPaidCents = collected,
            excessCents = excess,
            participantNet = participantNet.filterValues { it != 0L },
            driverRefunds = refunds,
            driverOwed = driverOwed,
            edges = edges,
        )
    }

    /** Net position of each person in a cash expense: payers vs beneficiary shares. */
    fun expenseNet(payers: Map<Long, Long>, shares: Map<Long, Long>): Map<Long, Long> {
        val ids = payers.keys + shares.keys
        val net = ids.associateWith { (payers[it] ?: 0) - (shares[it] ?: 0) }
        return net.filterValues { it != 0L }
    }

    /** Splits [total] equally between [ids], exact to the piaster. */
    fun equalShares(total: Long, ids: Collection<Long>): Map<Long, Long> {
        if (ids.isEmpty()) return emptyMap()
        return distribute(total, ids.associateWith { 1L })
    }

    /** Debt simplification: smallest practical set of payments that settles every net balance. */
    fun suggestSettlements(balances: Map<Long, Long>): List<DebtEdge> {
        val debtors = balances.filterValues { it < 0 }.mapValues { (_, v) -> -v }
        val creditors = balances.filterValues { it > 0 }
        return greedyMatch(debtors, creditors)
    }

    /** Greedy largest-first matching between debtors (positive amounts owed) and creditors. */
    fun greedyMatch(debtors: Map<Long, Long>, creditors: Map<Long, Long>): List<DebtEdge> {
        val d = debtors.filterValues { it > 0 }.toMutableMap()
        val c = creditors.filterValues { it > 0 }.toMutableMap()
        val edges = mutableListOf<DebtEdge>()
        while (d.isNotEmpty() && c.isNotEmpty()) {
            val debtor = d.maxByOrNull { it.value }!!.key
            val creditor = c.maxByOrNull { it.value }!!.key
            val amount = minOf(d.getValue(debtor), c.getValue(creditor))
            if (amount <= 0) break
            edges.add(DebtEdge(debtor, creditor, amount))
            d[debtor] = d.getValue(debtor) - amount
            c[creditor] = c.getValue(creditor) - amount
            if (d.getValue(debtor) == 0L) d.remove(debtor)
            if (c.getValue(creditor) == 0L) c.remove(creditor)
        }
        return edges
    }

    fun isSettled(balances: Map<Long, Long>): Boolean = balances.values.all { abs(it) == 0L }
}
