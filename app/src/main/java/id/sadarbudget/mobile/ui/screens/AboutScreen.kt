package id.sadarbudget.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sadarbudget.mobile.BuildConfig
import id.sadarbudget.mobile.ui.components.PageTitle
import id.sadarbudget.mobile.ui.components.SadarBudgetLogo
import id.sadarbudget.mobile.ui.components.SbCard
import id.sadarbudget.mobile.ui.components.pageHorizontalPadding
import id.sadarbudget.mobile.ui.components.pageVerticalPadding
import id.sadarbudget.mobile.ui.components.sectionSpacing
import id.sadarbudget.mobile.ui.theme.sbDesignSystem

@Composable
fun AboutScreen() {
    val design = sbDesignSystem()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = pageHorizontalPadding(),
            vertical = pageVerticalPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing()),
    ) {
        item {
            PageTitle(
                eyebrow = "Tentang",
                title = "SadarBudget",
            )
        }

        item {
            SbCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(design.radius.control),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f),
                        modifier = Modifier.size(58.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            SadarBudgetLogo(
                                contentDescription = "Logo SadarBudget",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            "SadarBudget",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Pencatatan keuangan pribadi",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                }

                HorizontalDivider()
                AboutInfoRow("Versi", BuildConfig.VERSION_NAME.substringBefore("-"))
                HorizontalDivider()
                AboutInfoRow("Dibuat oleh", "Ahmad Asyhari")
            }
        }
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}
