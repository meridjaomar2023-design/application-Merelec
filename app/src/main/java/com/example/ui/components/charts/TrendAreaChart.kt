package com.example.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun TrendAreaChart(
    buckets: List<PeriodBucket>,
    modifier: Modifier = Modifier
) {
    if (buckets.size < 2) {
        return
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(buckets) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val maxVal = remember(buckets) {
        val highest = buckets.maxOfOrNull { max(it.caTotal, it.reglementsTotal) } ?: 0.0
        if (highest <= 0.0) 10000.0 else highest * 1.2
    }

    val density = LocalDensity.current
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = onSurfaceVariant.copy(alpha = 0.12f)
    val textPaintColor = android.graphics.Color.argb(
        (0.75f * 255).toInt(),
        (onSurfaceVariant.red * 255).toInt(),
        (onSurfaceVariant.green * 255).toInt(),
        (onSurfaceVariant.blue * 255).toInt()
    )
    val labelTextSizePx = with(density) { 10.sp.toPx() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trend_area_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Courbes d'Évolution & Tendance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
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
                                .clip(CircleShape)
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

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Trajectoire continue avec dégradés de remplissage",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant.copy(alpha = 0.8f)
            )

            // Scrubber result box
            selectedIndex?.let { idx ->
                val bucket = buckets.getOrNull(idx)
                if (bucket != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bucket.label,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "CA: ${CurrencyUtils.formatDaCompact(bucket.caTotal)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ClientColor
                                )
                                Text(
                                    text = "Règ: ${CurrencyUtils.formatDaCompact(bucket.reglementsTotal)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ReglementColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(buckets) {
                            detectTapGestures { offset ->
                                val leftMargin = with(density) { 46.dp.toPx() }
                                val rightMargin = with(density) { 10.dp.toPx() }
                                val bottomMargin = with(density) { 26.dp.toPx() }
                                val chartWidth = size.width - leftMargin - rightMargin
                                val chartHeight = size.height - bottomMargin

                                if (offset.x in leftMargin..(size.width - rightMargin) && offset.y <= chartHeight) {
                                    val stepX = chartWidth / (buckets.size - 1)
                                    val idx = ((offset.x - leftMargin + stepX / 2f) / stepX).toInt().coerceIn(0, buckets.size - 1)
                                    selectedIndex = if (selectedIndex == idx) null else idx
                                } else {
                                    selectedIndex = null
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

                    // 1. Grid lines and Y labels
                    val gridSteps = 4
                    for (step in 0..gridSteps) {
                        val fraction = step.toFloat() / gridSteps
                        val y = zeroY - fraction * chartHeight
                        val valueAtY = fraction * maxVal

                        drawLine(
                            color = gridLineColor,
                            start = Offset(leftMargin, y),
                            end = Offset(size.width - rightMargin, y),
                            strokeWidth = 1.dp.toPx()
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            CurrencyUtils.formatDaCompact(valueAtY),
                            leftMargin - 6.dp.toPx(),
                            y + 3.dp.toPx(),
                            paint
                        )
                    }

                    // 2. Compute Points for CA and Reglements
                    val stepX = chartWidth / (buckets.size - 1)
                    val anim = animationProgress.value

                    val caPoints = mutableListOf<Offset>()
                    val regPoints = mutableListOf<Offset>()

                    buckets.forEachIndexed { i, b ->
                        val x = leftMargin + i * stepX
                        val caY = zeroY - ((b.caTotal / maxVal) * anim * chartHeight).toFloat()
                        val regY = zeroY - ((b.reglementsTotal / maxVal) * anim * chartHeight).toFloat()
                        caPoints.add(Offset(x, caY))
                        regPoints.add(Offset(x, regY))
                    }

                    // Helper to create smoothed bezier path
                    fun createSmoothPath(points: List<Offset>): Path {
                        val path = Path()
                        if (points.isEmpty()) return path
                        path.moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val midX = (p0.x + p1.x) / 2f
                            path.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        }
                        return path
                    }

                    // CA Gradient Area & Line
                    val caStrokePath = createSmoothPath(caPoints)
                    val caAreaPath = Path().apply {
                        addPath(caStrokePath)
                        lineTo(caPoints.last().x, zeroY)
                        lineTo(caPoints.first().x, zeroY)
                        close()
                    }

                    drawPath(
                        path = caAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(ClientColor.copy(alpha = 0.25f * anim), Color.Transparent),
                            startY = topMargin,
                            endY = zeroY
                        )
                    )

                    drawPath(
                        path = caStrokePath,
                        color = ClientColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Reglements Gradient Area & Line
                    val regStrokePath = createSmoothPath(regPoints)
                    val regAreaPath = Path().apply {
                        addPath(regStrokePath)
                        lineTo(regPoints.last().x, zeroY)
                        lineTo(regPoints.first().x, zeroY)
                        close()
                    }

                    drawPath(
                        path = regAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(ReglementColor.copy(alpha = 0.20f * anim), Color.Transparent),
                            startY = topMargin,
                            endY = zeroY
                        )
                    )

                    drawPath(
                        path = regStrokePath,
                        color = ReglementColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw Point Dots and X-axis Labels
                    caPoints.forEachIndexed { i, pt ->
                        // White inner ring with blue outline
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                        drawCircle(color = ClientColor, radius = 3.dp.toPx(), center = pt)

                        val regPt = regPoints[i]
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = regPt)
                        drawCircle(color = ReglementColor, radius = 3.dp.toPx(), center = regPt)

                        // Highlight vertical marker line if selected
                        if (selectedIndex == i) {
                            drawLine(
                                color = ClientColor.copy(alpha = 0.4f),
                                start = Offset(pt.x, topMargin),
                                end = Offset(pt.x, zeroY),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        // X-axis label
                        val labelPaint = android.graphics.Paint().apply {
                            color = if (selectedIndex == i) {
                                (ClientColor.value.toLong() and 0xFFFFFFFFL).toInt()
                            } else {
                                textPaintColor
                            }
                            textSize = labelTextSizePx
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = selectedIndex == i
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            buckets[i].shortLabel,
                            pt.x,
                            zeroY + 16.dp.toPx(),
                            labelPaint
                        )
                    }
                }
            }
        }
    }
}
