package dev.ahmeddawood.elghassala.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.ahmeddawood.elghassala.AppViewModel
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.domain.BalanceCalculator
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.ui.components.EmptyState
import dev.ahmeddawood.elghassala.ui.components.GradientHeader
import java.util.Calendar

private enum class Period { TODAY, WEEK, MONTH, YEAR }

@Composable
fun StatsScreen(vm: AppViewModel, nav: NavHostController) {
    val records by vm.records.collectAsState()
    var period by remember { mutableStateOf(Period.MONTH) }

    val (start, end) = remember(period) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val e: Long
        when (period) {
            Period.TODAY -> { e = cal.timeInMillis + 24L * 3600_000 }
            Period.WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -(cal.get(Calendar.DAY_OF_WEEK) - cal.firstDayOfWeek + 7) % 7)
                e = cal.timeInMillis + 7L * 24 * 3600_000
            }
            Period.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                e = run { val c = cal.clone() as Calendar; c.add(Calendar.MONTH, 1); c.timeInMillis }
            }
            Period.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                e = run { val c = cal.clone() as Calendar; c.add(Calendar.YEAR, 1); c.timeInMillis }
            }
        }
        cal.timeInMillis to e
    }

    val stats = remember(records, start, end) {
        BalanceCalculator.stats(
            startMillis = start, endMillis = end,
            transports = records.transportRecords(),
            expenses = records.expenseRecords(),
            transfers = records.transferRecords(),
            refunds = records.refundRecords(),
            dayLengthMillis = 24L * 3600_000,
        )
    }

    val periodLabels = listOf(
        Period.TODAY to stringResource(R.string.period_today),
        Period.WEEK to stringResource(R.string.period_week),
        Period.MONTH to stringResource(R.string.period_month),
        Period.YEAR to stringResource(R.string.period_year),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            GradientHeader(
                title = stringResource(R.string.stats_title),
                subtitle = stringResource(R.string.spending_trend),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column {
                        Text(
                            stringResource(R.string.actual_expenses),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            Money.shown(stats.actualExpensesCents),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.avg_daily_spending),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            Money.shown(stats.averageDailyCents),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(periodLabels.size) { i ->
                    val (p, label) = periodLabels[i]
                    FilterChip(selected = period == p, onClick = { period = p }, label = { Text(label) })
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.spending_trend), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        if (stats.dailyBuckets.all { it.second == 0L }) {
                            Text(
                                stringResource(R.string.no_data_period),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            BarChart(buckets = stats.dailyBuckets)
                        }
                    }
                }
            }
        }

        item { AwardsCard(records) }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatRow(stringResource(R.string.transport_expenses), stats.transportCents)
                        StatRow(stringResource(R.string.cash_expenses), stats.cashCents)
                        StatRow(stringResource(R.string.transfer_volume), stats.transferVolumeCents)
                        StatRow(stringResource(R.string.outstanding_refunds_stat), stats.outstandingRefundsCents)
                        StatRow(stringResource(R.string.collected_refunds_stat), stats.collectedRefundsCents)
                        StatRow(stringResource(R.string.vs_previous_period), stats.previousPeriodExpensesCents)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: Long) {
    RowSpaceBetween {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(Money.shown(value), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun BarChart(buckets: List<Pair<Long, Long>>) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val max = (buckets.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    val shown = if (buckets.size > 31) buckets.takeLast(31) else buckets
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val count = shown.size.coerceAtLeast(1)
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2f)
        shown.forEachIndexed { i, (_, v) ->
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = track,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            )
            if (v > 0) {
                val h = size.height * (v.toFloat() / max)
                drawRoundRect(
                    color = if (i == shown.size - 1) tertiary else primary,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                )
            }
        }
    }
}
