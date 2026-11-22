package dev.ahmeddawood.elghassala.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.ahmeddawood.elghassala.AppViewModel
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.ui.components.AnimatedMoney
import dev.ahmeddawood.elghassala.ui.components.BalanceText
import dev.ahmeddawood.elghassala.ui.components.EmptyState
import dev.ahmeddawood.elghassala.ui.components.GradientHeader
import dev.ahmeddawood.elghassala.ui.components.PressScaleCard
import dev.ahmeddawood.elghassala.ui.nav.Routes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(vm: AppViewModel, nav: NavHostController) {
    val records by vm.records.collectAsState()
    val ledgers by vm.ledgers.collectAsState()
    val avatars by vm.avatars.collectAsState()

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = todayStart + 24L * 60 * 60 * 1000

    val todayTransport = records.transports.filter { it.dateMillis in todayStart until todayEnd }
    val todayExpenses = records.expenses.filter { it.dateMillis in todayStart until todayEnd }
    val todaySpend = todayTransport.sumOf { t ->
        val riders = records.participants.count { it.transportId == t.id }.coerceAtLeast(1)
        t.fareCents * riders
    } + todayExpenses.sumOf { it.totalCents }

    val outstandingRefunds = records.refunds.filter { !it.collected }.sumOf { it.amountCents }
    val recent = remember(records) {
        (records.transports.take(2).map { Triple("t", it.id, it.dateMillis) } +
            records.expenses.take(2).map { Triple("e", it.id, it.dateMillis) } +
            records.transfers.take(2).map { Triple("x", it.id, it.dateMillis) })
            .sortedByDescending { it.third }.take(5)
    }
    val netBalances = ledgers.mapValues { it.value.netBalanceCents }.filterValues { it != 0L }
    val openDays = records.days.filter { !it.isClosed }
    val openDay by vm.openDay.collectAsState()
    var confirmEnd by remember { mutableStateOf(false) }
    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text(stringResource(R.string.end_day_confirm_title)) },
            text = { Text(stringResource(R.string.end_day_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { confirmEnd = false; vm.endDay() }) {
                    Text(stringResource(R.string.end_day_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) { Text(stringResource(R.string.end_day_confirm_no)) }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            GradientHeader(
                title = stringResource(R.string.dashboard_title),
                subtitle = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
                trailing = {
                    val hidden by vm.privacyMode.collectAsState()
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(onClick = { vm.togglePrivacy() }) {
                            Icon(
                                if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.privacy_toggle),
                            )
                        }
                        FilledTonalButton(onClick = { nav.navigate(Routes.SEARCH) }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                        }
                    }
                },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text(
                            stringResource(R.string.today),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        AnimatedMoney(
                            cents = todaySpend,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 26,
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.outstanding_refunds),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        AnimatedMoney(
                            cents = outstandingRefunds,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 26,
                        )
                    }
                }
            }
        }

        item {
            val day = openDay
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                if (day == null) {
                    val pulse = rememberInfiniteTransition(label = "pulse")
                    val scale by pulse.animateFloat(
                        initialValue = 1f, targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulseScale",
                    )
                    SectionCard {
                        Column(
                            Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Filled.WbSunny, null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(44.dp).graphicsLayer { scaleX = scale; scaleY = scale },
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(stringResource(R.string.start_day_title), style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.start_day_sub),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { vm.startDay() },
                                modifier = Modifier.fillMaxWidth().height(54.dp).graphicsLayer { scaleX = scale; scaleY = scale },
                            ) {
                                Text(stringResource(R.string.start_day_button), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                } else {
                    val dayTrips = records.transports.filter { it.dayId == day.id }
                    val dayTotal = dayTrips.sumOf { t ->
                        t.fareCents * records.participants.count { it.transportId == t.id }.coerceAtLeast(1)
                    } + records.expenses.filter { it.dayId == day.id }.sumOf { it.totalCents }
                    SectionCard {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text(
                                stringResource(R.string.day_card_open),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(day.title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(R.string.day_card_stats, dayTrips.size, Money.shown(dayTotal)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { confirmEnd = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                ),
                            ) {
                                Icon(Icons.Filled.Check, null)
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.end_day_button), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                SectionHeader(Icons.Filled.Savings, stringResource(R.string.quick_actions))
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (openDay != null) item {
                        QuickAction(stringResource(R.string.start_trip), Icons.Filled.DirectionsBus) {
                            nav.navigate(Routes.transportForm())
                        }
                    }
                    item {
                        QuickAction(stringResource(R.string.new_day), Icons.Filled.CalendarMonth) {
                            nav.navigate(Routes.DAYS)
                        }
                    }
                    if (openDay != null) item {
                        QuickAction(stringResource(R.string.new_expense), Icons.Filled.Payments) {
                            nav.navigate(Routes.expenseForm())
                        }
                    }
                    item {
                        QuickAction(stringResource(R.string.new_transfer), Icons.Filled.SwapHoriz) {
                            nav.navigate(Routes.transferForm())
                        }
                    }
                    item {
                        QuickAction(stringResource(R.string.people), Icons.Filled.Groups) {
                            nav.navigate(Routes.PEOPLE)
                        }
                    }
                    item {
                        QuickAction(stringResource(R.string.routes_title), Icons.Filled.Route) {
                            nav.navigate(Routes.ROUTES_LIST)
                        }
                    }
                }
            }
        }

        if (netBalances.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(18.dp))
                    SectionHeader(Icons.Filled.AccountBalanceWallet, stringResource(R.string.balances))
                    Spacer(Modifier.height(8.dp))
                    SectionCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            netBalances.entries.take(4).forEach { (pid, net) ->
                                RowSpaceBetween {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        dev.ahmeddawood.elghassala.ui.components.PersonAvatar(
                                            records.peopleNames[pid] ?: "?", avatars[pid], { vm.avatarPhotoFile(it) }, size = 30.dp,
                                        )
                                        Spacer(Modifier.size(10.dp))
                                        Text(records.peopleNames[pid] ?: "#$pid")
                                    }
                                    BalanceText(net)
                                }
                            }
                            if (netBalances.size > 4) {
                                TextButton(onClick = { nav.navigate(Routes.SETTLE) }) {
                                    Text(stringResource(R.string.view_all))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (openDays.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(18.dp))
                    RowSpaceBetween {
                        SectionHeader(Icons.Filled.CalendarMonth, stringResource(R.string.days_title))
                        TextButton(onClick = { nav.navigate(Routes.DAYS) }) { Text(stringResource(R.string.view_all)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(openDays.take(3), key = { "day-${it.id}" }) { day ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    PressScaleCard(onClick = { nav.navigate(Routes.dayDetail(day.id)) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(day.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    formatDate(day.dateMillis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                stringResource(R.string.day_open),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }

        val closedDays = records.days.filter { it.isClosed }.sortedWith(compareByDescending<dev.ahmeddawood.elghassala.data.db.DayEntity> { it.dateMillis }.thenByDescending { it.id })
        if (closedDays.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(18.dp))
                    RowSpaceBetween {
                        SectionHeader(Icons.Filled.History, stringResource(R.string.history_title))
                        TextButton(onClick = { nav.navigate(Routes.DAYS) }) { Text(stringResource(R.string.view_all)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(closedDays.take(5), key = { "closed-${it.id}" }) { day ->
                val trips = records.transports.filter { it.dayId == day.id }
                val total = trips.sumOf { t ->
                    t.fareCents * records.participants.count { it.transportId == t.id }.coerceAtLeast(1)
                } + records.expenses.filter { it.dayId == day.id }.sumOf { it.totalCents }
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    PressScaleCard(onClick = { nav.navigate(Routes.dayDetail(day.id)) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(day.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(
                                    "${formatDate(day.dateMillis)}  •  ${stringResource(R.string.celebration_trips, trips.size)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(Money.shown(total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(18.dp))
                SectionHeader(Icons.Filled.SwapHoriz, stringResource(R.string.recent_activity))
                Spacer(Modifier.height(8.dp))
                if (records.transports.isEmpty() && records.expenses.isEmpty() && records.transfers.isEmpty()) {
                    EmptyState(Icons.Filled.Savings, stringResource(R.string.no_activity_yet))
                }
            }
        }

        items(recent, key = { "${it.first}-${it.second}" }) { (kind, id, _) ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                when (kind) {
                    "t" -> {
                        val t = records.transports.first { it.id == id }
                        SectionCard {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.DirectionsBus, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${t.origin} → ${t.destination}", style = MaterialTheme.typography.titleMedium)
                                    Text(formatDateTime(t.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(Money.shown(t.fareCents), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    "e" -> {
                        val e = records.expenses.first { it.id == id }
                        SectionCard {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Payments, null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.title, style = MaterialTheme.typography.titleMedium)
                                    Text(formatDateTime(e.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(Money.shown(e.totalCents), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    else -> {
                        val x = records.transfers.first { it.id == id }
                        SectionCard {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.SwapHoriz, null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${records.peopleNames[x.fromPersonId] ?: ""} → ${records.peopleNames[x.toPersonId] ?: ""}",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(formatDateTime(x.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(Money.shown(x.amountCents), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    PressScaleCard(onClick = onClick) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
