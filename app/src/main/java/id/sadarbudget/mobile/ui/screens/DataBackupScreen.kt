package id.sadarbudget.mobile.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import id.sadarbudget.mobile.data.AppRepository
import id.sadarbudget.mobile.data.BackupItem
import id.sadarbudget.mobile.data.DataStatus
import id.sadarbudget.mobile.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private enum class DataDialog { Export, Import, ExportEncrypted, ImportEncrypted, Clean }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupScreen(repository: AppRepository, onNotify: (String) -> Unit = {}) {
    var status by remember { mutableStateOf<DataStatus?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var restoreTarget by remember { mutableStateOf<BackupItem?>(null) }
    var selectedBackupFilename by remember { mutableStateOf<String?>(null) }
    var backupMenu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<DataDialog?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val saveSql = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val bytes = pendingBytes
        if (uri != null && bytes != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
                .onSuccess { message = "Berkas backup berhasil disimpan."; onNotify("Berkas backup berhasil disimpan.") }
                .onFailure { error = "Gagal menyimpan backup: ${it.message}" }
        }
        pendingBytes = null
    }

    val openSql = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importUri = uri
            dialog = DataDialog.Import
        }
    }

    val openEncrypted = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { importUri = uri; dialog = DataDialog.ImportEncrypted }
    }

    LaunchedEffect(refresh) {
        loading = true
        error = null
        runCatching { repository.dataStatus() }
            .onSuccess { status = it }
            .onFailure { error = it.message ?: "Gagal memuat status data." }
        loading = false
    }

    LaunchedEffect(status?.autoBackup?.latest?.filename) {
        val items = status?.autoBackup?.items.orEmpty()
        if (items.isNotEmpty() && items.none { it.filename == selectedBackupFilename }) {
            selectedBackupFilename = items.first().filename
        }
    }

    fun saveBackupBytes(bytes: ByteArray, filename: String) {
        pendingBytes = bytes
        saveSql.launch(filename)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(pageHorizontalPadding(), pageVerticalPadding(), pageHorizontalPadding(), 24.dp),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing()),
    ) {
        item {
            PageTitle(
                "Data dan privasi",
                "Data & Backup",
                "Ekspor data untuk disimpan di luar aplikasi, impor saat diperlukan, atau pulihkan versi sebelumnya.",
            )
        }
        if (message != null) item { MessageBanner(message!!) }
        if (error != null) item { MessageBanner(error!!, true) }
        if (loading) item { LoadingState() }

        status?.let { s ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Transaksi", s.counts.transactions.toString(), "Data akun", modifier = Modifier.weight(1f))
                    MetricCard("Kategori", s.counts.categories.toString(), "Kategori tersimpan", modifier = Modifier.weight(1f))
                }
            }

            item {
                SbCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Ekspor backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Simpan salinan data ke folder pilihan Anda sebagai berkas .sql.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.SaveAlt, null)
                    }
                    SbPrimaryButton(
                        onClick = { dialog = DataDialog.Export },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    ) {
                        Icon(Icons.Outlined.Download, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Ekspor backup SQL")
                    }
                    SbSecondaryButton(
                        onClick = { dialog = DataDialog.ExportEncrypted },
                        modifier = Modifier.fillMaxWidth(), enabled = !busy,
                    ) { Icon(Icons.Outlined.Lock, null); Spacer(Modifier.width(6.dp)); Text("Ekspor terenkripsi (.sbb)") }
                    Text("File .sbb dilindungi kata sandi khusus; jangan sampai lupa kata sandinya.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                SbCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Impor backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Gunakan berkas .sql SadarBudget untuk mengembalikan transaksi dan kategori.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.UploadFile, null)
                    }
                    SbSecondaryButton(
                        onClick = { openSql.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    ) {
                        Icon(Icons.Outlined.UploadFile, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Pilih backup SQL")
                    }
                    SbSecondaryButton(onClick = { openEncrypted.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                        Icon(Icons.Outlined.LockOpen, null); Spacer(Modifier.width(6.dp)); Text("Pilih backup terenkripsi (.sbb)")
                    }
                }
            }

            item {
                val selectedBackup = s.autoBackup.items.firstOrNull { it.filename == selectedBackupFilename }
                    ?: s.autoBackup.latest

                SbCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Pemulihan cepat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "SadarBudget menyimpan hingga ${s.autoBackup.maxFiles} versi pengaman sebelum perubahan penting.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Outlined.Restore, null)
                    }

                    if (s.autoBackup.items.isEmpty()) {
                        Text("Belum ada versi pemulihan. Versi akan dibuat otomatis sebelum perubahan yang berisiko menghapus atau mengganti data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        ExposedDropdownMenuBox(expanded = backupMenu, onExpandedChange = { backupMenu = it }) {
                            OutlinedTextField(
                                value = selectedBackup?.let { backupLabel(it) } ?: "Pilih versi",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Versi pemulihan") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(backupMenu) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                            )
                            ExposedDropdownMenu(expanded = backupMenu, onDismissRequest = { backupMenu = false }) {
                                s.autoBackup.items.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(item.createdAt ?: item.filename)
                                                Text(
                                                    "${item.reasonLabel ?: "Versi pengaman"} · ${formatFileSize(item.size)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedBackupFilename = item.filename
                                            backupMenu = false
                                        },
                                    )
                                }
                            }
                        }

                        HighlightRowData("Tersimpan", "${s.autoBackup.count} versi")
                        HighlightRowData("Total ruang", s.autoBackup.totalSizeLabel)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SbSecondaryButton(
                                onClick = {
                                    selectedBackup?.let { item ->
                                        scope.launch {
                                            busy = true
                                            runCatching { repository.downloadBackup(item.filename) }
                                                .onSuccess { bytes -> saveBackupBytes(bytes, item.filename) }
                                                .onFailure { error = it.message }
                                            busy = false
                                        }
                                    }
                                },
                                enabled = !busy && selectedBackup != null,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Download, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Unduh")
                            }
                            SbPrimaryButton(
                                onClick = { if (selectedBackup != null) restoreTarget = selectedBackup },
                                enabled = !busy && selectedBackup != null,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Restore, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Pulihkan")
                            }
                        }
                    }
                }
            }

            item {
                SbCard {
                    Text("Bersihkan data keuangan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Hapus seluruh transaksi dan kategori. Akun serta profil tidak ikut dihapus.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SbDangerButton(
                        onClick = { dialog = DataDialog.Clean },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    ) {
                        Icon(Icons.Outlined.DeleteForever, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Bersihkan semua data")
                    }
                }
            }
        }
    }

    restoreTarget?.let { item ->
        RestoreDialog(
            item = item,
            onDismiss = { restoreTarget = null },
            onRestore = { password, confirmation, ack ->
                restoreTarget = null
                scope.launch {
                    busy = true
                    error = null
                    message = null
                    runCatching { repository.restoreBackup(item.filename, password, confirmation, ack) }
                        .onSuccess { message = it; onNotify(it); refresh++ }
                        .onFailure { error = it.message }
                    busy = false
                }
            },
        )
    }

    when (dialog) {
        DataDialog.Export -> ExportDialog(onDismiss = { dialog = null }) { password ->
            dialog = null
            scope.launch {
                busy = true
                error = null
                runCatching { repository.exportSql(password) }
                    .onSuccess { bytes ->
                        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
                        saveBackupBytes(bytes, "sadarbudget-backup-$stamp.sql")
                    }
                    .onFailure { error = it.message }
                busy = false
            }
        }
        DataDialog.ExportEncrypted -> EncryptedExportDialog(onDismiss = { dialog = null }) { accountPassword, filePassword ->
            dialog = null
            scope.launch {
                busy = true; error = null
                runCatching { repository.exportEncryptedSql(accountPassword, filePassword) }
                    .onSuccess { bytes ->
                        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
                        saveBackupBytes(bytes, "sadarbudget-rahasia-$stamp.sbb")
                    }.onFailure { error = it.message }
                busy = false
            }
        }
        DataDialog.ImportEncrypted -> EncryptedImportDialog(onDismiss = { dialog = null; importUri = null }) { password, filePassword, confirmation, ack ->
            val uri = importUri
            dialog = null
            if (uri != null) scope.launch {
                busy = true; error = null; message = null
                runCatching { repository.importEncryptedSql(uri, password, filePassword, confirmation, ack) }
                    .onSuccess { message = it; onNotify(it); refresh++ }
                    .onFailure { error = it.message }
                busy = false; importUri = null
            }
        }
        DataDialog.Import -> ImportDialog(onDismiss = { dialog = null; importUri = null }) { password, confirmation, ack ->
            val uri = importUri
            dialog = null
            if (uri != null) scope.launch {
                busy = true
                error = null
                message = null
                runCatching { repository.importSql(uri, password, confirmation, ack) }
                    .onSuccess { message = it; onNotify(it); refresh++ }
                    .onFailure { error = it.message }
                busy = false
                importUri = null
            }
        }
        DataDialog.Clean -> CleanDialog(onDismiss = { dialog = null }) { password, confirmation, ack ->
            dialog = null
            scope.launch {
                busy = true
                error = null
                message = null
                runCatching { repository.cleanData(password, confirmation, ack) }
                    .onSuccess { message = it; onNotify(it); refresh++ }
                    .onFailure { error = it.message }
                busy = false
            }
        }
        null -> Unit
    }
}

@Composable
private fun HighlightRowData(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun backupLabel(item: BackupItem): String {
    val date = item.createdAt ?: "Versi pemulihan"
    return "$date · ${item.reasonLabel ?: "Pengaman"}"
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1024f / 1024f)
    bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
    else -> "$bytes B"
}

@Composable
private fun RestoreDialog(item: BackupItem, onDismiss: () -> Unit, onRestore: (String, String, Boolean) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var ack by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pulihkan versi data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(backupLabel(item), style = MaterialTheme.typography.labelMedium)
                Text("Data keuangan saat ini akan diganti dengan versi yang dipilih.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PasswordField(password, { password = it }, "Kata sandi saat ini")
                OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Ketik PULIHKAN BACKUP") }, modifier = Modifier.fillMaxWidth())
                CheckRow(ack, { ack = it }, "Saya memahami data keuangan saat ini akan diganti.")
            }
        },
        confirmButton = {
            SbPrimaryButton(
                onClick = { onRestore(password, confirmation, ack) },
                enabled = password.isNotBlank() && confirmation == "PULIHKAN BACKUP" && ack,
            ) { Text("Pulihkan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun ExportDialog(onDismiss: () -> Unit, onExport: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ekspor backup SQL") },
        text = { PasswordField(password, { password = it }, "Kata sandi saat ini") },
        confirmButton = { SbPrimaryButton(onClick = { onExport(password) }, enabled = password.isNotBlank()) { Text("Ekspor") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onImport: (String, String, Boolean) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var ack by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Impor backup SQL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Transaksi dan kategori saat ini akan diganti oleh isi backup.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PasswordField(password, { password = it }, "Kata sandi saat ini")
                OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Ketik IMPOR DATA") }, modifier = Modifier.fillMaxWidth())
                CheckRow(ack, { ack = it }, "Saya memahami transaksi dan kategori saat ini akan diganti.")
            }
        },
        confirmButton = {
            SbPrimaryButton(
                onClick = { onImport(password, confirmation, ack) },
                enabled = password.isNotBlank() && confirmation == "IMPOR DATA" && ack,
            ) { Text("Impor data") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun CleanDialog(onDismiss: () -> Unit, onClean: (String, String, Boolean) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var ack by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bersihkan data keuangan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Versi pengaman akan dibuat otomatis sebelum data dihapus.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PasswordField(password, { password = it }, "Kata sandi saat ini")
                OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Ketik BERSIHKAN DATA") }, modifier = Modifier.fillMaxWidth())
                CheckRow(ack, { ack = it }, "Saya memahami data yang belum diekspor bisa hilang jika versi pemulihan ikut terhapus.")
            }
        },
        confirmButton = {
            SbDangerButton(
                onClick = { onClean(password, confirmation, ack) },
                enabled = password.isNotBlank() && confirmation == "BERSIHKAN DATA" && ack,
            ) { Text("Bersihkan data") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun PasswordField(value: String, onValue: (String) -> Unit, label: String) {
    OutlinedTextField(
        value,
        onValue,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
    )
}

@Composable
private fun CheckRow(checked: Boolean, onChecked: (Boolean) -> Unit, text: String) {
    Row(Modifier.fillMaxWidth()) {
        Checkbox(checked, onCheckedChange = onChecked)
        Spacer(Modifier.width(6.dp))
        Text(text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EncryptedExportDialog(onDismiss: () -> Unit, onExport: (String, String) -> Unit) {
    var accountPassword by remember { mutableStateOf("") }
    var filePassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Backup terenkripsi") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Buat file .sbb dengan enkripsi AES-256-GCM. Kata sandi file tidak dapat dipulihkan jika lupa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            PasswordField(accountPassword, { accountPassword = it }, "Kata sandi akun saat ini")
            PasswordField(filePassword, { filePassword = it }, "Kata sandi file (min. 8 karakter)")
            PasswordField(confirmation, { confirmation = it }, "Ulangi kata sandi file")
        } },
        confirmButton = { SbPrimaryButton(onClick = { onExport(accountPassword, filePassword) }, enabled = accountPassword.isNotBlank() && filePassword.length >= 8 && filePassword == confirmation) { Text("Ekspor aman") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

@Composable
private fun EncryptedImportDialog(onDismiss: () -> Unit, onImport: (String, String, String, Boolean) -> Unit) {
    var accountPassword by remember { mutableStateOf("") }
    var filePassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var ack by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Impor backup terenkripsi") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Data keuangan saat ini akan diganti dengan isi backup .sbb.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            PasswordField(accountPassword, { accountPassword = it }, "Kata sandi akun saat ini")
            PasswordField(filePassword, { filePassword = it }, "Kata sandi file .sbb")
            OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Ketik IMPOR DATA") }, modifier = Modifier.fillMaxWidth())
            CheckRow(ack, { ack = it }, "Saya memahami data saat ini akan diganti.")
        } },
        confirmButton = { SbPrimaryButton(onClick = { onImport(accountPassword, filePassword, confirmation, ack) }, enabled = accountPassword.isNotBlank() && filePassword.isNotBlank() && confirmation == "IMPOR DATA" && ack) { Text("Impor aman") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}
