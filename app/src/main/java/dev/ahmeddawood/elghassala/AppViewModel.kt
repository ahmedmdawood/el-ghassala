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
import dev.ahmeddawood.elghassala.ui.components.DayCelebration

sealed class UiEvent {
    data class Message(val resId: Int) : UiEvent()
    data class Text(val text: String) : UiEvent()
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

    /** Every past trip as a route + riders list, for route stars (نجم المشوار). */
    fun rides(): List<dev.ahmeddawood.elghassala.domain.RouteStars.Ride> {
        val parts = participants.groupBy { it.transportId }
        return transports.map { t ->
            dev.ahmeddawood.elghassala.domain.RouteStars.Ride(t.origin, t.destination, parts[t.id].orEmpty().map { it.personId })
        }
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

    /** True while the language change runs behind a washing-machine veil (fades out after the screen rebuilds). */
    val langSwitching = MutableStateFlow(false)
    fun setLanguage(lang: AppLanguage) = viewModelScope.launch {
        if (lang == language.value) return@launch
        langSwitching.value = true
        kotlinx.coroutines.delay(420)
        settings.setLanguage(lang)
        kotlinx.coroutines.delay(1000)
        langSwitching.value = false
    }

    val palette: StateFlow<dev.ahmeddawood.elghassala.ui.theme.AppPalette> = settings.palette
        .stateIn(viewModelScope, SharingStarted.Eagerly, dev.ahmeddawood.elghassala.ui.theme.AppPalette.TEAL)
    fun setPalette(p: dev.ahmeddawood.elghassala.ui.theme.AppPalette) = viewModelScope.launch { settings.setPalette(p) }
    fun setTheme(value: AppTheme) = viewModelScope.launch { settings.setTheme(value) }

    // ---- Avatars (stored outside the database) ----

    private val avatarStore = application.avatarStore
    val avatars: StateFlow<Map<Long, dev.ahmeddawood.elghassala.settings.Avatar>> = avatarStore.avatars
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    fun avatarPhotoFile(a: dev.ahmeddawood.elghassala.settings.Avatar) = avatarStore.photoFile(a)
    fun setAvatarEmoji(personId: Long, emoji: String) = viewModelScope.launch { avatarStore.setEmoji(personId, emoji) }
    fun clearAvatar(personId: Long) = viewModelScope.launch { avatarStore.clear(personId) }
    fun setAvatarPhoto(personId: Long, uri: Uri) = viewModelScope.launch {
        if (!avatarStore.setPhoto(personId, uri)) _events.emit(UiEvent.Message(R.string.avatar_failed))
    }

    // ---- Privacy mode (وضع الستر) ----

    val privacyMode: StateFlow<Boolean> = settings.privacyMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            privacyMode.collect { dev.ahmeddawood.elghassala.domain.Money.hidden = it }
        }
    }

    fun setPrivacyMode(value: Boolean) {
        dev.ahmeddawood.elghassala.domain.Money.hidden = value
        viewModelScope.launch { settings.setPrivacyMode(value) }
    }

    fun togglePrivacy() = setPrivacyMode(!privacyMode.value)

    // ---- Playful feedback ----

    private val jokes = application.jokes

    val funEnabled: StateFlow<Boolean> = settings.funEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val funFrequency: StateFlow<String> = settings.funFrequency
        .stateIn(viewModelScope, SharingStarted.Eagerly, "balanced")

    fun setFunEnabled(value: Boolean) = viewModelScope.launch { settings.setFunEnabled(value) }
    fun setFunFrequency(value: String) = viewModelScope.launch { settings.setFunFrequency(value) }

    /** Emits a joke from [bucket] (subject to the user's toggle and frequency) or the plain message. */
    private suspend fun feedback(bucket: String, fallbackRes: Int, always: Boolean = false) {
        if (funEnabled.value) {
            val chance = when (funFrequency.value) { "often" -> 1.0; "rare" -> 0.2; else -> 0.5 }
            if (always || Math.random() < chance) {
                jokes.pick(bucket)?.let { _events.emit(UiEvent.Text(it)); return }
            }
        }
        _events.emit(UiEvent.Message(fallbackRes))
    }

    // ---- Day lifecycle ----

    val openDay: StateFlow<DayEntity?> = records
        .map { r -> r.days.filter { !it.isClosed }.maxWithOrNull(compareBy({ it.dateMillis }, { it.id })) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val celebration = MutableStateFlow<DayCelebration?>(null)
    /** Receipt-style summary, shown after the celebration. */
    val receipt = MutableStateFlow<dev.ahmeddawood.elghassala.ui.components.DayReceipt?>(null)
    private var pendingReceipt: dev.ahmeddawood.elghassala.ui.components.DayReceipt? = null
    fun dismissCelebration() {
        celebration.value = null
        pendingReceipt?.let { receipt.value = it; pendingReceipt = null }
    }
    fun dismissReceipt() { receipt.value = null }

    /** True while an import or restore is running: the washing machine spins. */
    val busy = MutableStateFlow(false)

    fun pickJoke(bucket: String): String? = jokes.pick(bucket)

    fun startDay() = viewModelScope.launch {
        if (repo.currentOpenDay() != null) return@launch
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        val base = java.text.SimpleDateFormat("EEE d MMM", java.util.Locale.getDefault()).format(cal.time)
        var title = base
        var n = 2
        while (true) {
            try { repo.saveDay(null, title, cal.timeInMillis); break }
            catch (e: DuplicateNameException) { title = "$base ($n)"; n++ }
        }
        feedback("day_start", R.string.day_started, always = true)
    }

    fun endDay() = viewModelScope.launch {
        val day = repo.currentOpenDay() ?: return@launch
        val rec = records.value
        val dayTransports = rec.transports.filter { it.dayId == day.id }
        val transportTotal = dayTransports.sumOf { t ->
            t.fareCents * rec.participants.count { it.transportId == t.id }.coerceAtLeast(1)
        }
        val expenseTotal = rec.expenses.filter { it.dayId == day.id }.sumOf { it.totalCents }
        repo.setDayClosed(day.id, true)
        val message = (if (funEnabled.value) jokes.pick("day_close") else null)
            ?: getApplication<Application>().getString(R.string.day_celebration_title)
        celebration.value = DayCelebration(day.title, dayTransports.size, transportTotal + expenseTotal, message)

        // Receipt data: every trip and expense of the day, who owes whom, refunds still out.
        val names = rec.peopleNames
        val ledger = ledgerForDay(rec, day.id)
        val tripLines = dayTransports.sortedBy { it.dateMillis }.map { t ->
            val riders = rec.participants.count { it.transportId == t.id }.coerceAtLeast(1)
            dev.ahmeddawood.elghassala.ui.components.ReceiptLine(
                "${t.origin}→${t.destination}", "x$riders", t.fareCents * riders,
            )
        }
        val expenseLines = rec.expenses.filter { it.dayId == day.id }.sortedBy { it.dateMillis }.map {
            dev.ahmeddawood.elghassala.ui.components.ReceiptLine(it.title, "", it.totalCents)
        }
        val dayTransportIds = dayTransports.map { it.id }.toSet()
        pendingReceipt = dev.ahmeddawood.elghassala.ui.components.DayReceipt(
            dayTitle = day.title,
            dateText = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(day.dateMillis)),
            tripLines = tripLines,
            expenseLines = expenseLines,
            totalCents = transportTotal + expenseTotal,
            balances = ledger.map { (pid, l) -> (names[pid] ?: "#$pid") to l.netBalanceCents },
            refundsOutstandingCents = rec.refunds.filter { it.transportId in dayTransportIds && !it.collected }.sumOf { it.amountCents },
            joke = if (funEnabled.value) jokes.pick("receipt") else null,
        )
    }

    /** Day id a new record should belong to, or null (after telling the user) when no day is open. */
    private suspend fun requireOpenDayId(): Long? {
        val day = repo.currentOpenDay()
        if (day == null) _events.emit(UiEvent.Message(R.string.no_open_day))
        return day?.id
    }

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
            .onSuccess { if (id == null) feedback("person_added", R.string.saved_successfully) else _events.emit(UiEvent.Message(R.string.saved_successfully)) }
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
            .onSuccess { if (id == null) feedback("route_saved", R.string.saved_successfully) else _events.emit(UiEvent.Message(R.string.saved_successfully)) }
            .onFailure {
                _events.emit(UiEvent.Message(if (it is DuplicateNameException) R.string.duplicate_route else R.string.error_required))
            }
    }

    fun deleteRoute(id: Long) = viewModelScope.launch {
        repo.deleteRoute(id)
        _events.emit(UiEvent.Message(R.string.deleted_successfully))
    }

    // ---- Transport ----

    fun saveTransport(id: Long?, inputIn: TransportInput, markRefundsCollected: Boolean? = null) = viewModelScope.launch {
        var input = inputIn
        if (id == null) {
            val openId = requireOpenDayId() ?: return@launch
            input = inputIn.copy(dayId = openId)
        }
        runCatching { repo.saveTransport(id, input) }
            .onSuccess { (_, refunds) ->
                if (markRefundsCollected != null) {
                    refunds.forEach { repo.setRefundCollected(it.id, markRefundsCollected) }
                }
                if (refunds.isNotEmpty() && markRefundsCollected == null) {
                    _events.emit(UiEvent.RefundPrompt(refunds))
                } else {
                    val total = input.fareCents * input.paid.size
                    val onePayer = input.paid.size > 1 && input.paid.values.count { it > 0 } == 1 &&
                        input.paid.values.sum() >= total
                    val bucket = if (onePayer) "trip_one_payer" else
                        dev.ahmeddawood.elghassala.playful.JokeBuckets.tripCandidates(
                            input.paid.size, input.fareCents,
                            dev.ahmeddawood.elghassala.playful.JokeBuckets.hourOf(input.dateMillis),
                        ).random()
                    feedback(bucket, R.string.transport_saved)
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

    fun saveExpense(id: Long?, inputIn: ExpenseInput) = viewModelScope.launch {
        var input = inputIn
        if (id == null) {
            val openId = requireOpenDayId() ?: return@launch
            input = inputIn.copy(dayId = openId)
        }
        runCatching { repo.saveExpense(id, input) }
            .onSuccess {
                feedback(
                    dev.ahmeddawood.elghassala.playful.JokeBuckets.expenseCandidates(input.title).random(),
                    R.string.saved_successfully,
                )
            }
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
                val n = notes.lowercase()
                val bucket = when {
                    kind == TransferKind.SETTLEMENT -> "settlement_recorded"
                    "instapay" in n || "انستا" in n || "إنستا" in n -> "transfer_instapay"
                    "vodafone" in n || "فودافون" in n -> "transfer_vodafone"
                    else -> "transfer_cash"
                }
                feedback(bucket, if (kind == TransferKind.SETTLEMENT) R.string.settlement_recorded else R.string.saved_successfully)
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
        if (collected) feedback("refund_collected", R.string.refund_marked_collected)
        else _events.emit(UiEvent.Message(R.string.refund_kept_outstanding))
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
    val importNotice = MutableStateFlow<String?>(null)
    fun clearImportNotice() { importNotice.value = null }
    fun clearLegacyImportResult() { legacyImportResult.value = null }

    fun importLegacyBackup(uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("no stream")
            repo.importLegacy(text)
        }.onSuccess { legacyImportResult.value = it; feedback("import_done", R.string.legacy_ok) }
            .onFailure {
                val app = getApplication<Application>()
                importNotice.value = app.getString(R.string.legacy_import_failed) + "\n\n" + (it.message ?: it.javaClass.simpleName)
            }
    }

    enum class ImportKind { LEGACY, BACKUP }
    data class PendingImport(val kind: ImportKind, val text: String)

    /** Step 1 of the single import button: read the file and detect which format it is. */
    val pendingImport = MutableStateFlow<PendingImport?>(null)
    fun cancelPendingImport() { pendingImport.value = null }

    fun prepareImport(uri: Uri) = viewModelScope.launch {
        val app = getApplication<Application>()
        val text = runCatching {
            app.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("no stream")
        }.getOrElse {
            importNotice.value = app.getString(R.string.import_unreadable) + "\n\n" + (it.message ?: "")
            return@launch
        }
        val marker = runCatching { org.json.JSONObject(text).optString("app", "").trim().lowercase() }.getOrNull()
        pendingImport.value = when (marker) {
            "elghassala" -> PendingImport(ImportKind.BACKUP, text)
            "el ghasala", "el ghassala legacy" -> PendingImport(ImportKind.LEGACY, text)
            else -> {
                importNotice.value = app.getString(R.string.import_unknown_format)
                null
            }
        }
    }

    /** Step 2: the user confirmed. */
    fun confirmPendingImport() = viewModelScope.launch {
        val p = pendingImport.value ?: return@launch
        pendingImport.value = null
        val app = getApplication<Application>()
        busy.value = true
        runCatching {
            kotlinx.coroutines.delay(600) // let the washing machine be seen

            when (p.kind) {
                ImportKind.LEGACY -> {
                    val r = repo.importLegacy(p.text)
                    legacyImportResult.value = r
                    feedback("import_done", R.string.legacy_ok)
                }
                ImportKind.BACKUP -> {
                    repo.replaceAll(BackupManager.parse(p.text))
                    importNotice.value = app.getString(R.string.restore_success)
                }
            }
        }.also { busy.value = false }.onFailure {
            importNotice.value = app.getString(
                if (p.kind == ImportKind.LEGACY) R.string.legacy_import_failed else R.string.restore_failed
            ) + "\n\n" + (it.message ?: it.javaClass.simpleName)
        }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("no stream")
            repo.replaceAll(BackupManager.parse(text))
        }.onSuccess { importNotice.value = getApplication<Application>().getString(R.string.restore_success) }
            .onFailure {
                importNotice.value = getApplication<Application>().getString(R.string.restore_failed) + "\n\n" + (it.message ?: it.javaClass.simpleName)
            }
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
