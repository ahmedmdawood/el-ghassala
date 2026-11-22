package dev.ahmeddawood.elghassala.domain

/** Pure balance & statistics math over plain data — no Android, no Room. */
object BalanceCalculator {

    data class TransportRecord(
        val id: Long,
        val dayId: Long?,
        val fareCents: Long,
        val paid: Map<Long, Long>,
        val dateMillis: Long,
    )

    data class ExpenseRecord(
        val id: Long,
        val dayId: Long?,
        val totalCents: Long,
        val payers: Map<Long, Long>,
        val shares: Map<Long, Long>,
        val dateMillis: Long,
    )

    data class TransferRecord(
        val id: Long,
        val dayId: Long?,
        val fromPersonId: Long,
        val toPersonId: Long,
        val amountCents: Long,
        val isSettlement: Boolean,
        val dateMillis: Long,
    )

    data class RefundRecord(
        val personId: Long,
        val amountCents: Long,
        val collected: Boolean,
        val collectedAt: Long?,
        val createdAt: Long,
    )

    data class PersonLedger(
        val personId: Long,
        val transportNetCents: Long,
        val expenseNetCents: Long,
        val transfersSentCents: Long,
        val transfersReceivedCents: Long,
        val settlementsSentCents: Long,
        val settlementsReceivedCents: Long,
        val refundsOutstandingCents: Long,
        val refundsCollectedCents: Long,
    ) {
        /** Net balance against other people (driver refunds excluded by design). */
        val netBalanceCents: Long
            get() = transportNetCents + expenseNetCents +
                (transfersReceivedCents - transfersSentCents) +
                (settlementsSentCents - settlementsReceivedCents)
    }

    fun ledgers(
        transports: List<TransportRecord>,
        expenses: List<ExpenseRecord>,
        transfers: List<TransferRecord>,
        refunds: List<RefundRecord>,
        inScope: (dayId: Long?) -> Boolean,
    ): Map<Long, PersonLedger> {
        val transportNet = mutableMapOf<Long, Long>()
        val expenseNet = mutableMapOf<Long, Long>()
        val sent = mutableMapOf<Long, Long>()
        val received = mutableMapOf<Long, Long>()
        val settleSent = mutableMapOf<Long, Long>()
        val settleReceived = mutableMapOf<Long, Long>()

        // Separate Room streams can briefly deliver a transport before its riders.
        // Ignore that incomplete snapshot instead of throwing on the UI collector.
        transports.filter { inScope(it.dayId) && it.paid.isNotEmpty() }.forEach { t ->
            FinanceEngine.computeTransport(t.fareCents, t.paid).participantNet.forEach { (id, v) ->
                transportNet.merge(id, v, Long::plus)
            }
        }
        expenses.filter { inScope(it.dayId) }.forEach { e ->
            FinanceEngine.expenseNet(e.payers, e.shares).forEach { (id, v) -> expenseNet.merge(id, v, Long::plus) }
        }
        transfers.filter { inScope(it.dayId) }.forEach { t ->
            if (t.isSettlement) {
                settleSent.merge(t.fromPersonId, t.amountCents, Long::plus)
                settleReceived.merge(t.toPersonId, t.amountCents, Long::plus)
            } else {
                sent.merge(t.fromPersonId, t.amountCents, Long::plus)
                received.merge(t.toPersonId, t.amountCents, Long::plus)
            }
        }

        val refundsOut = refunds.filter { !it.collected }.groupBy { it.personId }.mapValues { (_, l) -> l.sumOf { it.amountCents } }
        val refundsCol = refunds.filter { it.collected }.groupBy { it.personId }.mapValues { (_, l) -> l.sumOf { it.amountCents } }

        val ids = transportNet.keys + expenseNet.keys + sent.keys + received.keys +
            settleSent.keys + settleReceived.keys + refundsOut.keys + refundsCol.keys
        return ids.associateWith { id ->
            PersonLedger(
                personId = id,
                transportNetCents = transportNet[id] ?: 0,
                expenseNetCents = expenseNet[id] ?: 0,
                transfersSentCents = sent[id] ?: 0,
                transfersReceivedCents = received[id] ?: 0,
                settlementsSentCents = settleSent[id] ?: 0,
                settlementsReceivedCents = settleReceived[id] ?: 0,
                refundsOutstandingCents = refundsOut[id] ?: 0,
                refundsCollectedCents = refundsCol[id] ?: 0,
            )
        }
    }

    fun netBalances(ledgers: Map<Long, PersonLedger>): Map<Long, Long> =
        ledgers.mapValues { (_, l) -> l.netBalanceCents }.filterValues { it != 0L }

    data class PeriodStats(
        val transportCents: Long,
        val cashCents: Long,
        val transferVolumeCents: Long,
        val outstandingRefundsCents: Long,
        val collectedRefundsCents: Long,
        val averageDailyCents: Long,
        val dailyBuckets: List<Pair<Long, Long>>,
        val previousPeriodExpensesCents: Long,
    ) {
        val actualExpensesCents: Long get() = transportCents + cashCents
    }

    fun stats(
        startMillis: Long,
        endMillis: Long,
        transports: List<TransportRecord>,
        expenses: List<ExpenseRecord>,
        transfers: List<TransferRecord>,
        refunds: List<RefundRecord>,
        dayLengthMillis: Long,
    ): PeriodStats {
        fun inPeriod(t: Long) = t in startMillis until endMillis
        val length = endMillis - startMillis
        val prevStart = startMillis - length

        val transportSpend = transports.filter { inPeriod(it.dateMillis) }.sumOf { it.fareCents * it.paid.size }
        val cashSpend = expenses.filter { inPeriod(it.dateMillis) }.sumOf { it.totalCents }
        val transferVolume = transfers.filter { inPeriod(it.dateMillis) && !it.isSettlement }.sumOf { it.amountCents }
        val prevTransport = transports.filter { it.dateMillis in prevStart until startMillis }.sumOf { it.fareCents * it.paid.size }
        val prevCash = expenses.filter { it.dateMillis in prevStart until startMillis }.sumOf { it.totalCents }

        val outstanding = refunds.filter { !it.collected }.sumOf { it.amountCents }
        val collected = refunds.filter { it.collected && it.collectedAt != null && inPeriod(it.collectedAt!!) }.sumOf { it.amountCents }

        val dayCount = maxOf(1L, length / dayLengthMillis)
        val avg = (transportSpend + cashSpend) / dayCount

        val buckets = LinkedHashMap<Long, Long>()
        var d = startMillis
        while (d < endMillis) {
            buckets[d] = 0L
            d += dayLengthMillis
        }
        transports.filter { inPeriod(it.dateMillis) }.forEach { t ->
            val bucket = startMillis + ((t.dateMillis - startMillis) / dayLengthMillis) * dayLengthMillis
            buckets[bucket] = (buckets[bucket] ?: 0) + t.fareCents * t.paid.size
        }
        expenses.filter { inPeriod(it.dateMillis) }.forEach { e ->
            val bucket = startMillis + ((e.dateMillis - startMillis) / dayLengthMillis) * dayLengthMillis
            buckets[bucket] = (buckets[bucket] ?: 0) + e.totalCents
        }

        return PeriodStats(
            transportCents = transportSpend,
            cashCents = cashSpend,
            transferVolumeCents = transferVolume,
            outstandingRefundsCents = outstanding,
            collectedRefundsCents = collected,
            averageDailyCents = avg,
            dailyBuckets = buckets.toList(),
            previousPeriodExpensesCents = prevTransport + prevCash,
        )
    }
}
