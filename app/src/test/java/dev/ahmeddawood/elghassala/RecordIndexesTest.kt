package dev.ahmeddawood.elghassala

import dev.ahmeddawood.elghassala.data.db.*
import org.junit.Assert.*
import org.junit.Test

class RecordIndexesTest {
    @Test fun tripIndexesAndDayTotalsMatchRiders() {
        val trip = TransportEntity(id = 9, dayId = 2, origin = "A", destination = "B", fareCents = 1700, dateMillis = 0, notes = "", routeId = null)
        val records = Records(transports = listOf(trip), participants = listOf(
            TransportParticipantEntity(transportId = 9, personId = 1, paidCents = 2000),
            TransportParticipantEntity(transportId = 9, personId = 2, paidCents = 1500),
        ))
        assertEquals(2, records.participantsByTransport.getValue(9).size)
        assertEquals(3400L, records.transportTotals[9])
        assertEquals(3400L, records.daySpendTotals[2])
        assertSame(trip, records.transportsById[9])
        assertEquals(1, records.transportsByDay.getValue(2).size)
        assertSame(records.participantsByTransport, records.participantsByTransport)
        val closed = records.copy(days = listOf(DayEntity(id = 2, title = "Closed", dateMillis = 0, isClosed = true, createdAt = 0)))
        assertEquals(200L, closed.allReceivableEdges().single().amountCents)
        val received = closed.copy(transfers = listOf(TransferEntity(id = 1, dayId = null,
            fromPersonId = 2, toPersonId = 1, amountCents = 200, kind = TransferKind.SETTLEMENT,
            dateMillis = 1, notes = "")))
        assertTrue(received.allReceivableEdges().isEmpty())
    }
}
