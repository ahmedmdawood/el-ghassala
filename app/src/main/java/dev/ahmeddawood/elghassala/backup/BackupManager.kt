package dev.ahmeddawood.elghassala.backup

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
import dev.ahmeddawood.elghassala.data.repo.GhassalaRepository.Snapshot
import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned JSON backup. Restore validates the envelope before touching the
 * database and replaces data inside a single transaction.
 */
object BackupManager {

    const val BACKUP_VERSION = 1

    class InvalidBackupException(message: String) : Exception(message)

    fun export(snapshot: Snapshot): String {
        val root = JSONObject()
        root.put("app", "elghassala")
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("days", JSONArray().also { arr ->
            snapshot.days.forEach { d ->
                arr.put(JSONObject().apply {
                    put("id", d.id); put("title", d.title); put("dateMillis", d.dateMillis)
                    put("isClosed", d.isClosed); put("createdAt", d.createdAt)
                })
            }
        })
        root.put("people", JSONArray().also { arr ->
            snapshot.people.forEach { p ->
                arr.put(JSONObject().apply {
                    put("id", p.id); put("name", p.name); put("archived", p.archived); put("createdAt", p.createdAt)
                })
            }
        })
        root.put("routes", JSONArray().also { arr ->
            snapshot.routes.forEach { r ->
                arr.put(JSONObject().apply {
                    put("id", r.id); put("name", r.name); put("origin", r.origin); put("destination", r.destination)
                    put("defaultFareCents", r.defaultFareCents); put("usageCount", r.usageCount)
                    put("createdAt", r.createdAt); put("updatedAt", r.updatedAt)
                    if (r.lastUsedAt != null) put("lastUsedAt", r.lastUsedAt)
                })
            }
        })
        root.put("transports", JSONArray().also { arr ->
            snapshot.transports.forEach { t ->
                arr.put(JSONObject().apply {
                    put("id", t.id); if (t.dayId != null) put("dayId", t.dayId)
                    put("origin", t.origin); put("destination", t.destination)
                    put("fareCents", t.fareCents); put("dateMillis", t.dateMillis)
                    put("notes", t.notes); if (t.routeId != null) put("routeId", t.routeId)
                })
            }
        })
        root.put("transportParticipants", JSONArray().also { arr ->
            snapshot.transportParticipants.forEach { tp ->
                arr.put(JSONObject().apply {
                    put("id", tp.id); put("transportId", tp.transportId)
                    put("personId", tp.personId); put("paidCents", tp.paidCents)
                })
            }
        })
        root.put("driverRefunds", JSONArray().also { arr ->
            snapshot.refunds.forEach { r ->
                arr.put(JSONObject().apply {
                    put("id", r.id); put("transportId", r.transportId); put("personId", r.personId)
                    put("amountCents", r.amountCents); put("collected", r.collected)
                    if (r.collectedAt != null) put("collectedAt", r.collectedAt); put("createdAt", r.createdAt)
                })
            }
        })
        root.put("expenses", JSONArray().also { arr ->
            snapshot.expenses.forEach { e ->
                arr.put(JSONObject().apply {
                    put("id", e.id); if (e.dayId != null) put("dayId", e.dayId)
                    put("title", e.title); put("totalCents", e.totalCents)
                    put("dateMillis", e.dateMillis); put("notes", e.notes)
                })
            }
        })
        root.put("expensePayers", JSONArray().also { arr ->
            snapshot.expensePayers.forEach { p ->
                arr.put(JSONObject().apply {
                    put("id", p.id); put("expenseId", p.expenseId); put("personId", p.personId); put("paidCents", p.paidCents)
                })
            }
        })
        root.put("expenseBeneficiaries", JSONArray().also { arr ->
            snapshot.expenseBeneficiaries.forEach { b ->
                arr.put(JSONObject().apply {
                    put("id", b.id); put("expenseId", b.expenseId); put("personId", b.personId); put("shareCents", b.shareCents)
                })
            }
        })
        root.put("transfers", JSONArray().also { arr ->
            snapshot.transfers.forEach { t ->
                arr.put(JSONObject().apply {
                    put("id", t.id); if (t.dayId != null) put("dayId", t.dayId)
                    put("fromPersonId", t.fromPersonId); put("toPersonId", t.toPersonId)
                    put("amountCents", t.amountCents); put("kind", t.kind.name)
                    put("dateMillis", t.dateMillis); put("notes", t.notes)
                })
            }
        })
        return root.toString(2)
    }

    fun parse(json: String): Snapshot {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw InvalidBackupException("not a JSON object")
        }
        if (root.optString("app") != "elghassala") throw InvalidBackupException("not an El Ghassala backup")
        val version = root.optInt("version", -1)
        if (version < 1 || version > BACKUP_VERSION) throw InvalidBackupException("unsupported backup version $version")

        fun JSONObject.longOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null

        val days = root.getJSONArray("days").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DayEntity(
                    id = o.getLong("id"), title = o.getString("title"), dateMillis = o.getLong("dateMillis"),
                    isClosed = o.optBoolean("isClosed"), createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
            }
        }
        val people = root.getJSONArray("people").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                PersonEntity(
                    id = o.getLong("id"), name = o.getString("name"),
                    archived = o.optBoolean("archived"), createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
            }
        }
        val routes = root.getJSONArray("routes").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                RouteEntity(
                    id = o.getLong("id"), name = o.getString("name"), origin = o.getString("origin"),
                    destination = o.getString("destination"), defaultFareCents = o.getLong("defaultFareCents"),
                    usageCount = o.optInt("usageCount"), createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()), lastUsedAt = o.longOrNull("lastUsedAt"),
                )
            }
        }
        val transports = root.getJSONArray("transports").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                TransportEntity(
                    id = o.getLong("id"), dayId = o.longOrNull("dayId"), origin = o.getString("origin"),
                    destination = o.getString("destination"), fareCents = o.getLong("fareCents"),
                    dateMillis = o.getLong("dateMillis"), notes = o.optString("notes"), routeId = o.longOrNull("routeId"),
                )
            }
        }
        val participants = root.getJSONArray("transportParticipants").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                TransportParticipantEntity(
                    id = o.getLong("id"), transportId = o.getLong("transportId"),
                    personId = o.getLong("personId"), paidCents = o.getLong("paidCents"),
                )
            }
        }
        val refunds = root.getJSONArray("driverRefunds").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DriverRefundEntity(
                    id = o.getLong("id"), transportId = o.getLong("transportId"), personId = o.getLong("personId"),
                    amountCents = o.getLong("amountCents"), collected = o.optBoolean("collected"),
                    collectedAt = o.longOrNull("collectedAt"), createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
            }
        }
        val expenses = root.getJSONArray("expenses").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ExpenseEntity(
                    id = o.getLong("id"), dayId = o.longOrNull("dayId"), title = o.getString("title"),
                    totalCents = o.getLong("totalCents"), dateMillis = o.getLong("dateMillis"), notes = o.optString("notes"),
                )
            }
        }
        val payers = root.getJSONArray("expensePayers").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ExpensePayerEntity(
                    id = o.getLong("id"), expenseId = o.getLong("expenseId"),
                    personId = o.getLong("personId"), paidCents = o.getLong("paidCents"),
                )
            }
        }
        val beneficiaries = root.getJSONArray("expenseBeneficiaries").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ExpenseBeneficiaryEntity(
                    id = o.getLong("id"), expenseId = o.getLong("expenseId"),
                    personId = o.getLong("personId"), shareCents = o.getLong("shareCents"),
                )
            }
        }
        val transfers = root.getJSONArray("transfers").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                TransferEntity(
                    id = o.getLong("id"), dayId = o.longOrNull("dayId"),
                    fromPersonId = o.getLong("fromPersonId"), toPersonId = o.getLong("toPersonId"),
                    amountCents = o.getLong("amountCents"),
                    kind = TransferKind.valueOf(o.optString("kind", "TRANSFER")),
                    dateMillis = o.getLong("dateMillis"), notes = o.optString("notes"),
                )
            }
        }
        return Snapshot(days, people, routes, transports, participants, refunds, expenses, payers, beneficiaries, transfers)
    }
}
