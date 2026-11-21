package dev.ahmeddawood.elghassala.export

import dev.ahmeddawood.elghassala.data.db.ExpenseEntity
import dev.ahmeddawood.elghassala.data.db.TransferEntity
import dev.ahmeddawood.elghassala.data.db.TransferKind
import dev.ahmeddawood.elghassala.data.db.TransportEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** CSV export with UTF-8 BOM so spreadsheet apps read Arabic correctly. */
object CsvExporter {

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun money(cents: Long): String = "%.2f".format(Locale.US, cents / 100.0)

    private fun date(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))

    fun export(
        transports: List<TransportEntity>,
        transportParticipants: Map<Long, List<Pair<String, Long>>>,
        expenses: List<ExpenseEntity>,
        transfers: List<TransferEntity>,
        peopleNames: Map<Long, String>,
    ): String {
        val sb = StringBuilder("﻿")
        sb.appendLine("type,date,title,from,to,amount_egp,paid_by,participants,notes")
        transports.forEach { t ->
            val participants = transportParticipants[t.id].orEmpty()
            val paidBy = participants.joinToString(" | ") { (name, paid) -> "$name=${money(paid)}" }
            val names = participants.joinToString(" | ") { it.first }
            sb.appendLine(
                listOf(
                    "transport", date(t.dateMillis), escape("${t.origin} -> ${t.destination}"),
                    escape(t.origin), escape(t.destination), money(t.fareCents * participants.size),
                    escape(paidBy), escape(names), escape(t.notes),
                ).joinToString(",")
            )
        }
        expenses.forEach { e ->
            sb.appendLine(
                listOf(
                    "expense", date(e.dateMillis), escape(e.title), "", "", money(e.totalCents), "", "", escape(e.notes),
                ).joinToString(",")
            )
        }
        transfers.forEach { t ->
            sb.appendLine(
                listOf(
                    if (t.kind == TransferKind.SETTLEMENT) "settlement" else "transfer", date(t.dateMillis), "",
                    escape(peopleNames[t.fromPersonId] ?: ""), escape(peopleNames[t.toPersonId] ?: ""),
                    money(t.amountCents), "", "", escape(t.notes),
                ).joinToString(",")
            )
        }
        return sb.toString()
    }
}
