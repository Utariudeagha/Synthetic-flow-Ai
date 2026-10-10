package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.data.model.MarketZone
import com.example.data.model.MarketZoneType
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GoldLiquidity
import com.example.ui.theme.PurpleInstitutional
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CandleStickChart(
    candles: List<Candle>,
    zones: List<MarketZone>,
    showOrderBlocks: Boolean = true,
    showFvg: Boolean = true,
    showLiquidity: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(TerminalSurface, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading market candles...", color = TextSecondary)
        }
        return
    }

    var scrollOffset by remember { mutableFloatStateOf(0f) }
    var selectedCandleIndex by remember { mutableIntStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()

    val visibleCandleCount = 30
    val totalCandles = candles.size

    // Determine window of candles to display based on scroll
    val maxScroll = max(0, totalCandles - visibleCandleCount)
    val startIndex = max(0, min(maxScroll, (totalCandles - visibleCandleCount - (scrollOffset / 30f).toInt())))
    val endIndex = min(totalCandles, startIndex + visibleCandleCount)
    val displayCandles = candles.subList(startIndex, endIndex)

    val minPrice = displayCandles.minOfOrNull { it.low } ?: 0.0
    val maxPrice = displayCandles.maxOfOrNull { it.high } ?: 1.0
    val priceRange = if (maxPrice - minPrice <= 0.0) 1.0 else (maxPrice - minPrice)

    // Formatters
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val activeCandle = if (selectedCandleIndex in displayCandles.indices) displayCandles[selectedCandleIndex] else displayCandles.lastOrNull()

    Column(modifier = modifier.fillMaxWidth()) {
        // Crosshair HUD Information Bar
        if (activeCandle != null) {
            Surface(
                color = TerminalSurface,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(activeCandle.timestamp)),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "O: ${formatPrice(activeCandle.open)}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "H: ${formatPrice(activeCandle.high)}",
                        color = BullishGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "L: ${formatPrice(activeCandle.low)}",
                        color = BearishRed,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val isBull = activeCandle.close >= activeCandle.open
                    Text(
                        text = "C: ${formatPrice(activeCandle.close)}",
                        color = if (isBull) BullishGreen else BearishRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Canvas Drawing Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .background(TerminalBackground)
                .testTag("candlestick_chart_canvas")
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scrollOffset = (scrollOffset + dragAmount.x).coerceIn(-1000f, 1000f)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val candleWidth = size.width / visibleCandleCount
                            val index = (offset.x / candleWidth).toInt()
                            selectedCandleIndex = if (index in displayCandles.indices) index else -1
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val pricePadding = 60f // space for right price axis
                val chartWidth = width - pricePadding
                val bottomTimePadding = 24f
                val chartHeight = height - bottomTimePadding

                // Helper to map price to Y
                fun priceToY(price: Double): Float {
                    val normalized = (price - minPrice) / priceRange
                    return (chartHeight - (normalized * chartHeight * 0.85f) - (chartHeight * 0.08f)).toFloat()
                }

                // Safe text drawing helper to prevent Compose TextPainter constraints crashes
                // when coordinates lie near or outside canvas bounds
                fun safeDrawText(
                    text: String,
                    targetX: Float,
                    targetY: Float,
                    style: TextStyle
                ) {
                    if (text.isEmpty() || width <= 0f || height <= 0f) return
                    val layoutResult = textMeasurer.measure(text = text, style = style)
                    val textWidth = layoutResult.size.width.toFloat()
                    val textHeight = layoutResult.size.height.toFloat()

                    // Ensure the text fits within the canvas viewport
                    val maxX = max(0f, width - textWidth)
                    val maxY = max(0f, height - textHeight)
                    val clampedX = targetX.coerceIn(0f, maxX)
                    val clampedY = targetY.coerceIn(0f, maxY)

                    drawText(
                        textLayoutResult = layoutResult,
                        topLeft = Offset(clampedX, clampedY)
                    )
                }

                // 1. Draw horizontal price grid lines and labels
                val gridLevels = 5
                val dottedEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                for (i in 0..gridLevels) {
                    val levelPrice = minPrice + (priceRange * (i.toDouble() / gridLevels))
                    val y = priceToY(levelPrice)

                    // Dotted grid line
                    drawLine(
                        color = TerminalBorder.copy(alpha = 0.4f),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        pathEffect = dottedEffect,
                        strokeWidth = 1f
                    )

                    // Right price label
                    val priceLabel = formatPrice(levelPrice)
                    safeDrawText(
                        text = priceLabel,
                        targetX = chartWidth + 6f,
                        targetY = y - 8f,
                        style = TextStyle(
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // 2. Draw SMC / ICT Overlays
                for (zone in zones) {
                    val zoneTopY = priceToY(zone.topPrice)
                    val zoneBottomY = priceToY(zone.bottomPrice)
                    val rawZoneHeight = abs(zoneBottomY - zoneTopY)
                    val zoneHeight = max(4f, rawZoneHeight)
                    val zoneY = min(zoneTopY, zoneBottomY)

                    // Skip drawing zones completely outside the visible canvas
                    if (zoneY > height || zoneY + zoneHeight < 0f) continue

                    when (zone.type) {
                        MarketZoneType.ORDER_BLOCK_BULLISH -> if (showOrderBlocks) {
                            // Shaded green order block demand zone
                            drawRect(
                                color = BullishGreen.copy(alpha = 0.18f),
                                topLeft = Offset(0f, zoneY),
                                size = Size(chartWidth, zoneHeight)
                            )
                            drawLine(
                                color = BullishGreen.copy(alpha = 0.7f),
                                start = Offset(0f, zoneY),
                                end = Offset(chartWidth, zoneY),
                                strokeWidth = 1.5f
                            )
                            safeDrawText(
                                text = "+OB (Demand)",
                                targetX = 10f,
                                targetY = zoneY + 2f,
                                style = TextStyle(color = BullishGreen, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            )
                        }

                        MarketZoneType.ORDER_BLOCK_BEARISH -> if (showOrderBlocks) {
                            // Shaded red order block supply zone
                            drawRect(
                                color = BearishRed.copy(alpha = 0.18f),
                                topLeft = Offset(0f, zoneY),
                                size = Size(chartWidth, zoneHeight)
                            )
                            drawLine(
                                color = BearishRed.copy(alpha = 0.7f),
                                start = Offset(0f, zoneY + zoneHeight),
                                end = Offset(chartWidth, zoneY + zoneHeight),
                                strokeWidth = 1.5f
                            )
                            safeDrawText(
                                text = "-OB (Supply)",
                                targetX = 10f,
                                targetY = zoneY + 2f,
                                style = TextStyle(color = BearishRed, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            )
                        }

                        MarketZoneType.FVG_BULLISH, MarketZoneType.FVG_BEARISH -> if (showFvg) {
                            // Cyan / purple fair value gap band
                            val isBullFvg = zone.type == MarketZoneType.FVG_BULLISH
                            val fvgColor = if (isBullFvg) CyanAccent else PurpleInstitutional
                            drawRect(
                                color = fvgColor.copy(alpha = 0.14f),
                                topLeft = Offset(0f, zoneY),
                                size = Size(chartWidth, zoneHeight)
                            )
                            drawLine(
                                color = fvgColor.copy(alpha = 0.6f),
                                start = Offset(0f, zoneY),
                                end = Offset(chartWidth, zoneY),
                                pathEffect = dottedEffect,
                                strokeWidth = 1f
                            )
                            safeDrawText(
                                text = if (isBullFvg) "FVG (Imbalance)" else "-FVG",
                                targetX = chartWidth * 0.45f,
                                targetY = zoneY + 2f,
                                style = TextStyle(color = fvgColor, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                            )
                        }

                        MarketZoneType.EQUAL_HIGHS, MarketZoneType.OLD_HIGH -> if (showLiquidity) {
                            // Amber Gold dotted line for buy-side liquidity
                            drawLine(
                                color = GoldLiquidity,
                                start = Offset(0f, zoneY),
                                end = Offset(chartWidth, zoneY),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
                                strokeWidth = 1.5f
                            )
                            safeDrawText(
                                text = "BSL / ${zone.type.label}",
                                targetX = chartWidth * 0.65f,
                                targetY = zoneY - 12f,
                                style = TextStyle(color = GoldLiquidity, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            )
                        }

                        MarketZoneType.EQUAL_LOWS, MarketZoneType.OLD_LOW -> if (showLiquidity) {
                            // Amber Gold dotted line for sell-side liquidity
                            drawLine(
                                color = GoldLiquidity,
                                start = Offset(0f, zoneY + zoneHeight),
                                end = Offset(chartWidth, zoneY + zoneHeight),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
                                strokeWidth = 1.5f
                            )
                            safeDrawText(
                                text = "SSL / ${zone.type.label}",
                                targetX = chartWidth * 0.65f,
                                targetY = zoneY + zoneHeight + 2f,
                                style = TextStyle(color = GoldLiquidity, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        else -> {}
                    }
                }

                // 3. Draw Candlesticks
                val candleSlotWidth = chartWidth / displayCandles.size
                val candleBodyWidth = candleSlotWidth * 0.68f

                displayCandles.forEachIndexed { index, candle ->
                    val centerX = (index * candleSlotWidth) + (candleSlotWidth / 2f)
                    val isBullish = candle.close >= candle.open
                    val candleColor = if (isBullish) BullishGreen else BearishRed

                    val highY = priceToY(candle.high)
                    val lowY = priceToY(candle.low)
                    val openY = priceToY(candle.open)
                    val closeY = priceToY(candle.close)

                    val bodyTop = min(openY, closeY)
                    val bodyBottom = max(openY, closeY)
                    val bodyHeight = max(2f, bodyBottom - bodyTop)

                    // Draw Wick
                    drawLine(
                        color = candleColor,
                        start = Offset(centerX, highY),
                        end = Offset(centerX, lowY),
                        strokeWidth = 1.5f
                    )

                    // Draw Body
                    drawRect(
                        color = candleColor,
                        topLeft = Offset(centerX - (candleBodyWidth / 2f), bodyTop),
                        size = Size(candleBodyWidth, bodyHeight)
                    )

                    // Selected Crosshair vertical line
                    if (index == selectedCandleIndex) {
                        drawLine(
                            color = CyanAccent.copy(alpha = 0.8f),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, chartHeight),
                            strokeWidth = 1f,
                            pathEffect = dottedEffect
                        )
                        // Selected price circle on close
                        drawCircle(
                            color = CyanAccent,
                            radius = 3.5f,
                            center = Offset(centerX, closeY)
                        )
                    }
                }

                // Current Market Price horizontal indicator line
                val latestCandle = displayCandles.lastOrNull()
                if (latestCandle != null) {
                    val currentY = priceToY(latestCandle.close)
                    val isBull = latestCandle.close >= latestCandle.open
                    val currentLineColor = if (isBull) BullishGreen else BearishRed

                    drawLine(
                        color = currentLineColor,
                        start = Offset(0f, currentY),
                        end = Offset(chartWidth, currentY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )

                    // Current price tag box on right
                    val clampedTagY = (currentY - 8f).coerceIn(0f, max(0f, height - 16f))
                    drawRect(
                        color = currentLineColor,
                        topLeft = Offset(chartWidth, clampedTagY),
                        size = Size(pricePadding, 16f)
                    )
                    safeDrawText(
                        text = formatPrice(latestCandle.close),
                        targetX = chartWidth + 3f,
                        targetY = clampedTagY + 1f,
                        style = TextStyle(
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    return if (price >= 10000) {
        String.format(Locale.US, "%.1f", price)
    } else {
        String.format(Locale.US, "%.2f", price)
    }
}
