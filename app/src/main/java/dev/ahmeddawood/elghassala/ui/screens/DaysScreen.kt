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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.ahmeddawood.elghassala.AppViewModel
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.ui.components.DateTimeField
import dev.ahmeddawood.elghassala.ui.components.EmptyState
import dev.ahmeddawood.elghassala.ui.components.GradientHeader
import dev.ahmeddawood.elghassala.ui.components.PressScaleCard
import dev.ahmeddawood.elghassala.ui.nav.Routes

@Composable
fun DaysScreen(vm: AppViewModel, nav: NavHostController) {
    val records by vm.records.collectAsState()
    var showNewDay by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewDay = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.new_day)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                GradientHeader(
                    title = stringResource(R.string.days_title),
                    subtitle = stringResource(R.string.day_summary),
                )
            }
            if (records.days.isEmpty()) {
                item { EmptyState(Icons.Filled.CalendarMonth, stringResource(R.string.no_days_yet)) }
            }
            items(records.days, key = { it.id }) { day ->
                val totals = remember(records, day.id) {
                    val ts = records.transports.filter { it.dayId == day.id }
                        .sumOf { t -> t.fareCents * records.participants.count { it.transportId == t.id }.coerceAtLeast(1) }
                    val es = records.expenses.filter { it.dayId == day.id }.sumOf { it.totalCents }
                    ts + es
                }
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    PressScaleCard(onClick = { nav.navigate(Routes.dayDetail(day.id)) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(day.title, style = MaterialTheme.typography.titleMedium)
                                    if (day.isClosed) {
                                        Spacer(Modifier.padding(4.dp))
                                        Icon(
                                            Icons.Filled.Lock, null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.height(16.dp),
                                        )
                                    }
                                }
                                Text(
                                    formatDate(day.dateMillis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                dev.ahmeddawood.elghassala.domain.Money.formatWithCurrency(totals),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewDay) {
        var title by remember { mutableStateOf("") }
        var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
        AlertDialog(
            onDismissRequest = { showNewDay = false },
            title = { Text(stringResource(R.string.new_day)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.day_title_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    DateTimeField(dateMillis, { dateMillis = it }, stringResource(R.string.day_date))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            vm.saveDay(null, title, dateMillis)
                            showNewDay = false
                        }
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showNewDay = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
