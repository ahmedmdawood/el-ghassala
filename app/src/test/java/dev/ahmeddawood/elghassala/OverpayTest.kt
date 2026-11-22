package dev.ahmeddawood.elghassala

import androidx.room.Room
import dev.ahmeddawood.elghassala.data.db.AppDatabase
import dev.ahmeddawood.elghassala.data.db.PersonEntity
import dev.ahmeddawood.elghassala.data.repo.GhassalaRepository
import dev.ahmeddawood.elghassala.data.repo.TransportInput
import dev.ahmeddawood.elghassala.domain.FinanceEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OverpayTest {

    /** Ahmed's acceptance example: fare 17, Ahmed paid 20, Mohamed 15. Driver owes Ahmed 1, Mohamed owes Ahmed 2. */
    @Test
    fun engine_acceptanceExample() {
        val c = FinanceEngine.computeTransport(1700, mapOf(1L to 2000L, 2L to 1500L))
        assertEquals(mapOf(1L to 100L), c.driverRefunds)
        assertEquals(1, c.edges.size)
        assertEquals(2L, c.edges[0].fromPersonId)
        assertEquals(1L, c.edges[0].toPersonId)
        assertEquals(200L, c.edges[0].amountCents)
    }

    /** Saving a trip with extra paid, then reading everything back and marking the refund collected, must not throw. */
    @Test
    fun saveOverpaidTrip_roundTrip() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = GhassalaRepository(db)
        val a = db.personDao().insert(PersonEntity(name = "Ahmed", archived = false, createdAt = 1))
        val m = db.personDao().insert(PersonEntity(name = "Mohamed", archived = false, createdAt = 1))
        val dayId = repo.saveDay(null, "Day", 1000L)
        val (tid, refunds) = repo.saveTransport(
            null,
            TransportInput(dayId, "القلزم", "شبين", 1700, mapOf(a to 2000L, m to 1500L), 2000L, "", null),
        )
        assertEquals(1, refunds.size)
        assertEquals(100L, refunds[0].amountCents)
        assertFalse(refunds[0].collected)
        assertEquals(1, repo.observeRefunds().first().size)
        assertEquals(1, repo.observeTransports().first().size)
        repo.setRefundCollected(refunds[0].id, true)
        assertTrue(repo.observeRefunds().first()[0].collected)
        repo.saveTransport(tid, TransportInput(dayId, "القلزم", "شبين", 1700, mapOf(a to 2500L, m to 1500L), 2000L, "", null))
        db.close()
    }
}
