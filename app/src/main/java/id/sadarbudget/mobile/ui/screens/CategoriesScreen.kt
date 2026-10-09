package id.sadarbudget.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.sadarbudget.mobile.data.AppRepository
import id.sadarbudget.mobile.data.Category
import id.sadarbudget.mobile.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(repository: AppRepository, onNotify: (String) -> Unit = {}) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") }
    var typeMenu by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf<Category?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refresh) {
        loading = true; error = null
        runCatching { repository.categories(activeOnly = true) }
            .onSuccess { categories = it }
            .onFailure { error = it.message ?: "Gagal memuat kategori." }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(pageHorizontalPadding(), pageVerticalPadding(), pageHorizontalPadding(), 24.dp), verticalArrangement = Arrangement.spacedBy(sectionSpacing())) {
        item { PageTitle("Pengaturan transaksi", "Kategori Transaksi", "Kelompokkan pemasukan dan pengeluaran agar riwayat serta laporan lebih mudah dianalisis.") }
        if (message != null) item { MessageBanner(message!!, message!!.startsWith("Gagal")) }
        item {
            SbCard {
                Text("Tambah kategori", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(name, { if (it.length <= 100) name = it }, label = { Text("Nama kategori") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { typeMenu = it }) {
                    OutlinedTextField(value = typeLabel(type), onValueChange = {}, readOnly = true, label = { Text("Jenis") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        DropdownMenuItem(text = { Text("Pemasukan") }, onClick = { type = "income"; typeMenu = false })
                        DropdownMenuItem(text = { Text("Pengeluaran") }, onClick = { type = "expense"; typeMenu = false })
                    }
                }
                SbPrimaryButton(onClick = {
                    scope.launch {
                        runCatching { repository.createCategory(name.trim(), type) }
                            .onSuccess { message = it; onNotify(it); name = ""; refresh++ }
                            .onFailure { message = "Gagal: ${it.message}" }
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = name.trim().length >= 2) { Text("Tambah kategori") }
            }
        }
        if (loading) item { LoadingState() }
        if (error != null) item { ErrorState(error!!) { refresh++ } }
        listOf("income" to "Kategori pemasukan", "expense" to "Kategori pengeluaran").forEach { (groupType, title) ->
            item { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            val group = categories.filter { it.type == groupType }
            if (group.isEmpty()) item { SbCard { Text("Belum ada kategori aktif.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            items(group, key = { it.id }) { category ->
                SbCard {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(category.name, fontWeight = FontWeight.SemiBold)
                            Text("${category.transactionCount} transaksi terkait", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { target = category }) { Icon(Icons.Outlined.DeleteOutline, "Nonaktifkan", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    target?.let { category ->
        AlertDialog(
            onDismissRequest = { target = null },
            title = { Text("Nonaktifkan kategori?") },
            text = { Text("Kategori “${category.name}” tidak lagi muncul pada transaksi baru. Nama pada transaksi lama tetap dipertahankan.") },
            confirmButton = { TextButton(onClick = { target = null; scope.launch { runCatching { repository.deactivateCategory(category.id) }.onSuccess { message = it; onNotify(it); refresh++ }.onFailure { message = "Gagal: ${it.message}" } } }) { Text("Nonaktifkan") } },
            dismissButton = { TextButton(onClick = { target = null }) { Text("Batal") } },
        )
    }
}
