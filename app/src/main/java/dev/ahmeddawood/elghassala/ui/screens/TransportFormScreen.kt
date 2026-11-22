package dev.ahmeddawood.elghassala.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import dev.ahmeddawood.elghassala.UiEvent
import dev.ahmeddawood.elghassala.data.db.RouteEntity
import dev.ahmeddawood.elghassala.data.repo.TransportInput
import dev.ahmeddawood.elghassala.domain.FinanceEngine
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.ui.components.AmountField
import dev.ahmeddawood.elghassala.ui.components.DateTimeField
import dev.ahmeddawood.elghassala.ui.components.FormSectionTitle
import dev.ahmeddawood.elghassala.ui.components.PersonMultiSelect
import dev.ahmeddawood.elghassala.ui.components.PressScaleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportFormScreen(vm: AppViewModel, nav: NavHostController, dayId: Long?, editId: Long?) {
    val records by vm.records.collectAsState()
    val people = records.activePeople

    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var fare by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var paidAmounts by remember { mutableStateOf(mapOf<Long, String>()) }
    var pickedRoute by remember { mutableStateOf<RouteEntity?>(null) }
    var loaded by remember { mutableStateOf(editId == null) }
    var pendingRefunds by remember { mutableStateOf<List<dev.ahmeddawood.elghassala.data.db.DriverRefundEntity>?>(null) }
    var saveAsRoute by remember { mutableStateOf(false) }

    LaunchedEffect(editId, records.transports) {
        if (!loaded && editId != null) {
            val t = records.transports.firstOrNull { it.id == editId } ?: return@LaunchedEffect
            origin = t.origin
            destination = t.destination
            fare = Money.format(t.fareCents)
            notes = t.notes
            dateMillis = t.dateMillis
            val parts = vm.transportParticipants(editId)
            selected = parts.map { it.personId }.toSet()
            paidAmounts = parts.associate { it.personId to Money.format(it.paidCents) }
            loaded = true
        }
    }

    // Refund confirmation dialog after save.
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            if (event is UiEvent.RefundPrompt) pendingRefunds = event.refunds
        }
    }

    val fareCents = Money.parse(fare) ?: 0L
    val riderCount = selected.size
    val fullRide = fareCents * riderCount

    fun paidMap(): Map<Long, Long> = selected.associateWith { pid -> Money.parse(paidAmounts[pid]) ?: 0L }

    val totalPaid = paidMap().values.sum()
    val excess = totalPaid - fullRide
    val saveEnabled = origin.isNotBlank() && destination.isNotBlank() && fareCents > 0 && selected.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (editId != null) R.string.edit_transport else R.string.new_transport)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (records.routes.isNotEmpty()) {
                FormSectionTitle(stringResource(R.string.pick_route))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(records.routes.size) { i ->
                        val route = records.routes[i]
                        androidx.compose.material3.FilterChip(
                            selected = pickedRoute?.id == route.id,
                            onClick = {
                                pickedRoute = route
                                origin = route.origin
                                destination = route.destination
                                fare = Money.format(route.defaultFareCents)
                            },
                            label = { Text(route.name, maxLines = 1) },
                            leadingIcon = { Icon(Icons.Filled.Route, null) },
                        )
                    }
                }
            }

            OutlinedTextField(origin, { origin = it }, label = { Text(stringResource(R.string.origin)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(destination, { destination = it }, label = { Text(stringResource(R.string.destination)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            AmountField(fare, { fare = it }, stringResource(R.string.fare_per_person), isError = fare.isNotBlank() && fareCents <= 0)
            DateTimeField(dateMillis, { dateMillis = it }, stringResource(R.string.date))
            OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth())

            FormSectionTitle(stringResource(R.string.participants))
            val rides = remember(records.transports, records.participants) { records.rides() }
            val stars = remember(rides, origin, destination) {
                dev.ahmeddawood.elghassala.domain.RouteStars.stars(rides, origin, destination)
            }
            if (stars.isNotEmpty()) {
                val names = people.filter { it.id in stars }.joinToString(", ") { it.name }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = dev.ahmeddawood.elghassala.ui.theme.Amber)
                    Text(
                        " " + stringResource(R.string.route_stars_line, names),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (!stars.all { it in selected }) {
                        androidx.compose.material3.TextButton(onClick = {
                            val add = stars.filter { it !in selected && people.any { p -> p.id == it } }
                            selected = selected + add
                            paidAmounts = paidAmounts + add.associateWith { Money.format(fareCents) }
                        }) { Text(stringResource(R.string.route_stars_pick)) }
                    }
                }
            }
            PersonMultiSelect(people, selected, stars) { pid ->
                selected = if (pid in selected) selected - pid else selected + pid
                paidAmounts = if (pid in selected) {
                    paidAmounts - pid
                } else {
                    paidAmounts + (pid to Money.format(fareCents))
                }
            }

            if (selected.isNotEmpty()) {
                FormSectionTitle(stringResource(R.string.paid_by_each))
                SectionCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selected.forEach { pid ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    records.peopleNames[pid] ?: "#$pid",
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                )
                                OutlinedTextField(
                                    value = paidAmounts[pid] ?: "",
                                    onValueChange = { v -> paidAmounts = paidAmounts + (pid to v) },
                                    modifier = Modifier.padding(start = 8.dp).fillMaxWidth(0.45f),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                    ),
                                    label = { Text(stringResource(R.string.pays)) },
                                )
                            }
                        }
                        if (riderCount > 1) {
                            TextButton(onClick = {
                                val first = selected.first()
                                paidAmounts = selected.associateWith { if (it == first) Money.format(fullRide) else "0" }
                            }) {
                                Text(stringResource(R.string.who_paid))
                            }
                        }
                        RowSpaceBetween {
                            Text(stringResource(R.string.expected_total))
                            Text(Money.shown(fullRide))
                        }
                        RowSpaceBetween {
                            Text(stringResource(R.string.actual_paid))
                            Text(Money.shown(totalPaid))
                        }
                        if (excess > 0) {
                            val computation = FinanceEngine.computeTransport(fareCents, paidMap())
                            computation.driverRefunds.forEach { (pid, amount) ->
                                Text(
                                    "${stringResource(R.string.driver_owes)} ${records.peopleNames[pid] ?: ""}: ${Money.shown(amount)}",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        } else if (excess < 0) {
                            Text(
                                "${stringResource(R.string.owed_to_driver)}: ${Money.shown(-excess)}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            if (pickedRoute == null && origin.isNotBlank() && destination.isNotBlank() && fareCents > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saveAsRoute, onCheckedChange = { saveAsRoute = it })
                    Text(stringResource(R.string.save_route_prompt))
                }
            }

            PressScaleButton(
                onClick = {
                    val input = TransportInput(
                        dayId = dayId,
                        origin = origin,
                        destination = destination,
                        fareCents = fareCents,
                        paid = paidMap(),
                        dateMillis = dateMillis,
                        notes = notes,
                        routeId = pickedRoute?.id,
                    )
                    vm.saveTransport(editId, input)
                    pickedRoute?.let { vm.markRouteUsed(it.id) }
                    if (saveAsRoute) {
                        vm.saveRoute(null, "$origin - $destination", origin, destination, fareCents)
                    }
                    nav.popBackStack()
                },
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    pendingRefunds?.let { refunds ->
        AlertDialog(
            onDismissRequest = { pendingRefunds = null },
            title = { Text(stringResource(R.string.driver_refund_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.driver_refund_question))
                    Spacer(Modifier.height(8.dp))
                    refunds.forEach { r ->
                        Text("${records.peopleNames[r.personId] ?: ""}: ${Money.shown(r.amountCents)}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    refunds.forEach { vm.setRefundCollected(it.id, true) }
                    pendingRefunds = null
                }) { Text(stringResource(R.string.refund_received_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRefunds = null }) { Text(stringResource(R.string.refund_received_no)) }
            },
        )
    }
}
