package id.sadarbudget.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sadarbudget.mobile.R
import id.sadarbudget.mobile.data.Transaction
import id.sadarbudget.mobile.ui.theme.SbDanger
import id.sadarbudget.mobile.ui.theme.SbSuccess
import id.sadarbudget.mobile.ui.theme.SbWindowClass
import id.sadarbudget.mobile.ui.theme.sbDesignSystem
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SadarBudgetLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Logo SadarBudget",
) {
    Image(
        painter = painterResource(R.drawable.sadarbudget_logo_color),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

private val idLocale = Locale("id", "ID")
private val rupiahFormatter = NumberFormat.getCurrencyInstance(idLocale).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}

fun formatRupiah(value: Double): String = rupiahFormatter.format(value).replace("Rp", "Rp ").replace("  ", " ")

fun formatDateId(raw: String): String = runCatching {
    LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd MMM yyyy", idLocale))
}.getOrDefault(raw)

fun typeLabel(type: String) = if (type == "income") "Pemasukan" else "Pengeluaran"

typealias UiSizeBucket = SbWindowClass

@Composable
fun uiSizeBucket(): UiSizeBucket = sbDesignSystem().windowClass

@Composable
fun isWideUi(): Boolean {
    val c = LocalConfiguration.current
    return c.screenWidthDp >= 840 || (c.screenWidthDp >= 600 && c.screenWidthDp > c.screenHeightDp)
}

@Composable
fun isSmallPhoneUi(): Boolean = uiSizeBucket() == UiSizeBucket.Mini

@Composable
fun isCompactUi(): Boolean {
    val c = LocalConfiguration.current
    return isSmallPhoneUi() || c.screenHeightDp < 720 || isWideUi()
}

@Composable fun pageHorizontalPadding(): Dp = sbDesignSystem().spacing.pageHorizontal
@Composable fun pageVerticalPadding(): Dp = sbDesignSystem().spacing.pageVertical
@Composable fun sectionSpacing(): Dp = sbDesignSystem().spacing.section
@Composable fun cardPadding(): Dp = sbDesignSystem().spacing.cardPadding
@Composable fun cardRadius(): Dp = sbDesignSystem().radius.card
@Composable fun controlHeight(): Dp = sbDesignSystem().controlHeight

@Composable
fun PageTitle(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val bucket = uiSizeBucket()
    val titleSize = when (bucket) {
        UiSizeBucket.Mini -> 26.sp
        UiSizeBucket.Medium -> 30.sp
        UiSizeBucket.Large -> 32.sp
        UiSizeBucket.Expanded -> 34.sp
    }
    val subtitleSize = when (bucket) {
        UiSizeBucket.Mini -> 12.sp
        UiSizeBucket.Medium -> 13.sp
        UiSizeBucket.Large -> 14.sp
        UiSizeBucket.Expanded -> 15.sp
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            eyebrow.uppercase(),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                fontSize = titleSize,
                lineHeight = titleSize * 1.15f,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
                maxLines = if (bucket == UiSizeBucket.Mini || bucket == UiSizeBucket.Medium) 2 else 1,
                overflow = TextOverflow.Clip,
            )
            Row(content = actions)
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = subtitleSize,
                lineHeight = subtitleSize * 1.45f,
                maxLines = if (isWideUi()) 2 else 3,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
fun SbCard(
    modifier: Modifier = Modifier.fillMaxWidth(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val design = sbDesignSystem()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(design.radius.card),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(design.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(if (isSmallPhoneUi()) design.spacing.xs else design.spacing.sm),
            content = content,
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    meta: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val design = sbDesignSystem()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(design.radius.card),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(design.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(if (isSmallPhoneUi()) design.spacing.xs else design.spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                Text(label, color = colors.onSurfaceVariant, fontSize = if (isSmallPhoneUi()) 10.sp else 12.sp, maxLines = 2, overflow = TextOverflow.Clip)
            }
            AdaptiveValueText(
                value = value,
                fontWeight = FontWeight.ExtraBold,
            )
            if (meta.isNotBlank()) {
                Text(meta, color = colors.onSurfaceVariant, fontSize = if (isSmallPhoneUi()) 9.sp else 11.sp, maxLines = 2, overflow = TextOverflow.Clip)
            }
        }
    }
}

@Composable
fun AdaptiveValueText(
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Bold,
    textAlign: TextAlign = TextAlign.Start,
) {
    val bucket = uiSizeBucket()
    val longValue = value.length >= 13
    val veryLongValue = value.length >= 17
    val fontSize = when (bucket) {
        UiSizeBucket.Mini -> when { veryLongValue -> 13.sp; longValue -> 15.sp; else -> 18.sp }
        UiSizeBucket.Medium -> when { veryLongValue -> 14.sp; longValue -> 16.sp; else -> 20.sp }
        UiSizeBucket.Large -> when { veryLongValue -> 15.sp; longValue -> 17.sp; else -> 20.sp }
        UiSizeBucket.Expanded -> when { veryLongValue -> 16.sp; longValue -> 18.sp; else -> 22.sp }
    }
    Text(
        text = value,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize * 1.2f,
        fontWeight = fontWeight,
        maxLines = 2,
        overflow = TextOverflow.Clip,
        softWrap = true,
        textAlign = textAlign,
    )
}

@Composable
fun TransactionItem(transaction: Transaction, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    val small = isSmallPhoneUi()
    val compact = isCompactUi()
    val income = transaction.type == "income"
    Row(
        modifier.fillMaxWidth().padding(vertical = if (small) 2.dp else if (compact) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(if (small) 32.dp else if (compact) 36.dp else 42.dp)
                .clip(RoundedCornerShape(if (small) 10.dp else 12.dp))
                .background(if (income) SbSuccess.copy(alpha = .12f) else SbDanger.copy(alpha = .10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (income) "+" else "−", color = if (income) SbSuccess else SbDanger, fontWeight = FontWeight.Bold, fontSize = if (small) 14.sp else if (compact) 16.sp else 18.sp)
        }
        Spacer(Modifier.width(if (small) 9.dp else 12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(transaction.categoryName, fontWeight = FontWeight.SemiBold, fontSize = if (small) 12.sp else if (compact) 13.sp else 14.sp, maxLines = 2, overflow = TextOverflow.Clip)
            Text(
                listOfNotNull(formatDateId(transaction.transactionDate), transaction.description?.takeIf { it.isNotBlank() }).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (small) 9.sp else if (compact) 10.sp else 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = if (small) 112.dp else 136.dp)) {
            val amountText = (if (income) "+" else "−") + formatRupiah(transaction.amount)
            Text(
                amountText,
                color = if (income) SbSuccess else SbDanger,
                fontWeight = FontWeight.Bold,
                fontSize = when {
                    amountText.length >= 18 -> if (small) 9.sp else 10.sp
                    amountText.length >= 14 -> if (small) 10.sp else 11.sp
                    else -> if (small) 11.sp else if (compact) 12.sp else 13.sp
                },
                maxLines = 2,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.End,
            )
            if (trailing != null) trailing()
        }
    }
}

@Composable
fun LoadingState(text: String = "Memuat data…") {
    val small = isSmallPhoneUi()
    val compact = isCompactUi()
    Box(Modifier.fillMaxWidth().padding(if (small) 14.dp else if (compact) 18.dp else 28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(if (small) 26.dp else if (compact) 30.dp else 36.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null) {
    SbCard {
        Text(message, color = MaterialTheme.colorScheme.error)
        if (onRetry != null) {
            SbSecondaryButton(onClick = onRetry, modifier = Modifier.heightIn(min = controlHeight())) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Coba lagi")
            }
        }
    }
}

@Composable
fun MessageBanner(message: String, error: Boolean = false) {
    Surface(
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(if (isSmallPhoneUi()) 10.dp else 12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = if (isSmallPhoneUi()) 10.dp else 12.dp, vertical = if (isSmallPhoneUi()) 7.dp else 9.dp),
            color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = if (isSmallPhoneUi()) 11.sp else 12.sp,
        )
    }
}


@Composable
fun SbPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val design = sbDesignSystem()
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = design.controlHeight),
        enabled = enabled,
        shape = RoundedCornerShape(design.radius.control),
        contentPadding = PaddingValues(horizontal = design.spacing.md, vertical = design.spacing.xs),
        content = content,
    )
}

@Composable
fun SbSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val design = sbDesignSystem()
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = design.controlHeight),
        enabled = enabled,
        shape = RoundedCornerShape(design.radius.control),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .75f)),
        contentPadding = contentPadding ?: PaddingValues(horizontal = design.spacing.md, vertical = design.spacing.xs),
        content = content,
    )
}

@Composable
fun SbTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val design = sbDesignSystem()
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = design.controlHeight),
        enabled = enabled,
        shape = RoundedCornerShape(design.radius.control),
        contentPadding = PaddingValues(horizontal = design.spacing.md, vertical = design.spacing.xs),
        content = content,
    )
}

@Composable
fun SbFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val design = sbDesignSystem()
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.heightIn(min = design.controlHeight - 6.dp),
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(design.radius.pill),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

@Composable
fun SbDangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val design = sbDesignSystem()
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = design.controlHeight),
        enabled = enabled,
        shape = RoundedCornerShape(design.radius.control),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        contentPadding = PaddingValues(horizontal = design.spacing.md, vertical = design.spacing.xs),
        content = content,
    )
}
