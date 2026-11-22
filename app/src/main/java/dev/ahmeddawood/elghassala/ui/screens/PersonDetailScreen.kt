package dev.ahmeddawood.elghassala.ui.screens

import dev.ahmeddawood.elghassala.ui.arrow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
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
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.ui.components.BalanceText
import dev.ahmeddawood.elghassala.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(vm: AppViewModel, nav: NavHostController, personId: Long) {
    val records by vm.records.collectAsState()
    val ledgers by vm.ledgers.collectAsState()
    val person = records.people.firstOrNull { it.id == personId }
    var showRename by remember { mutableStateOf(false) }

    if (person == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.nothing_here_yet))
        }
        return
    }

    val avatars by vm.avatars.collectAsState()
    var showAvatarPicker by remember { mutableStateOf(false) }
    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.setAvatarPhoto(personId, it) } }
    if (showAvatarPicker) {
        AvatarPickerDialog(
            onPhoto = { showAvatarPicker = false; photoPicker.launch("image/*") },
            onEmoji = { vm.setAvatarEmoji(personId, it); showAvatarPicker = false },
            onClear = { vm.clearAvatar(personId); showAvatarPicker = false },
            onDismiss = { showAvatarPicker = false },
        )
    }

    val ledger = ledgers[personId]
    val myTransports = remember(records, personId) {
        records.participants.filter { it.personId == personId }
            .mapNotNull { p -> records.transportsById[p.transportId]?.let { p to it } }
            .sortedByDescending { it.second.dateMillis }
    }
    val myExpenses = remember(records, personId) {
        val ids = records.payers.filter { it.personId == personId }.map { it.expenseId }.toSet() +
            records.beneficiaries.filter { it.personId == personId }.map { it.expenseId }.toSet()
        records.expenses.filter { it.id in ids }
    }
    val myTransfers = remember(records, personId) { records.transfers.filter { it.fromPersonId == personId || it.toPersonId == personId } }
    val myRefunds = remember(records, personId) { records.pendingRefunds.filter { it.personId == personId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person.name) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = { showRename = true }) { Text(stringResource(R.string.edit)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        dev.ahmeddawood.elghassala.ui.components.PersonAvatar(
                            person.name, avatars[personId], { vm.avatarPhotoFile(it) }, size = 72.dp,
                            modifier = Modifier.clickable { showAvatarPicker = true },
                        )
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(person.name, style = MaterialTheme.typography.titleLarge)
                            TextButton(onClick = { showAvatarPicker = true }, contentPadding = PaddingValues(0.dp)) {
                                Text(stringResource(R.string.avatar_change))
                            }
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RowSpaceBetween {
                            Text(stringResource(R.string.current_balance), style = MaterialTheme.typography.titleMedium)
                            BalanceText(ledger?.netBalanceCents ?: 0, fontSize = 20)
                        }
                        Text(
                            stringResource(if ((ledger?.netBalanceCents ?: 0) >= 0) R.string.you_are_owed else R.string.you_owe),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (myRefunds.isNotEmpty()) {
                item {
                    SectionCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.outstanding_refunds), style = MaterialTheme.typography.titleMedium)
                            myRefunds.forEach { r ->
                                RowSpaceBetween {
                                    Text(formatDate(r.createdAt), style = MaterialTheme.typography.bodyMedium)
                                    Text(Money.shown(r.amountCents), color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }
                }
            }

            item { Text(stringResource(R.string.person_history), style = MaterialTheme.typography.titleMedium) }

            if (myTransports.isEmpty() && myExpenses.isEmpty() && myTransfers.isEmpty()) {
                item { EmptyState(Icons.Filled.SwapHoriz, stringResource(R.string.no_activity_yet)) }
            }

            items(myTransports.take(15), key = { "t-${it.second.id}" }, contentType = { "PersonDetailScreen-row" }) { (part, t) ->
                SectionCard {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DirectionsBus, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${t.origin} ${arrow()} ${t.destination}", style = MaterialTheme.typography.bodyLarge)
                            Text(formatDate(t.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "${stringResource(R.string.pays)} ${Money.shownPlain(part.paidCents)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            items(myExpenses.take(15), key = { "e-${it.id}" }, contentType = { "PersonDetailScreen-row" }) { e ->
                SectionCard {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Payments, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, style = MaterialTheme.typography.bodyLarge)
                            Text(formatDate(e.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Money.shown(e.totalCents), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(myTransfers.take(15), key = { "x-${it.id}" }, contentType = { "PersonDetailScreen-row" }) { x ->
                SectionCard {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SwapHoriz, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${records.peopleNames[x.fromPersonId] ?: ""} ${arrow()} ${records.peopleNames[x.toPersonId] ?: ""}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(formatDate(x.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Money.shown(x.amountCents), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showRename) {
        var name by remember { mutableStateOf(person.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text(stringResource(R.string.edit_person)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.person_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.savePerson(person.id, name)
                        showRename = false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}


private val emojiChoices = listOf(
    "😀","😎","🤓","🥳","😴","🤠","🦁","🐯","🦊","🐼","🐸","🦄",
    "🐧","🦉","🐙","🐢","🚗","🚌","🚀","⚽","🎧","🍕","☕","🌟",
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AvatarPickerDialog(onPhoto: () -> Unit, onEmoji: (String) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.avatar_change)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.FilledTonalButton(onClick = onPhoto, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.avatar_pick_photo))
                }
                Text(stringResource(R.string.avatar_or_emoji), style = MaterialTheme.typography.bodyMedium)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    emojiChoices.forEach { e ->
                        Text(
                            e, fontSize = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp),
                            modifier = Modifier.clickable { onEmoji(e) }.padding(4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClear) { Text(stringResource(R.string.avatar_remove)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
