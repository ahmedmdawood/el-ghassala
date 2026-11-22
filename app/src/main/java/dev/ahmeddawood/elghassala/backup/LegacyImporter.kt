package dev.ahmeddawood.elghassala.backup

import org.json.JSONArray
import org.json.JSONObject

/**
 * One-time importer for backups exported by the OLD El Ghasala app
 * ("app": "El Ghasala", "version": 1). Pure parsing lives here so it is
 * unit-testable; database writes live in GhassalaRepository.importLegacy.
 *
 * Old money fields are whole/fractional EGP; the new schema stores piasters
 * (Long), so every amount is multiplied by 100. Old and new refund semantics
 * agree (excess over total trip cost goes back to whoever overpaid), so
 * refunds are recomputed per transport and then reconciled with the old
 * refund rows for collected state.
 */
object LegacyImporter {

    class InvalidLegacyBackupException(message: String) : Exception(message)

    data class LegacyDay(val id: Long, val title: String, val date: String?, val isClosed: Boolean, val createdAt: Long)
    data class LegacyPerson(val displayName: String, val archived: Boolean, val createdAt: Long?)
    data class LegacyRoute(val title: String, val origin: String, val destination: String, val defaultFare: Double, val usageCount: Int, val createdAt: Long, val updatedAt: Long)
    data class LegacyTransport(
        val id: Long, val dayId: Long?, val timestamp: Long, val notes: String,
        val origin: String, val destination: String, val farePerPerson: Double,
        val participants: List<String>, val payerPayments: Map<String, Double>,
    )
    data class LegacyExpense(
        val id: Long, val dayId: Long?, val timestamp: Long, val notes: String,
        val title: String, val category: String,
        val payerPayments: Map<String, Double>, val beneficiaryShares: Map<String, Double>,
    )
    data class LegacyTransfer(
        val id: Long, val dayId: Long?, val timestamp: Long, val notes: String,
        val from: String, val to: String, val amount: Double,
    )
    data class LegacyRefund(
        val transactionId: Long, val participantName: String,
        val amount: Double, val isCollected: Boolean, val collectedAt: Long?, val createdAt: Long,
    )

    data class LegacyExport(
        val days: List<LegacyDay>,
        val people: List<LegacyPerson>,
        val routes: List<LegacyRoute>,
        val transports: List<LegacyTransport>,
        val expenses: List<LegacyExpense>,
        val transfers: List<LegacyTransfer>,
        val refunds: List<LegacyRefund>,
    ) {
        /** Every participant name referenced anywhere, in first-seen order. */
        val allNames: List<String>
            get() {
                val seen = LinkedHashSet<String>()
                transports.forEach { t ->
                    t.participants.forEach { seen.add(it.trim()) }
                    t.payerPayments.keys.forEach { seen.add(it.trim()) }
                }
                expenses.forEach { e ->
                    e.payerPayments.keys.forEach { seen.add(it.trim()) }
                    e.beneficiaryShares.keys.forEach { seen.add(it.trim()) }
                }
                transfers.forEach { seen.add(it.from.trim()); seen.add(it.to.trim()) }
                refunds.forEach { seen.add(it.participantName.trim()) }
                people.forEach { seen.add(it.displayName.trim()) }
                return seen.filter { it.isNotEmpty() }
            }
    }

    data class Result(
        val daysAdded: Int,
        val peopleAdded: Int,
        val peopleReused: Int,
        val routesAdded: Int,
        val transportsAdded: Int,
        val expensesAdded: Int,
        val transfersAdded: Int,
        val refundsMarkedCollected: Int,
        val warnings: List<String>,
    )

    fun parse(json: String): LegacyExport {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw InvalidLegacyBackupException("not a JSON object")
        }
        val app = root.optString("app", "").trim()
        if (!app.equals("El Ghasala", ignoreCase = true) && !app.equals("El Ghassala Legacy", ignoreCase = true)) {
            throw InvalidLegacyBackupException("unexpected app marker: $app")
        }
        if (root.optInt("version", -1) != 1) throw InvalidLegacyBackupException("unsupported version")

        fun JSONObject.strMap(key: String): Map<String, Double> {
            val o = optJSONObject(key) ?: return emptyMap()
            val out = LinkedHashMap<String, Double>()
            o.keys().forEach { k -> out[k] = o.optDouble(k, 0.0) }
            return out
        }
        fun JSONObject.strList(key: String): List<String> {
            val a = optJSONArray(key) ?: return emptyList()
            return (0 until a.length()).map { a.optString(it, "") }.filter { it.isNotBlank() }
        }

        val days = root.optJSONArray("days").objListOrEmpty().map { d ->
            LegacyDay(
                id = d.optLong("id"), title = d.optString("title").trim(),
                date = d.optString("date").ifBlank { null },
                isClosed = d.optBoolean("isClosed"), createdAt = d.optLong("createdAt"),
            )
        }
        val people = root.optJSONArray("people").objListOrEmpty().map { p ->
            LegacyPerson(
                displayName = p.optString("displayName").trim(),
                archived = p.optBoolean("isArchived"),
                createdAt = p.optLong("createdAt").takeIf { it > 0 },
            )
        }
        val routes = root.optJSONArray("savedRoutes").objListOrEmpty().map { r ->
            LegacyRoute(
                title = r.optString("title").trim(),
                origin = r.optString("origin").trim(), destination = r.optString("destination").trim(),
                defaultFare = r.optDouble("defaultFare"), usageCount = r.optInt("usageCount"),
                createdAt = r.optLong("createdAt"), updatedAt = r.optLong("updatedAt"),
            )
        }

        val transports = mutableListOf<LegacyTransport>()
        val expenses = mutableListOf<LegacyExpense>()
        val transfers = mutableListOf<LegacyTransfer>()
        root.optJSONArray("transactions").objListOrEmpty().forEach { t ->
            when (t.optString("type")) {
                "TRANSPORT" -> transports += LegacyTransport(
                    id = t.optLong("id"), dayId = t.optLong("dayId").takeIf { it > 0 },
                    timestamp = t.optLong("timestamp"), notes = t.optString("notes"),
                    origin = t.optString("origin").trim(), destination = t.optString("destination").trim(),
                    farePerPerson = t.optDouble("farePerPerson"),
                    participants = t.strList("tripParticipants"),
                    payerPayments = t.strMap("payerPayments"),
                )
                "EXPENSE" -> expenses += LegacyExpense(
                    id = t.optLong("id"), dayId = t.optLong("dayId").takeIf { it > 0 },
                    timestamp = t.optLong("timestamp"), notes = t.optString("notes"),
                    title = t.optString("expenseTitle").trim(), category = t.optString("category").trim(),
                    payerPayments = t.strMap("payerPayments"),
                    beneficiaryShares = t.strMap("beneficiaryShares"),
                )
                "TRANSFER" -> transfers += LegacyTransfer(
                    id = t.optLong("id"), dayId = t.optLong("dayId").takeIf { it > 0 },
                    timestamp = t.optLong("timestamp"), notes = t.optString("notes"),
                    from = t.optString("fromParticipant").trim(), to = t.optString("toParticipant").trim(),
                    amount = t.optDouble("transferAmount"),
                )
            }
        }

        val refunds = root.optJSONArray("driverRefunds").objListOrEmpty().map { r ->
            LegacyRefund(
                transactionId = r.optLong("transactionId"),
                participantName = r.optString("participantName").trim(),
                amount = r.optDouble("amount"),
                isCollected = r.optBoolean("isCollected"),
                collectedAt = r.optLong("collectedAt").takeIf { it > 0 },
                createdAt = r.optLong("createdAt"),
            )
        }

        return LegacyExport(
            days = days, people = people, routes = routes,
            transports = transports.sortedBy { it.timestamp },
            expenses = expenses.sortedBy { it.timestamp },
            transfers = transfers.sortedBy { it.timestamp },
            refunds = refunds,
        )
    }

    private fun JSONArray?.objListOrEmpty(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
