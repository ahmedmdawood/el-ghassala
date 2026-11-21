package dev.ahmeddawood.elghassala

import dev.ahmeddawood.elghassala.backup.BackupManager
import dev.ahmeddawood.elghassala.data.repo.GhassalaRepository.Snapshot
import dev.ahmeddawood.elghassala.data.db.DayEntity
import dev.ahmeddawood.elghassala.data.db.PersonEntity
import dev.ahmeddawood.elghassala.data.db.TransferEntity
import dev.ahmeddawood.elghassala.data.db.TransferKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupTest {

    @Test
    fun exportImport_roundTrips() {
        val snapshot = Snapshot(
            days = listOf(DayEntity(id = 1, title = "رحلة", dateMillis = 1000, isClosed = false, createdAt = 1000)),
            people = listOf(
                PersonEntity(id = 1, name = "أحمد", archived = false, createdAt = 1000),
                PersonEntity(id = 2, name = "Hima", archived = true, createdAt = 1000),
            ),
            routes = emptyList(),
            transports = emptyList(),
            transportParticipants = emptyList(),
            refunds = emptyList(),
            expenses = emptyList(),
            expensePayers = emptyList(),
            expenseBeneficiaries = emptyList(),
            transfers = listOf(
                TransferEntity(
                    id = 7, dayId = 1, fromPersonId = 1, toPersonId = 2, amountCents = 2500,
                    kind = TransferKind.SETTLEMENT, dateMillis = 2000, notes = "settle up",
                )
            ),
        )
        val json = BackupManager.export(snapshot)
        val restored = BackupManager.parse(json)
        assertEquals(snapshot.days, restored.days)
        assertEquals(snapshot.people, restored.people)
        assertEquals(snapshot.transfers, restored.transfers)
    }

    @Test
    fun parse_rejectsWrongApp() {
        assertThrows(BackupManager.InvalidBackupException::class.java) {
            BackupManager.parse("""{"app":"other","version":1}""")
        }
    }

    @Test
    fun parse_rejectsFutureVersion() {
        assertThrows(BackupManager.InvalidBackupException::class.java) {
            BackupManager.parse("""{"app":"elghassala","version":999}""")
        }
    }

    @Test
    fun parse_rejectsGarbage() {
        assertThrows(BackupManager.InvalidBackupException::class.java) {
            BackupManager.parse("not json at all")
        }
    }
}
