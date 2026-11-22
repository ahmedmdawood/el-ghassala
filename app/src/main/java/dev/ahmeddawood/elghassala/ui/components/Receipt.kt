package dev.ahmeddawood.elghassala.ui.components

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.domain.Money

/** Everything the end-of-day receipt prints. Built when the day is closed. */
data class DayReceipt(
    val dayTitle: String,
    val dateText: String,
    val tripLines: List<ReceiptLine>,
    val expenseLines: List<ReceiptLine>,
    val totalCents: Long,
    val balances: List<Pair<String, Long>>,
    val refundsOutstandingCents: Long,
    val joke: String?,
)

data class ReceiptLine(val label: String, val detail: String, val amountCents: Long)

private val Paper = Color(0xFFFFFBEF)
private val Ink = Color(0xFF2B2A26)
private val InkSoft = Color(0xFF7A766A)

@Composable
private fun Dashes() {
    Canvas(Modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            InkSoft, Offset(0f, 1f), Offset(size.width, 1f), strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
        )
    }
}

@Composable
private fun ReceiptRow(left: String, right: String, bold: Boolean = false, color: Color = Ink) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            left, color = color, fontFamily = FontFamily.Monospace, fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f),
        )
        Text(
            right, color = color, fontFamily = FontFamily.Monospace, fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private fun zigzag(width: Float, top: Boolean, h: Float): Path {
    val p = Path()
    val teeth = 22
    val step = width / teeth
    p.moveTo(0f, if (top) h else 0f)
    for (i in 0 until teeth) {
        p.lineTo(i * step + step / 2f, if (top) 0f else h)
        p.lineTo((i + 1) * step, if (top) h else 0f)
    }
    p.lineTo(width, if (top) h else 0f)
    p.close()
    return p
}

@Composable
fun ReceiptOverlay(receipt: DayReceipt, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val shareText = remember(receipt) { receiptText(receipt) }
    Box(
        Modifier.fillMaxSize().background(Color(0xE6062E27))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { },
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.weight(1f, fill = false)) {
                Canvas(Modifier.fillMaxWidth().height(10.dp)) { drawPath(zigzag(size.width, top = true, h = size.height), Paper) }
                Column(
                    Modifier.fillMaxWidth().background(Paper).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        stringResource(R.string.receipt_title), color = Ink, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "${receipt.dayTitle}  ·  ${receipt.dateText}", color = InkSoft, fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    Dashes()
                    if (receipt.tripLines.isNotEmpty()) {
                        Text(stringResource(R.string.receipt_trips), color = InkSoft, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        receipt.tripLines.forEach {
                            ReceiptRow("${it.label} ${it.detail}", Money.shown(it.amountCents))
                        }
                    }
                    if (receipt.expenseLines.isNotEmpty()) {
                        Text(stringResource(R.string.receipt_expenses), color = InkSoft, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        receipt.expenseLines.forEach { ReceiptRow(it.label, Money.shown(it.amountCents)) }
                    }
                    if (receipt.tripLines.isEmpty() && receipt.expenseLines.isEmpty()) {
                        Text(stringResource(R.string.receipt_empty), color = InkSoft, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                    Dashes()
                    ReceiptRow(stringResource(R.string.receipt_total), Money.shown(receipt.totalCents), bold = true)
                    if (receipt.balances.any { it.second != 0L }) {
                        Dashes()
                        Text(stringResource(R.string.receipt_balances), color = InkSoft, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        receipt.balances.filter { it.second != 0L }.forEach { (n, v) ->
                            ReceiptRow(n, Money.shown(v), color = if (v > 0) Color(0xFF2E7D55) else Color(0xFFB4372C))
                        }
                    }
                    if (receipt.refundsOutstandingCents > 0) {
                        ReceiptRow(stringResource(R.string.receipt_refunds), Money.shown(receipt.refundsOutstandingCents))
                    }
                    Dashes()
                    Text(
                        receipt.joke ?: stringResource(R.string.receipt_thanks), color = Ink, fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    )
                    Text(
                        "* * *  ${stringResource(R.string.receipt_footer)}  * * *", color = InkSoft, fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Canvas(Modifier.fillMaxWidth().height(10.dp)) { drawPath(zigzag(size.width, top = false, h = size.height), Paper) }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                    ctx.startActivity(Intent.createChooser(send, null))
                }) { Text(stringResource(R.string.receipt_share), color = Color(0xFFF2C76B)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.receipt_done), color = Color.White) }
            }
        }
    }
}

/** Plain-text version for sharing (real amounts: the user chose to share it). */
fun receiptText(r: DayReceipt): String = buildString {
    appendLine("🧺 ${r.dayTitle} · ${r.dateText}")
    r.tripLines.forEach { appendLine("${it.label} ${it.detail}: ${Money.formatWithCurrency(it.amountCents)}") }
    r.expenseLines.forEach { appendLine("${it.label}: ${Money.formatWithCurrency(it.amountCents)}") }
    appendLine("= ${Money.formatWithCurrency(r.totalCents)}")
    r.balances.filter { it.second != 0L }.forEach { appendLine("${it.first}: ${Money.formatWithCurrency(it.second)}") }
}.trim()
