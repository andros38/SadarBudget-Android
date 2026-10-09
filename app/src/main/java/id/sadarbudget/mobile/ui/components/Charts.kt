package id.sadarbudget.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import id.sadarbudget.mobile.data.AnnualMonth
import id.sadarbudget.mobile.data.ChartPoint
import id.sadarbudget.mobile.ui.theme.SbDanger
import id.sadarbudget.mobile.ui.theme.SbSuccess
import kotlin.math.max
import kotlin.math.min

@Composable
fun BalanceLineChart(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = .45f)
    val surface = MaterialTheme.colorScheme.surface
    val small = isSmallPhoneUi()
    val wide = isWideUi()
    val compact = isCompactUi()
    if (points.isEmpty()) return
    val chartHeight = if (wide) 120.dp else if (small) 110.dp else if (compact) 126.dp else 144.dp
    Canvas(modifier.fillMaxWidth().height(chartHeight)) {
        val left = 18f
        val right = size.width - 12f
        val top = 10f
        val bottom = size.height - 10f
        repeat(4) { i ->
            val y = top + (bottom - top) * i / 3f
            drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1f)
        }
        val minValue = points.minOf { it.balance }
        val maxValue = points.maxOf { it.balance }
        val span = max(1.0, maxValue - minValue)
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = if (points.size == 1) (left + right) / 2f else left + (right - left) * index / (points.size - 1f)
            val y = bottom - ((point.balance - minValue) / span).toFloat() * (bottom - top)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, line, style = Stroke(width = if (small) 3f else 3.5f))
        points.forEachIndexed { index, point ->
            val x = if (points.size == 1) (left + right) / 2f else left + (right - left) * index / (points.size - 1f)
            val y = bottom - ((point.balance - minValue) / span).toFloat() * (bottom - top)
            drawCircle(surface, radius = if (small) 5f else 5.5f, center = Offset(x, y))
            drawCircle(line, radius = if (small) 3.2f else 3.6f, center = Offset(x, y))
        }
    }
}

@Composable
fun AnnualBarChart(months: List<AnnualMonth>, modifier: Modifier = Modifier) {
    if (months.isEmpty()) return
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = .40f)
    val small = isSmallPhoneUi()
    val wide = isWideUi()
    val compact = isCompactUi()
    val chartHeight = if (wide) 108.dp else if (small) 92.dp else if (compact) 100.dp else 112.dp
    Canvas(modifier.fillMaxWidth().height(chartHeight)) {
        val left = 12f
        val right = size.width - 12f
        val top = 10f
        val bottom = size.height - 12f
        repeat(4) { i ->
            val y = top + (bottom - top) * i / 3f
            drawLine(outline, Offset(left, y), Offset(right, y), strokeWidth = 1f)
        }
        val maxValue = max(1.0, months.maxOf { max(it.income, it.expense) })
        val slot = (right - left) / months.size
        val barWidth = min(18f, slot * .30f)
        months.forEachIndexed { i, row ->
            val center = left + slot * (i + .5f)
            val incomeH = ((row.income / maxValue) * (bottom - top)).toFloat()
            val expenseH = ((row.expense / maxValue) * (bottom - top)).toFloat()
            drawRoundRect(
                color = SbSuccess,
                topLeft = Offset(center - barWidth - 2f, bottom - incomeH),
                size = Size(barWidth, incomeH),
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawRoundRect(
                color = SbDanger,
                topLeft = Offset(center + 2f, bottom - expenseH),
                size = Size(barWidth, expenseH),
                cornerRadius = CornerRadius(6f, 6f),
            )
        }
    }
}
