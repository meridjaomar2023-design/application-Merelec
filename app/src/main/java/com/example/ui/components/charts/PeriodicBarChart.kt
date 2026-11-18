package com.example.ui.components.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PeriodBucket
import com.example.ui.theme.ClientColor
import com.example.ui.theme.ReglementColor
import com.example.util.CurrencyUtils
import kotlin.math.max

@Composable
fun PeriodicBarChart(
    buckets: List<PeriodBucket>,
    selectedBucket: PeriodBucket?,
    onBucketSelected: (PeriodBucket?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (buckets.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aucune donnée disponible pour cette période",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Animation progress for bar growth
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(buckets) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val maxVal = remember(buckets) {
        val highest = buckets.maxOfOrNull { max(it.caTotal, it.reglementsTotal) } ?: 0.0
        if (highest <= 0.0) 10000.0 else highest * 1.15 // 15% headroom
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = onSurfaceVariant.copy(alpha = 0.15f)
    val textPaintColor = android.graphics.Color.argb(
        (0.75f * 255).toInt(),
        (onSurfaceVariant.red * 255).toInt(),
        (onSurfaceVariant.green * 255).toInt(),
        (onSurfaceVariant.blue * 255).toInt()
    )

    val density = LocalDensity.current
    val labelTextSizePx = with(density) { 10.sp.toPx() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("periodic_bar_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title and legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comparatif CA vs Règlements",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Compact Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(ClientColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ClientColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(ReglementColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Règlem.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ReglementColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Touchez une colonne pour afficher le détail chiffré",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant.copy(alpha = 0.8f)
            )

            // Selected bucket detail card (Tooltip banner)
            AnimatedVisibility(visible = selectedBucket != null) {
                selectedBucket?.let { bucket ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(
                                    text = bucket.label,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${bucket.countBlClient} bon(s) client • ${bucket.countReglements} règlement(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "CA: ${CurrencyUtils.formatDaSmart(bucket.caTotal)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ClientColor
                                )
                                Text(
                                    text = "Règlem: ${CurrencyUtils.formatDaSmart(bucket.reglementsTotal)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ReglementColor
                                )
                                Text(
                                    text = "Recouvrement: ${String.format("%.0f%%", bucket.tauxRecouvrement)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (bucket.tauxRecouvrement >= 80f) ReglementColor else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                val bucketCount = buckets.size

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .pointerInput(buckets) {
                            detectTapGestures { offset ->
                                val leftMargin = with(density) { 46.dp.toPx() }
                                val rightMargin = with(density) { 10.dp.toPx() }
                                val bottomMargin = with(density) { 26.dp.toPx() }
                                val chartWidth = size.width - leftMargin - rightMargin
                                val chartHeight = size.height - bottomMargin

                                if (offset.x >= leftMargin && offset.x <= size.width - rightMargin && offset.y <= chartHeight) {
                                    val bucketWidth = chartWidth / bucketCount
                                    val index = ((offset.x - leftMargin) / bucketWidth).toInt().coerceIn(0, bucketCount - 1)
                                    val tapped = buckets[index]
                                    if (selectedBucket?.key == tapped.key) {
                                        onBucketSelected(null)
                                    } else {
                                        onBucketSelected(tapped)
                                    }
                                } else {
                                    onBucketSelected(null)
                                }
                            }
                        }
                ) {
                    val leftMargin = 46.dp.toPx()
                    val rightMargin = 10.dp.toPx()
                    val topMargin = 16.dp.toPx()
                    val bottomMargin = 26.dp.toPx()

                    val chartWidth = size.width - leftMargin - rightMargin
                    val chartHeight = size.height - topMargin - bottomMargin
                    val zeroY = topMargin + chartHeight

                    val paint = android.graphics.Paint().apply {
                        color = textPaintColor
                        textSize = labelTextSizePx
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }

                    // 1. Draw 4 horizontal grid lines and Y-axis labels
                    val gridSteps = 4
                    for (step in 0..gridSteps) {
                        val fraction = step.toFloat() / gridSteps
                        val y = zeroY - fraction * chartHeight
                        val valueAtY = fraction * maxVal

                        // Grid line
                        drawLine(
                            color = gridLineColor,
                            start = Offset(leftMargin, y),
                            end = Offset(size.width - rightMargin, y),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Y-axis label text
                        val labelText = CurrencyUtils.formatDaCompact(valueAtY)
                        drawContext.canvas.nativeCanvas.drawText(
                            labelText,
                            leftMargin - 6.dp.toPx(),
                            y + 3.dp.toPx(),
                            paint
                        )
                    }

                    // 2. Draw Grouped Bars for each bucket
                    val slotWidth = chartWidth / bucketCount
                    val barSpacing = slotWidth * 0.08f
                    val groupPadding = slotWidth * 0.16f
                    val barWidth = ((slotWidth - 2 * groupPadding - barSpacing) / 2f).coerceAtLeast(4.dp.toPx())

                    val anim = animationProgress.value

                    buckets.forEachIndexed { index, bucket ->
                        val slotLeft = leftMargin + index * slotWidth
                        val isSelected = selectedBucket?.key == bucket.key

                        // Highlight background pill if selected
                        if (isSelected) {
                            drawRoundRect(
                                color = ClientColor.copy(alpha = 0.08f),
                                topLeft = Offset(slotLeft + 2.dp.toPx(), topMargin - 4.dp.toPx()),
                                size = Size(slotWidth - 4.dp.toPx(), chartHeight + bottomMargin + 2.dp.toPx()),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }

                        // Coordinates for CA bar
                        val caFraction = ((bucket.caTotal / maxVal) * anim).toFloat().coerceIn(0f, 1f)
                        val caHeight = (chartHeight * caFraction).coerceAtLeast(if (bucket.caTotal > 0) 3.dp.toPx() else 0f)
                        val caX = slotLeft + groupPadding
                        val caY = zeroY - caHeight

                        // Coordinates for Reglements bar
                        val regFraction = ((bucket.reglementsTotal / maxVal) * anim).toFloat().coerceIn(0f, 1f)
                        val regHeight = (chartHeight * regFraction).coerceAtLeast(if (bucket.reglementsTotal > 0) 3.dp.toPx() else 0f)
                        val regX = caX + barWidth + barSpacing
                        val regY = zeroY - regHeight

                        val barRadius = 3.dp.toPx()

                        // Draw CA bar
                        if (caHeight > 0f) {
                            val caPath = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        rect = Rect(caX, caY, caX + barWidth, zeroY),
                                        topLeft = CornerRadius(barRadius, barRadius),
                                        topRight = CornerRadius(barRadius, barRadius),
                                        bottomLeft = CornerRadius(0f, 0f),
                                        bottomRight = CornerRadius(0f, 0f)
                                    )
                                )
                            }
                            drawPath(caPath, color = ClientColor)
                        }

                        // Draw Reglement bar
                        if (regHeight > 0f) {
                            val regPath = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        rect = Rect(regX, regY, regX + barWidth, zeroY),
                                        topLeft = CornerRadius(barRadius, barRadius),
                                        topRight = CornerRadius(barRadius, barRadius),
                                        bottomLeft = CornerRadius(0f, 0f),
                                        bottomRight = CornerRadius(0f, 0f)
                                    )
                                )
                            }
                            drawPath(regPath, color = ReglementColor)
                        }

                        // Draw X-axis label centered under the group
                        val centerX = slotLeft + slotWidth / 2f
                        val centerPaint = android.graphics.Paint().apply {
                            color = if (isSelected) {
                                (ClientColor.value.toLong() and 0xFFFFFFFFL).toInt()
                            } else {
                                textPaintColor
                            }
                            textSize = labelTextSizePx
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = isSelected
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            bucket.shortLabel,
                            centerX,
                            zeroY + 16.dp.toPx(),
                            centerPaint
                        )
                    }
                }
            }
        }
    }
}
