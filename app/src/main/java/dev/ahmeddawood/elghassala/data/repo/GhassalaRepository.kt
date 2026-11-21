package dev.ahmeddawood.elghassala.data.repo

import androidx.room.withTransaction
import dev.ahmeddawood.elghassala.data.db.AppDatabase
import dev.ahmeddawood.elghassala.data.db.DayEntity
import dev.ahmeddawood.elghassala.data.db.DriverRefundEntity
import dev.ahmeddawood.elghassala.data.db.ExpenseBeneficiaryEntity
import dev.ahmeddawood.elghassala.data.db.ExpenseEntity
import dev.ahmeddawood.elghassala.data.db.ExpensePayerEntity
import dev.ahmeddawood.elghassala.data.db.PersonEntity
import dev.ahmeddawood.elghassala.data.db.RouteEntity
import dev.ahmeddawood.elghassala.data.db.TransferEntity
import dev.ahmeddawood.elghassala.data.db.TransferKind
import dev.ahmeddawood.elghassala.data.db.TransportEntity
import dev.ahmeddawood.elghassala.data.db.TransportParticipantEntity
import dev.ahmeddawood.elghassala.domain.FinanceEngine

class DuplicateNameException : Exception()

data class TransportInput(
    val dayId: Long?,
    val origin: String,
    val destination: String,
    val fareCents: Long,
    val paid: Map<Long, Long>,
    val dateMillis: Long,
    val notes: String,
    val routeId: Long?,
)

data class ExpenseInput(
    val dayId: Long?,
    val title: String,
    val totalCents: Long,
    val payers: Map<Long, Long>,
    val shares: Map<Long, Long>,
    val dateMillis: Long,
    val notes: String,
)

class GhassalaRepository(private val db: AppDatabase) {

    private val days = db.dayDao()
    private val people = db.personDao()
    private val routes = db.routeDao()
    private val transports = db.transportDao()
    private val refunds = db.refundDao()
    private val expenses = db.expenseDao()
    private val transfers = db.transferDao()

    fun observeDays() = days.observeAll()
    fun observePeople() = people.observeAll()
    fun observeActivePeople() = people.observeActive()
    fun observeRoutes() = routes.observeAll()
    fun observeTransports() = transports.observeAll()
    fun observeExpenses() = expenses.observeAll()
    fun observeTransfers() = transfers.observeAll()
    fun observeRefunds() = refunds.observeAll()
    fun observeAllParticipants() = transports.observeAllParticipants()
    fun observeAllPayers() = expenses.observeAllPayers()
    fun observeAllBeneficiaries() = expenses.observeAllBeneficiaries()
    fun observeDay(id: Long) = days.observeById(id)

    // ---- Days ----

    suspend fun saveDay(id: Long?, title: String, dateMillis: Long): Long {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "title required" }
        if (days.countWithTitle(trimmed, id ?: 0) > 0) throw DuplicateNameException()
        return if (id == null) {
            days.insert(DayEntity(title = trimmed, dateMillis = dateMillis))
        } else {
            val existing = days.getById(id) ?: return id
            days.update(existing.copy(title = trimmed, dateMillis = dateMillis))
            id
        }
    }

    suspend fun setDayClosed(dayId: Long, closed: Boolean) {
        days.getById(dayId)?.let { days.update(it.copy(isClosed = closed)) }
    }

    suspend fun deleteDay(dayId: Long) = db.withTransaction {
        transports.all().filter { it.dayId == dayId }.forEach { t ->
            refunds.deleteForTransport(t.id)
            transports.deleteParticipants(t.id)
            transports.deleteById(t.id)
        }
        expenses.all().filter { it.dayId == dayId }.forEach { e ->
            expenses.deletePayers(e.id)
            expenses.deleteBeneficiaries(e.id)
            expenses.deleteById(e.id)
        }
        transfers.all().filter { it.dayId == dayId }.forEach { transfers.deleteById(it.id) }
        days.deleteById(dayId)
    }

    suspend fun getDay(id: Long) = days.getById(id)

    // ---- People ----

    suspend fun savePerson(id: Long?, name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "name required" }
        if (people.countWithName(trimmed, id ?: 0) > 0) throw DuplicateNameException()
        return if (id == null) {
            people.insert(PersonEntity(name = trimmed))
        } else {
            val existing = people.getById(id) ?: return id
            people.update(existing.copy(name = trimmed))
            id
        }
    }

    suspend fun setPersonArchived(personId: Long, archived: Boolean) {
        people.getById(personId)?.let { people.update(it.copy(archived = archived)) }
    }

    suspend fun getPerson(id: Long) = people.getById(id)

    // ---- Routes ----

    suspend fun saveRoute(
        id: Long?,
        name: String,
        origin: String,
        destination: String,
        defaultFareCents: Long,
    ): Long {
        val n = name.trim()
        val o = origin.trim()
        val d = destination.trim()
        require(n.isNotEmpty() && o.isNotEmpty() && d.isNotEmpty()) { "route fields required" }
        if (routes.countSimilar(o, d, id ?: 0) > 0) throw DuplicateNameException()
        return if (id == null) {
            routes.insert(RouteEntity(name = n, origin = o, destination = d, defaultFareCents = defaultFareCents))
        } else {
            val existing = routes.getById(id) ?: return id
            routes.update(
                existing.copy(name = n, origin = o, destination = d, defaultFareCents = defaultFareCents, updatedAt = System.currentTimeMillis())
            )
            id
        }
    }

    suspend fun deleteRoute(id: Long) = routes.deleteById(id)

    suspend fun markRouteUsed(id: Long) {
        routes.getById(id)?.let {
            routes.update(it.copy(usageCount = it.usageCount + 1, lastUsedAt = System.currentTimeMillis()))
        }
    }

    // ---- Transport ----

    /** Saves a transport and creates its driver refunds, always outstanding at first. */
    suspend fun saveTransport(id: Long?, input: TransportInput): Pair<Long, List<DriverRefundEntity>> = db.withTransaction {
        val computation = FinanceEngine.computeTransport(input.fareCents, input.paid)
        val transportId = if (id == null) {
            transports.insert(
                TransportEntity(
                    dayId = input.dayId,
                    origin = input.origin.trim(),
                    destination = input.destination.trim(),
                    fareCents = input.fareCents,
                    dateMillis = input.dateMillis,
                    notes = input.notes.trim(),
                    routeId = input.routeId,
                )
            )
        } else {
            transports.update(
                TransportEntity(
                    id = id,
                    dayId = input.dayId,
                    origin = input.origin.trim(),
                    destination = input.destination.trim(),
                    fareCents = input.fareCents,
                    dateMillis = input.dateMillis,
                    notes = input.notes.trim(),
                    routeId = input.routeId,
                )
            )
            refunds.deleteForTransport(id)
            transports.deleteParticipants(id)
            id
        }
        transports.insertParticipants(
            input.paid.map { (personId, paidCents) ->
                TransportParticipantEntity(transportId = transportId, personId = personId, paidCents = paidCents)
            }
        )
        val newRefunds = computation.driverRefunds.map { (personId, amount) ->
            DriverRefundEntity(transportId = transportId, personId = personId, amountCents = amount, collected = false)
        }
        if (newRefunds.isNotEmpty()) refunds.insertAll(newRefunds)
        input.routeId?.let { markRouteUsed(it) }
        transportId to refunds.forTransport(transportId)
    }

    suspend fun deleteTransport(id: Long) = db.withTransaction {
        refunds.deleteForTransport(id)
        transports.deleteParticipants(id)
        transports.deleteById(id)
    }

    suspend fun transportParticipants(id: Long) = transports.participantsFor(id)

    suspend fun getTransport(id: Long) = transports.getById(id)

    suspend fun refundsForTransport(id: Long) = refunds.forTransport(id)

    // ---- Expenses ----

    suspend fun saveExpense(id: Long?, input: ExpenseInput): Long = db.withTransaction {
        require(input.payers.values.sum() == input.totalCents) { "payers must add up to total" }
        require(input.shares.values.sum() == input.totalCents) { "shares must add up to total" }
        val expenseId = if (id == null) {
            expenses.insert(
                ExpenseEntity(
                    dayId = input.dayId,
                    title = input.title.trim(),
                    totalCents = input.totalCents,
                    dateMillis = input.dateMillis,
                    notes = input.notes.trim(),
                )
            )
        } else {
            expenses.update(
                ExpenseEntity(
                    id = id,
                    dayId = input.dayId,
                    title = input.title.trim(),
                    totalCents = input.totalCents,
                    dateMillis = input.dateMillis,
                    notes = input.notes.trim(),
                )
            )
            expenses.deletePayers(id)
            expenses.deleteBeneficiaries(id)
            id
        }
        expenses.insertPayers(input.payers.map { (pid, c) -> ExpensePayerEntity(expenseId = expenseId, personId = pid, paidCents = c) })
        expenses.insertBeneficiaries(input.shares.map { (pid, c) -> ExpenseBeneficiaryEntity(expenseId = expenseId, personId = pid, shareCents = c) })
        expenseId
    }

    suspend fun deleteExpense(id: Long) = db.withTransaction {
        expenses.deletePayers(id)
        expenses.deleteBeneficiaries(id)
        expenses.deleteById(id)
    }

    suspend fun getExpense(id: Long) = expenses.getById(id)

    suspend fun expensePayers(id: Long) = expenses.payersFor(id)
    suspend fun expenseBeneficiaries(id: Long) = expenses.beneficiariesFor(id)

    // ---- Transfers & settlements ----

    suspend fun saveTransfer(
        id: Long?,
        dayId: Long?,
        fromPersonId: Long,
        toPersonId: Long,
        amountCents: Long,
        kind: TransferKind,
        dateMillis: Long,
        notes: String,
    ): Long {
        require(fromPersonId != toPersonId) { "sender and receiver must differ" }
        require(amountCents > 0) { "amount must be positive" }
        return if (id == null) {
            transfers.insert(
                TransferEntity(
                    dayId = dayId,
                    fromPersonId = fromPersonId,
                    toPersonId = toPersonId,
                    amountCents = amountCents,
                    kind = kind,
                    dateMillis = dateMillis,
                    notes = notes.trim(),
                )
            )
        } else {
            transfers.update(
                TransferEntity(
                    id = id, dayId = dayId, fromPersonId = fromPersonId, toPersonId = toPersonId,
                    amountCents = amountCents, kind = kind, dateMillis = dateMillis, notes = notes.trim(),
                )
            )
            id
        }
    }

    suspend fun deleteTransfer(id: Long) = transfers.deleteById(id)

    // ---- Refunds ----

    suspend fun setRefundCollected(refundId: Long, collected: Boolean) {
        refunds.getById(refundId)?.let {
            refunds.update(it.copy(collected = collected, collectedAt = if (collected) System.currentTimeMillis() else null))
        }
    }

    // ---- Snapshot (balances, stats, backup) ----

    data class Snapshot(
        val days: List<DayEntity>,
        val people: List<PersonEntity>,
        val routes: List<RouteEntity>,
        val transports: List<TransportEntity>,
        val transportParticipants: List<TransportParticipantEntity>,
        val refunds: List<DriverRefundEntity>,
        val expenses: List<ExpenseEntity>,
        val expensePayers: List<ExpensePayerEntity>,
        val expenseBeneficiaries: List<ExpenseBeneficiaryEntity>,
        val transfers: List<TransferEntity>,
    )

    suspend fun snapshot(): Snapshot = Snapshot(
        days = days.all(),
        people = people.all(),
        routes = routes.all(),
        transports = transports.all(),
        transportParticipants = transports.allParticipants(),
        refunds = refunds.all(),
        expenses = expenses.all(),
        expensePayers = expenses.allPayers(),
        expenseBeneficiaries = expenses.allBeneficiaries(),
        transfers = transfers.all(),
    )

    suspend fun replaceAll(data: Snapshot) = db.withTransaction {
        db.clearAllTables()
        data.days.forEach { days.insert(it) }
        data.people.forEach { people.insert(it) }
        data.routes.forEach { routes.insert(it) }
        data.transports.forEach { transports.insert(it) }
        if (data.transportParticipants.isNotEmpty()) transports.insertParticipants(data.transportParticipants)
        if (data.refunds.isNotEmpty()) refunds.insertAll(data.refunds)
        data.expenses.forEach { expenses.insert(it) }
        if (data.expensePayers.isNotEmpty()) expenses.insertPayers(data.expensePayers)
        if (data.expenseBeneficiaries.isNotEmpty()) expenses.insertBeneficiaries(data.expenseBeneficiaries)
        data.transfers.forEach { transfers.insert(it) }
    }
}
