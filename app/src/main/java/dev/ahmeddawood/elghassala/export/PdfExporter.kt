package dev.ahmeddawood.elghassala.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.domain.BalanceCalculator.PersonLedger
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Simple branded PDF report drawn with PdfDocument. */
class PdfExporter(private val context: Context) {

    fun writeReport(
        out: OutputStream,
        title: String,
        periodLabel: String,
        ledgers: List<PersonLedger>,
        peopleNames: Map<Long, String>,
        transportTotals: Map<Long, Long>? = null,
    ) {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        val teal = android.graphics.Color.rgb(14, 124, 102)
        val amber = android.graphics.Color.rgb(217, 164, 65)
        val ink = android.graphics.Color.rgb(16, 32, 27)
        val muted = android.graphics.Color.rgb(90, 110, 103)
        val green = android.graphics.Color.rgb(46, 158, 107)
        val red = android.graphics.Color.rgb(217, 106, 95)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(220, 255, 255, 255)
            textSize = 12f
        }
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = teal
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 12f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 10f
        }
        val bandPaint = Paint().apply { color = teal }

        var pageNum = 1
        fun newPage(): Pair<PdfDocument.Page, android.graphics.Canvas> {
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val page = doc.startPage(info)
            val c = page.canvas
            c.drawRect(0f, 0f, pageWidth.toFloat(), 90f, bandPaint)
            Paint().apply { color = amber }.let { c.drawRect(0f, 90f, pageWidth.toFloat(), 94f, it) }
            c.drawText(title, margin, 42f, titlePaint)
            c.drawText(periodLabel, margin, 66f, subPaint)
            return page to c
        }

        var (page, canvas) = newPage()
        var y = 130f

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - 70f) {
                canvas.drawText("${context.getString(R.string.page)} $pageNum", pageWidth - margin - 60f, pageHeight - 30f, mutedPaint)
                doc.finishPage(page)
                pageNum++
                val (p, c) = newPage()
                page = p
                canvas = c
                y = 130f
            }
        }

        canvas.drawText(context.getString(R.string.pdf_balances_title), margin, y, headPaint)
        y += 26f

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        ledgers.sortedBy { peopleNames[it.personId] ?: "" }.forEach { ledger ->
            ensureSpace(60f)
            val name = peopleNames[ledger.personId] ?: "#${ledger.personId}"
            val transportTotal = transportTotals?.get(ledger.personId)
            val balanceColor = when {
                ledger.netBalanceCents > 0 -> green
                ledger.netBalanceCents < 0 -> red
                else -> muted
            }
            canvas.drawText(name, margin, y, bodyPaint)
            Paint(bodyPaint).apply { color = balanceColor; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
                .let { canvas.drawText(Money.formatWithCurrency(ledger.netBalanceCents), pageWidth - margin - it.measureText(Money.formatWithCurrency(ledger.netBalanceCents)), y, it) }
            y += 16f
            val parts = mutableListOf<String>()
            if (transportTotal != null && transportTotal > 0) {
                parts.add("${context.getString(R.string.pdf_transport_spent)}: ${Money.formatWithCurrency(transportTotal)}")
            }
            parts.add("${context.getString(R.string.pdf_refunds)}: ${Money.formatWithCurrency(ledger.refundsOutstandingCents)}")
            parts.add("${context.getString(R.string.pdf_transfers)}: ${Money.formatWithCurrency(ledger.transfersSentCents)} / ${Money.formatWithCurrency(ledger.transfersReceivedCents)}")
            canvas.drawText(parts.joinToString("   "), margin + 8f, y, mutedPaint)
            y += 20f
        }

        y += 10f
        ensureSpace(30f)
        canvas.drawText(
            "${context.getString(R.string.pdf_generated)} ${dateFormat.format(Date())} - ${context.getString(R.string.made_by)}",
            margin, pageHeight - 30f, mutedPaint,
        )
        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }
}
