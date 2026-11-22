package dev.ahmeddawood.elghassala.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.ahmeddawood.elghassala.AppViewModel
import dev.ahmeddawood.elghassala.BuildConfig
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.settings.AppLanguage
import dev.ahmeddawood.elghassala.settings.AppTheme
import dev.ahmeddawood.elghassala.ui.components.GradientHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val language by vm.language.collectAsState()
    val theme by vm.theme.collectAsState()
    var confirmImport by remember { mutableStateOf<android.net.Uri?>(null) }
    var confirmLegacyImport by remember { mutableStateOf<android.net.Uri?>(null) }
    val legacyResult by vm.legacyImportResult.collectAsState()

    val stamp = remember { SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) }

    val exportBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.exportBackup(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        confirmImport = uri
    }
    val legacyImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        confirmLegacyImport = uri
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { vm.exportCsv(it) }
    }
    val exportPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            val now = System.currentTimeMillis()
            vm.exportPdf(it, "El Ghassala", stamp, 0L, now)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            GradientHeader(
                title = stringResource(R.string.settings_title),
                subtitle = "El Ghassala",
            )
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                SectionHeader(Icons.Filled.Settings, stringResource(R.string.language))
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = language == AppLanguage.SYSTEM,
                            onClick = { vm.setLanguage(AppLanguage.SYSTEM) },
                            label = { Text(stringResource(R.string.language_system)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = language == AppLanguage.ARABIC,
                            onClick = { vm.setLanguage(AppLanguage.ARABIC) },
                            label = { Text(stringResource(R.string.language_ar)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = language == AppLanguage.ENGLISH,
                            onClick = { vm.setLanguage(AppLanguage.ENGLISH) },
                            label = { Text(stringResource(R.string.language_en)) },
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                SectionHeader(Icons.Filled.Palette, stringResource(R.string.appearance))
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = theme == AppTheme.SYSTEM,
                            onClick = { vm.setTheme(AppTheme.SYSTEM) },
                            label = { Text(stringResource(R.string.theme_system)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = theme == AppTheme.LIGHT,
                            onClick = { vm.setTheme(AppTheme.LIGHT) },
                            label = { Text(stringResource(R.string.theme_light)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = theme == AppTheme.DARK,
                            onClick = { vm.setTheme(AppTheme.DARK) },
                            label = { Text(stringResource(R.string.theme_dark)) },
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                SectionHeader(Icons.Filled.Backup, stringResource(R.string.backup_restore))
                Spacer(Modifier.height(8.dp))
                SectionCard {
                    Column(Modifier.padding(8.dp)) {
                        TextButton(onClick = { exportBackupLauncher.launch("elghassala-backup-$stamp.json") }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.export_backup))
                        }
                        TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.import_backup))
                        }
                        TextButton(onClick = { legacyImportLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.import_legacy))
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                SectionHeader(Icons.Filled.Download, stringResource(R.string.export_data))
                Spacer(Modifier.height(8.dp))
                SectionCard {
                    Column(Modifier.padding(8.dp)) {
                        TextButton(onClick = { exportPdfLauncher.launch("elghassala-report-$stamp.pdf") }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.export_pdf))
                        }
                        TextButton(onClick = { exportCsvLauncher.launch("elghassala-export-$stamp.csv") }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.export_csv))
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(20.dp))
                SectionHeader(Icons.Filled.Upload, stringResource(R.string.about))
                Spacer(Modifier.height(8.dp))
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.about_text), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${stringResource(R.string.version)} ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.made_by),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    confirmImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.import_backup),
            text = stringResource(R.string.import_confirm),
            onConfirm = { vm.importBackup(uri) },
            onDismiss = { confirmImport = null },
        )
    }

    confirmLegacyImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.import_legacy),
            text = stringResource(R.string.import_legacy_confirm),
            onConfirm = { vm.importLegacyBackup(uri); confirmLegacyImport = null },
            onDismiss = { confirmLegacyImport = null },
        )
    }

    legacyResult?.let { r ->
        AlertDialog(
            onDismissRequest = { vm.clearLegacyImportResult() },
            confirmButton = {
                TextButton(onClick = { vm.clearLegacyImportResult() }) { Text(stringResource(R.string.legacy_ok)) }
            },
            title = { Text(stringResource(R.string.legacy_import_done)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${stringResource(R.string.legacy_days)}: ${r.daysAdded}")
                    Text("${stringResource(R.string.legacy_people_new)}: ${r.peopleAdded}")
                    Text("${stringResource(R.string.legacy_people_reused)}: ${r.peopleReused}")
                    Text("${stringResource(R.string.legacy_routes)}: ${r.routesAdded}")
                    Text("${stringResource(R.string.legacy_trips)}: ${r.transportsAdded}")
                    Text("${stringResource(R.string.legacy_expenses)}: ${r.expensesAdded}")
                    Text("${stringResource(R.string.legacy_transfers)}: ${r.transfersAdded}")
                    Text("${stringResource(R.string.legacy_refunds_collected)}: ${r.refundsMarkedCollected}")
                    if (r.warnings.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.legacy_warnings), style = MaterialTheme.typography.titleSmall)
                        r.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
        )
    }
}
