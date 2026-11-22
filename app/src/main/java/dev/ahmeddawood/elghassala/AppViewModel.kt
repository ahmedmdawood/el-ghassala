package dev.ahmeddawood.elghassala

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ahmeddawood.elghassala.backup.BackupManager
import dev.ahmeddawood.elghassala.backup.LegacyImporter
import dev.ahmeddawood.elghassala.data.db.DayEntity
import dev.ahmeddawood.elghassala.data.db.DriverRefundEntity
import dev.ahmeddawood.elghassala.data.db.PersonEntity
import dev.ahmeddawood.elghassala.data.db.RouteEntity
import dev.ahmeddawood.elghassala.data.db.TransferKind
import dev.ahmeddawood.elghassala.data.repo.DuplicateNameException
import dev.ahmeddawood.elghassala.data.repo.ExpenseInput
import dev.ahmeddawood.elghassala.data.repo.GhassalaRepository
import dev.ahmeddawood.elghassala.data.repo.TransportInput
import dev.ahmeddawood.elghassala.domain.BalanceCalculator
import dev.ahmeddawood.elghassala.domain.FinanceEngine
import dev.ahmeddawood.elghassala.export.CsvExporter
import dev.ahmeddawood.elghassala.export.PdfExporter
import dev.ahmeddawood.elghassala.settings.AppLanguage
import dev.ahmeddawood.elghassala.settings.AppTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class Message(val resId: Int) : UiEvent()
    data class RefundPrompt(val refunds: List<DriverRefundEntity>) : UiEvent()
}

/** Combined reactive snapshot of everything the UI needs. */
data class Records(
    val days: List<DayEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val routes: List<RouteEntity> = emptyList(),
    val transports: List<dev.ahmeddawood.elghassala.data.db.TransportEntity> = emptyList(),
    val participants: List<dev.ahmeddawood.elghassala.data.db.TransportParticipantEntity> = emptyList(),
    val refunds: List<DriverRefundEntity> = emptyList(),
    val expenses: List<dev.ahmeddawood.elghassala.data.db.ExpenseEntity> = emptyList(),
    val payers: List<dev.ahmeddawood.elghassala.data.db.ExpensePayerEntity> = emptyList(),
    val beneficiaries: List<dev.ahmeddawood.elghassala.data.db.ExpenseBeneficiaryEntity> = emptyList(),
    val transfers: List<dev.ahmeddawood.elghassala.data.db.TransferEntity> = emptyList(),
) {
    val peopleNames: Map<Long, String> get() = people.associate { it.id to it.name }
    val activePeople: List<PersonEntity> get() = people.filter { !it.archived }

    fun transportRecords(dayFilter: Set<Long>? = null): List<BalanceCalculator.TransportRecord> {
        val parts = participants.groupBy { it.transportId }
        return transports
            .filter { dayFilter == null || it.dayId == null || it.dayId in dayFilter }
            .map { t ->
                BalanceCalculator.TransportRecord(
                    id = t.id, dayId = t.dayId, fareCents = t.fareCents,
                    paid = parts[t.id].orEmpty().associate { it.personId to it.paidCents },
                    dateMillis = t.dateMillis,
                )
            }
    }

    fun expenseRecords(dayFilter: Set<Long>? = null): List<BalanceCalculator.ExpenseRecord> {
        val pay = payers.groupBy { it.expenseId }
        val ben = beneficiaries.groupBy { it.expenseId }
        return expenses
            .filter { dayFilter == null || it.dayId == null || it.dayId in dayFilter }
            .map { e ->
                BalanceCalculator.ExpenseRecord(
                    id = e.id, dayId = e.dayId, totalCents = e.totalCents,
                    payers = pay[e.id].orEmpty().associate { it.personId to it.paidCents },
                    shares = ben[e.id].orEmpty().associate { it.personId to it.shareCents },
                    dateMillis = e.dateMillis,
                )
            }
    }

    fun transferRecords(dayFilter: Set<Long>? = null): List<BalanceCalculator.TransferRecord> =
        transfers
            .filter { dayFilter == null || it.dayId == null || it.dayId in dayFilter }
            .map { t ->
                BalanceCalculator.TransferRecord(
                    id = t.id, dayId = t.dayId, fromPersonId = t.fromPersonId, toPersonId = t.toPersonId,
                    amountCents = t.amountCents, isSettlement = t.kind == TransferKind.SETTLEMENT,
                    dateMillis = t.dateMillis,
                )
            }

    fun refundRecords(): List<BalanceCalculator.RefundRecord> =
        refunds.map {
            BalanceCalculator.RefundRecord(
                personId = it.personId, amountCents = it.amountCents,
                collected = it.collected, collectedAt = it.collectedAt, createdAt = it.createdAt,
            )
        }

    /** Per-transport grand total (fare x riders). */
    val transportTotals: Map<Long, Long>
        get() {
            val counts = participants.groupBy { it.transportId }.mapValues { it.value.size }
            return transports.associate { it.id to it.fareCents * (counts[it.id] ?: 0) }
        }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val application = app as ElGhassalaApp
    private val repo: GhassalaRepository = application.repository
    private val settings = application.settings

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events

    val includeClosedDays = MutableStateFlow(false)
    val searchQuery = MutableStateFlow("")

    val language: StateFlow<AppLanguage> = settings.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.SYSTEM)
    val theme: StateFlow<AppTheme> = settings.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.SYSTEM)

    val records: StateFlow<Records> = combine(
        combine(repo.observeDays(), repo.observePeople(), repo.observeRoutes()) { d, p, r -> Triple(d, p, r) },
        combine(repo.observeTransports(), repo.observeAllParticipants(), repo.observeRefunds()) { t, p, r -> Triple(t, p, r) },
        combine(repo.observeExpenses(), repo.observeAllPayers(), repo.observeAllBeneficiaries()) { e, p, b -> Triple(e, p, b) },
        repo.observeTransfers(),
    ) { a, b, c, transfers ->
        Records(
            days = a.first, people = a.second, routes = a.third,
            transports = b.first, participants = b.second, refunds = b.third,
            expenses = c.first, payers = c.second, beneficiaries = c.third,
            transfers = transfers,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Records())

    /** Ledgers honoring the closed-day settlement toggle. */
    val ledgers: StateFlow<Map<Long, BalanceCalculator.PersonLedger>> =
        combine(records, includeClosedDays) { rec, includeClosed ->
            val scope = dayScope(rec, includeClosed)
            BalanceCalculator.ledgers(
                transports = rec.transportRecords(),
                expenses = rec.expenseRecords(),
                transfers = rec.transferRecords(),
                refunds = rec.refundRecords(),
                inScope = scope,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val netBalances: StateFlow<Map<Long, Long>> = ledgers
        .map { BalanceCalculator.netBalances(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val suggestedSettlements: StateFlow<List<dev.ahmeddawood.elghassala.domain.DebtEdge>> = netBalances
        .map { FinanceEngine.suggestSettlements(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun dayScope(rec: Records, includeClosed: Boolean): (Long?) -> Boolean {
        if (includeClosed) return { true }
        val closedIds = rec.days.filter { it.isClosed }.map { it.id }.toSet()
        return { dayId -> dayId == null || dayId !in closedIds }
    }

    fun ledgerForDay(rec: Records, dayId: Long): Map<Long, BalanceCalculator.PersonLedger> =
        BalanceCalculator.ledgers(
            transports = rec.transportRecords().filter { it.dayId == dayId },
            expenses = rec.expenseRecords().filter { it.dayId == dayId },
            transfers = rec.transferRecords().filter { it.dayId == dayId },
            refunds = rec.refunds.filter { r ->
                rec.transports.firstOrNull { it.id == r.transportId }?.dayId == dayId
            }.map {
                BalanceCalculator.RefundRecord(it.personId, it.amountCents, it.collected, it.collectedAt, it.createdAt)
            },
            inScope = { true },
        )

    // ---- Settings ----

    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { settings.setLanguage(lang) }
    fun setTheme(value: AppTheme) = viewModelScope.launch { settings.setTheme(value) }

    // ---- Days ----

    fun saveDay(id: Long?, title: String, dateMillis: Long) = viewModelScope.launch {
        runCatching { repo.saveDay(id, title, dateMillis) }
            .onSuccess { _events.emit(UiEvent.Message(R.string.saved_successfully)) }
            .onFailure {
                _events.emit(UiEvent.Message(if (it is DuplicateNameException) R.string.duplicate_day_title else R.string.error_required))
            }
    }

    fun setDayClosed(dayId: Long, closed: Boolean) = viewModelScope.launch {
        repo.setDayClosed(dayId, closed)
        _events.emit(UiEvent.Message(if (closed) R.string.day_closed else R.string.day_open))
    }

    fun deleteDay(dayId: Long) = viewModelScope.launch {
        repo.deleteDay(dayId)
        _events.emit(UiEvent.Message(R.string.deleted_successfully))
    }

    suspend fun getDay(id: Long) = repo.getDay(id)

    // ---- People ----

    fun savePerson(id: Long?, name: String) = viewModelScope.launch {
        runCatching { repo.savePerson(id, name) }
            .onSuccess { _events.emit(UiEvent.Message(R.string.saved_successfully)) }
            .onFailure {
                _events.emit(UiEvent.Message(if (it is DuplicateNameException) R.string.duplicate_person else R.string.error_required))
            }
    }

    fun setPersonArchived(personId: Long, archived: Boolean) = viewModelScope.launch {
        repo.setPersonArchived(personId, archived)
        _events.emit(UiEvent.Message(R.string.saved_successfully))
    }

    // ---- Routes ----

    fun saveRoute(id: Long?, name: String, origin: String, destination: String, fareCents: Long) = viewModelScope.launch {
        runCatching { repo.saveRoute(id, name, origin, destination, fareCents) }
            .onSuccess { _events.emit(UiEvent.Message(R.string.saved_successfully)) }
            .onFailure {
                _events.emit(UiEvent.Message(if (it is DuplicateNameException) R.string.duplicate_route else R.string.error_required))
            }
    }

    fun deleteRoute(id: Long) = viewModelScope.launch {
        repo.deleteRoute(id)
        _events.emit(UiEvent.Message(R.string.deleted_successfully))
    }

    // ---- Transport ----

    fun saveTransport(id: Long?, input: TransportInput, markRefundsCollected: Boolean? = null) = viewModelScope.launch {
        runCatching { repo.saveTransport(id, input) }
            .onSuccess { (_, refunds) ->
                if (markRefundsCollected != null) {
                    refunds.forEach { repo.setRefundCollected(it.id, markRefundsCollected) }
                }
                if (refunds.isNotEmpty() && markRefundsCollected == null) {
                    _events.emit(UiEvent.RefundPrompt(refunds))
                } else {
                    _events.emit(UiEvent.Message(R.string.transport_saved))
                }
            }
            .onFailure { _events.emit(UiEvent.Message(R.string.error_required)) }
    }

    fun deleteTransport(id: Long) = viewModelScope.launch {
        repo.deleteTransport(id)
        _events.emit(UiEvent.Message(R.string.deleted_successfully))
    }

    suspend fun getTransport(id: Long) = repo.getTransport(id)
    suspend fun transportParticipants(id: Long) = repo.transportParticipants(id)
    suspend fun refundsForTransport(id: Long) = repo.refundsForTransport(id)

    fun markRouteUsed(id: Long) = viewModelScope.launch { repo.markRouteUsed(id) }

    // ---- Expenses ----

    fun saveExpense(id: Long?, input: ExpenseInput) = viewModelScope.launch {
        runCatching { repo.saveExpense(id, input) }
            .onSuccess { _events.emit(UiEvent.Message(R.string.saved_successfully)) }
            .onFailure {
                _events.emit(
                    UiEvent.Message(
                        if (it.message?.contains("add up") == true) R.string.shares_must_equal_total else R.string.error_required
                    )
                )
            }
    }

    fun deleteExpense(id: Long) = viewModelScope.launch {
        repo.deleteExpense(id)
        _events.emit(UiEvent.Message(R.string.deleted_successfully))
    }

    suspend fun getExpense(id: Long) = repo.getExpense(id)
    suspend fun expensePayers(id: Long) = repo.expensePayers(id)
    suspend fun expenseBeneficiaries(id: Long) = repo.expenseBeneficiaries(id)

    // ---- Transfers ----

    fun saveTransfer(
        id: Long?, dayId: Long?, from: Long, to: Long, amountCents: Long,
        kind: TransferKind, dateMillis: Long, notes: String,
    ) = viewModelScope.launch {
        runCatching { repo.saveTransfer(id, dayId, from, to, amountCents, kind, dateMillis, notes) }
            .onSuccess {
                _events.emit(UiEvent.Message(if (kind == TransferKind.SETTLEMENT) R.string.settlement_recorded else R.string.saved_successfully))
            }
            .onFailure { _events.emit(UiEvent.Message(R.string.error_required)) }
    }

    fun deleteTransfer(id: Long) = viewModelScope.launch {
        repo.deleteTransfer(id)
        _events.emit(UiEvent.Message(R.string.deleted_successfully))
    }

    // ---- Refunds ----

    fun setRefundCollected(refundId: Long, collected: Boolean) = viewModelScope.launch {
        repo.setRefundCollected(refundId, collected)
        _events.emit(UiEvent.Message(if (collected) R.string.refund_marked_collected else R.string.refund_kept_outstanding))
    }

    // ---- Backup / export ----

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        runCatching {
            val json = BackupManager.export(repo.snapshot())
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                ?: error("no stream")
        }.onSuccess { _events.emit(UiEvent.Message(R.string.backup_success)) }
            .onFailure { _events.emit(UiEvent.Message(R.string.restore_failed)) }
    }

    val legacyImportResult = MutableStateFlow<LegacyImporter.Result?>(null)
    fun clearLegacyImportResult() { legacyImportResult.value = null }

    fun importLegacyBackup(uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("no stream")
            repo.importLegacy(text)
        }.onSuccess { legacyImportResult.value = it }
            .onFailure { _events.emit(UiEvent.Message(R.string.legacy_import_failed)) }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("no stream")
            repo.replaceAll(BackupManager.parse(text))
        }.onSuccess { _events.emit(UiEvent.Message(R.string.restore_success)) }
            .onFailure { _events.emit(UiEvent.Message(R.string.restore_failed)) }
    }

    fun exportCsv(uri: Uri) = viewModelScope.launch {
        runCatching {
            val snap = repo.snapshot()
            val parts = snap.transportParticipants.groupBy { it.transportId }
                .mapValues { (tid, list) ->
                    val names = snap.people.associate { it.id to it.name }
                    list.map { (names[it.personId] ?: "#${it.personId}") to it.paidCents }
                }
            val csv = CsvExporter.export(
                transports = snap.transports,
                transportParticipants = parts,
                expenses = snap.expenses,
                transfers = snap.transfers,
                peopleNames = snap.people.associate { it.id to it.name },
            )
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                ?: error("no stream")
        }.onSuccess { _events.emit(UiEvent.Message(R.string.export_ready)) }
            .onFailure { _events.emit(UiEvent.Message(R.string.restore_failed)) }
    }

    fun exportPdf(uri: Uri, title: String, periodLabel: String, start: Long, end: Long) = viewModelScope.launch {
        runCatching {
            val rec = records.value
            val ledgers = BalanceCalculator.ledgers(
                transports = rec.transportRecords().filter { it.dateMillis in start until end },
                expenses = rec.expenseRecords().filter { it.dateMillis in start until end },
                transfers = rec.transferRecords().filter { it.dateMillis in start until end },
                refunds = rec.refundRecords(),
                inScope = { true },
            )
            val totals = mutableMapOf<Long, Long>()
            rec.transportRecords().filter { it.dateMillis in start until end }.forEach { t ->
                t.paid.keys.forEach { pid -> totals[pid] = (totals[pid] ?: 0) + t.fareCents }
            }
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                PdfExporter(getApplication()).writeReport(out, title, periodLabel, ledgers.values.toList(), rec.peopleNames, totals)
            } ?: error("no stream")
        }.onSuccess { _events.emit(UiEvent.Message(R.string.export_ready)) }
            .onFailure { _events.emit(UiEvent.Message(R.string.restore_failed)) }
    }
}
