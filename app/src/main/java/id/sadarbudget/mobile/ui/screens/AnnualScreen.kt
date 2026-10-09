package id.sadarbudget.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.sadarbudget.mobile.data.AnnualData
import id.sadarbudget.mobile.data.AnnualRecap
import id.sadarbudget.mobile.data.AppRepository
import id.sadarbudget.mobile.ui.components.*
import id.sadarbudget.mobile.ui.theme.SbDanger
import id.sadarbudget.mobile.ui.theme.SbSuccess
import id.sadarbudget.mobile.ui.theme.SbWindowClass
import id.sadarbudget.mobile.ui.theme.sbDesignSystem
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnualScreen(repository: AppRepository) {
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var data by remember { mutableStateOf<AnnualData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var yearMenu by remember { mutableStateOf(false) }
    var monthMenu by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val wide = isWideUi()

    LaunchedEffect(selectedYear, refresh) {
        error = null
        runCatching { repository.annual(selectedYear) }
            .onSuccess {
                data = it
                if (selectedYear == null) selectedYear = it.recap.year
            }
            .onFailure { error = it.message ?: "Gagal memuat laporan tahunan." }
    }

    LaunchedEffect(data?.recap?.year) {
        val recap = data?.recap ?: return@LaunchedEffect
        selectedMonth = when {
            recap.year == LocalDate.now().year -> LocalDate.now().monthValue
            recap.months.any { it.month == selectedMonth && it.hasActivity } -> selectedMonth
            else -> recap.months.firstOrNull { it.hasActivity }?.month ?: 1
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = pageHorizontalPadding(),
            vertical = pageVerticalPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing()),
    ) {
        if (data == null && error == null) item { LoadingState() }
        if (error != null) item { ErrorState(error!!) { refresh++ } }

        data?.let { d ->
            val recap = d.recap

            item {
                AnnualHeader(
                    data = d,
                    expanded = yearMenu,
                    onExpanded = { yearMenu = it },
                    onYear = { selectedYear = it },
                )
            }

            item { AnnualSummary(recap) }

            if (wide) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(sectionSpacing()),
                        verticalAlignment = Alignment.Top,
                    ) {
                        AnnualTrendPanel(recap, Modifier.weight(1.15f))
                        MonthDetailPanel(
                            recap = recap,
                            selectedMonth = selectedMonth,
                            expanded = monthMenu,
                            onExpanded = { monthMenu = it },
                            onMonth = { selectedMonth = it },
                            modifier = Modifier.weight(.85f),
                        )
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(sectionSpacing()),
                        verticalAlignment = Alignment.Top,
                    ) {
                        AnnualHighlights(recap, Modifier.weight(.85f))
                        TopCategoriesPanel(recap, Modifier.weight(1.15f))
                    }
                }
            } else {
                item { AnnualTrendPanel(recap) }
                item {
                    MonthDetailPanel(
                        recap = recap,
                        selectedMonth = selectedMonth,
                        expanded = monthMenu,
                        onExpanded = { monthMenu = it },
                        onMonth = { selectedMonth = it },
                    )
                }
                item { AnnualHighlights(recap) }
                item { TopCategoriesPanel(recap) }
            }

            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnualHeader(
    data: AnnualData,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit,
    onYear: (Int) -> Unit,
) {
    val design = sbDesignSystem()
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(design.spacing.sm),
    ) {
        Surface(
            modifier = Modifier.size(if (design.windowClass == SbWindowClass.Mini) 38.dp else 42.dp),
            shape = RoundedCornerShape(design.radius.control),
            color = colors.surfaceVariant,
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ShowChart, null, tint = colors.primary, modifier = Modifier.size(21.dp))
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "LAPORAN TAHUNAN",
                color = colors.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Analisis keuangan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Ringkasan arus uang selama satu tahun.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }

        YearPicker(
            data = data,
            expanded = expanded,
            onExpanded = onExpanded,
            onYear = onYear,
            modifier = Modifier.width(if (design.windowClass == SbWindowClass.Mini) 90.dp else 98.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearPicker(
    data: AnnualData,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit,
    onYear: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpanded,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = data.recap.year.toString(),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
            data.availableYears.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString()) },
                    onClick = { onYear(year); onExpanded(false) },
                )
            }
        }
    }
}

@Composable
private fun AnnualSummary(recap: AnnualRecap) {
    val design = sbDesignSystem()
    val colors = MaterialTheme.colorScheme
    val saldoColor = if (recap.totals.assetNet < 0) SbDanger else colors.primary

    SbCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SALDO TAHUN", color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                AdaptiveValueText(
                    value = formatRupiah(recap.totals.assetNet),
                    color = saldoColor,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Surface(
                shape = RoundedCornerShape(design.radius.control),
                color = colors.primaryContainer,
            ) {
                Text(
                    "${recap.activeMonths} bulan aktif",
                    modifier = Modifier.padding(horizontal = design.spacing.sm, vertical = design.spacing.xs),
                    color = colors.onPrimaryContainer,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        HorizontalDivider(color = colors.outlineVariant)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(design.spacing.sm),
        ) {
            SummaryInlineMetric("Masuk", formatRupiah(recap.totals.income), SbSuccess, Modifier.weight(1f))
            SummaryInlineMetric("Keluar", formatRupiah(recap.totals.expense), SbDanger, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryInlineMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            AdaptiveValueText(value = value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AnnualTrendPanel(recap: AnnualRecap, modifier: Modifier = Modifier) {
    val design = sbDesignSystem()
    SbCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ARUS 12 BULAN", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Pemasukan vs pengeluaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(design.spacing.sm)) {
                LegendDot("Masuk", SbSuccess)
                LegendDot("Keluar", SbDanger)
            }
        }
        AnnualBarChart(recap.months)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            recap.months.forEach { month ->
                Text(month.label.take(3), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LegendDot(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthDetailPanel(
    recap: AnnualRecap,
    selectedMonth: Int,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit,
    onMonth: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val design = sbDesignSystem()
    val selected = recap.months.firstOrNull { it.month == selectedMonth } ?: recap.months.first()
    val colors = MaterialTheme.colorScheme

    SbCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("RINCIAN BULAN", color = colors.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Detail transaksi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpanded,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = selected.fullLabel,
                onValueChange = {},
                readOnly = true,
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(19.dp)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
                recap.months.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month.fullLabel) },
                        onClick = { onMonth(month.month); onExpanded(false) },
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (selected.hasActivity) "Perubahan saldo" else "Belum ada transaksi",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (selected.hasActivity) {
                    AdaptiveValueText(
                        value = formatRupiah(selected.assetNet),
                        color = if (selected.assetNet < 0) SbDanger else colors.primary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(design.spacing.sm)) {
            MonthMetric("Masuk", formatRupiah(selected.income), SbSuccess, Modifier.weight(1f))
            MonthMetric("Keluar", formatRupiah(selected.expense), SbDanger, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MonthMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val design = sbDesignSystem()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(design.radius.control),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(design.spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(3.dp))
            AdaptiveValueText(value = value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AnnualHighlights(recap: AnnualRecap, modifier: Modifier = Modifier) {
    SbCard(modifier.fillMaxWidth()) {
        Text("SOROTAN", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        HighlightRow("Bulan aktif", "${recap.activeMonths} / 12")
        HighlightRow("Bulan terbaik", recap.bestMonth?.fullLabel ?: "Belum ada")
        HighlightRow("Saldo tahun", formatRupiah(recap.totals.moneyNet))
        HighlightRow("Rata-rata masuk", formatRupiah(if (recap.activeMonths > 0) recap.totals.income / recap.activeMonths else 0.0))
    }
}

@Composable
private fun TopCategoriesPanel(recap: AnnualRecap, modifier: Modifier = Modifier) {
    SbCard(modifier.fillMaxWidth()) {
        Text("KATEGORI PENGELUARAN", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        if (recap.topCategories.isEmpty()) {
            Text("Belum ada pengeluaran pada tahun ini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val max = recap.topCategories.maxOfOrNull { it.totalAmount } ?: 1.0
            recap.topCategories.take(if (isWideUi()) 5 else 4).forEachIndexed { index, row ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                row.categoryName,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(formatRupiah(row.totalAmount), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (row.totalAmount / max).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}
