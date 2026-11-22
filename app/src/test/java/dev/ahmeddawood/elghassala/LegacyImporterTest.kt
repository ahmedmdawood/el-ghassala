package dev.ahmeddawood.elghassala

import androidx.room.Room
import org.robolectric.RuntimeEnvironment
import dev.ahmeddawood.elghassala.backup.LegacyImporter
import dev.ahmeddawood.elghassala.data.db.AppDatabase
import dev.ahmeddawood.elghassala.data.db.PersonEntity
import dev.ahmeddawood.elghassala.data.db.TransferKind
import dev.ahmeddawood.elghassala.data.repo.GhassalaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LegacyImporterTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: GhassalaRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = GhassalaRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private val sample = """
    {
      "app": "El Ghasala", "version": 1, "exportedAt": 1795327434688,
      "days": [
        {"id": 7, "title": "كليه", "date": "2026-11-17", "isClosed": false, "createdAt": 1794890221003, "participants": ["داود", "هيما"]},
        {"id": 5, "title": "كليه", "date": "2026-11-09", "isClosed": true, "createdAt": 1794199353727, "participants": ["hima", "ahmed"]}
      ],
      "people": [
        {"id": 2, "displayName": "hima", "normalizedName": "hima", "createdAt": 1794075234781, "updatedAt": 1794075234781, "isArchived": false, "usageCount": 0}
      ],
      "savedRoutes": [
        {"id": 3, "title": "السلام → تجمع اول", "origin": "السلام", "destination": "تجمع اول", "defaultFare": 18, "usageCount": 1, "createdAt": 1794075158265, "updatedAt": 1794895698056}
      ],
      "transactions": [
        {"id": 44, "dayId": 7, "type": "TRANSPORT", "timestamp": 1794909968425, "notes": "", "origin": "سلام", "destination": "شبين",
         "farePerPerson": 20, "totalTripCost": 40, "tripParticipants": ["هيما", "داود"], "payerPayments": {"داود": 50},
         "beneficiaryShares": {"هيما": 20, "داود": 20}, "expenseTitle": "", "category": "General", "fromParticipant": "", "toParticipant": "", "transferAmount": 0},
        {"id": 45, "dayId": 7, "type": "TRANSFER", "timestamp": 1794910059765, "notes": "Settlement / تصفية حساب",
         "origin": "", "destination": "", "farePerPerson": 0, "totalTripCost": 0, "tripParticipants": [], "payerPayments": {}, "beneficiaryShares": {},
         "expenseTitle": "", "category": "General", "fromParticipant": "داود", "toParticipant": "هيما", "transferAmount": 3},
        {"id": 46, "dayId": 5, "type": "EXPENSE", "timestamp": 1794199400000, "notes": "", "origin": "", "destination": "",
         "farePerPerson": 0, "totalTripCost": 0, "tripParticipants": [], "payerPayments": {"ahmed": 30}, "beneficiaryShares": {"ahmed": 15, "hima": 15},
         "expenseTitle": "قهوة", "category": "Food", "fromParticipant": "", "toParticipant": "", "transferAmount": 0}
      ],
      "driverRefunds": [
        {"id": 16, "transactionId": 44, "dayId": 7, "participantName": "داود", "amount": 10, "isCollected": true, "collectedAt": 1794909977159, "createdAt": 1794909968498}
      ]
    }
    """.trimIndent()

    @Test
    fun import_twiceDoesNotDuplicate() = runBlocking {
        repo.importLegacy(sample)
        val before = repo.snapshot()
        val second = repo.importLegacy(sample)
        val after = repo.snapshot()
        assertEquals(0, second.daysAdded)
        assertEquals(0, second.routesAdded)
        assertEquals(0, second.transportsAdded)
        assertEquals(0, second.expensesAdded)
        assertEquals(0, second.transfersAdded)
        assertTrue(second.warnings.any { it.startsWith("ALREADY_IMPORTED:") })
        assertEquals(before.days.size, after.days.size)
        assertEquals(before.transports.size, after.transports.size)
        assertEquals(before.refunds.size, after.refunds.size)
        assertEquals(before.transfers.size, after.transfers.size)
    }

    @Test
    fun import_fullFlow() = runBlocking {
        // Pre-existing person must be reused, not duplicated.
        db.personDao().insert(PersonEntity(name = "ahmed"))

        val result = repo.importLegacy(sample)

        assertEquals(2, result.daysAdded)
        assertEquals(1, result.routesAdded)
        assertEquals(1, result.transportsAdded)
        assertEquals(1, result.expensesAdded)
        assertEquals(1, result.transfersAdded)
        assertEquals(1, result.refundsMarkedCollected)
        assertEquals(1, result.peopleReused) // ahmed
        // names: هيما, داود, ahmed(reused), hima -> 3 added
        assertEquals(3, result.peopleAdded)
        assertTrue(result.warnings.isEmpty())

        val snapshot = repo.snapshot()
        assertEquals(4, snapshot.people.size) // ahmed (reused) + داود + هيما + hima
        // refund reconciled: 1000 piasters collected for داود
        val refund = snapshot.refunds.single()
        assertEquals(1000L, refund.amountCents)
        assertTrue(refund.collected)
        assertEquals(1794909977159L, refund.collectedAt)

        // transport participants: داود paid 5000, هيما 0
        val parts = snapshot.transportParticipants.sortedBy { it.paidCents }
        assertEquals(2, parts.size)
        assertEquals(0L, parts[0].paidCents)
        assertEquals(5000L, parts[1].paidCents)

        // transfer is a settlement, 300 piasters
        val transfer = snapshot.transfers.single()
        assertEquals(TransferKind.SETTLEMENT, transfer.kind)
        assertEquals(300L, transfer.amountCents)

        // expense totals in piasters
        val expense = snapshot.expenses.single()
        assertEquals(3000L, expense.totalCents)
        assertEquals("قهوة", expense.title)
    }

    @Test
    fun import_rejectsForeignJson() {
        assertThrows(LegacyImporter.InvalidLegacyBackupException::class.java) {
            runBlocking { repo.importLegacy("""{"app":"elghassala","version":1}""") }
        }
        assertThrows(LegacyImporter.InvalidLegacyBackupException::class.java) {
            runBlocking { repo.importLegacy("""{"app":"El Ghasala","version":2}""") }
        }
        assertThrows(LegacyImporter.InvalidLegacyBackupException::class.java) {
            runBlocking { repo.importLegacy("not json") }
        }
    }

    @Test
    fun import_realExportIfPresent() = runBlocking {
        // Validates against Ahmed's real export when present on this machine;
        // skipped elsewhere. Never committed to the repo.
        val f = java.io.File("/downloads/file-c20fd87a.json")
        org.junit.Assume.assumeTrue(f.exists())
        val result = repo.importLegacy(f.readText())
        assertEquals(6, result.daysAdded)
        assertEquals(4, result.routesAdded)
        assertEquals(35, result.transportsAdded)
        assertEquals(0, result.expensesAdded)
        assertEquals(3, result.transfersAdded)
        assertEquals(14, result.refundsMarkedCollected)
        assertEquals(6, result.peopleAdded) // ahmed, hima, ninja, احمد, داود, هيما
        assertTrue(result.warnings.isEmpty())
    }
}
