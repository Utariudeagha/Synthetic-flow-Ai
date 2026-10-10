package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketZone
import com.example.data.model.PatternType
import com.example.data.model.SyntheticIndex
import com.example.data.model.TimeFrame
import com.example.ui.MainViewModel
import com.example.ui.components.CandleStickChart
import com.example.ui.components.ConfluenceMatrixCard
import com.example.ui.components.CreateAlertDialog
import com.example.ui.theme.BearishBg
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishBg
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanBg
import com.example.ui.theme.GoldLiquidity
import com.example.ui.theme.PurpleInstitutional
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ChartScreen(
    viewModel: MainViewModel,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indices by viewModel.indices.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()
    val currentAnalysis by viewModel.currentAnalysis.collectAsState()

    val showOrderBlocks by viewModel.showOrderBlocks.collectAsState()
    val showFvg by viewModel.showFvg.collectAsState()
    val showLiquidity by viewModel.showLiquidity.collectAsState()

    var showAlertDialog by remember { mutableStateOf(false) }
    var presetAlertPrice by remember { mutableStateOf<Double?>(null) }

    val candles = viewModel.getCurrentCandles()
    val activeIndex = indices.find { it.symbol == selectedSymbol } ?: indices.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .testTag("chart_screen_lazy_column"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Synthetic Indices Carousel Ticker
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                items(indices) { index ->
                    IndexTickerChip(
                        index = index,
                        isSelected = index.symbol == selectedSymbol,
                        onClick = { viewModel.selectSymbol(index.symbol) }
                    )
                }
            }
        }

        // 2. Active Index Quick Stats & Timeframe Bar
        item {
            if (activeIndex != null) {
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeIndex.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = TerminalSurfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = activeIndex.volatilityRating,
                                        fontSize = 10.sp,
                                        color = CyanAccent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatPrice(activeIndex.currentPrice),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val isPositive = activeIndex.change24h >= 0
                                Text(
                                    text = "${if (isPositive) "+" else ""}${activeIndex.change24h}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) BullishGreen else BearishRed
                                )
                            }
                        }

                        // Set Alert & AI Chat action buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    presetAlertPrice = activeIndex.currentPrice
                                    showAlertDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TerminalSurfaceVariant, contentColor = CyanAccent),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("set_alert_button")
                            ) {
                                Icon(Icons.Default.AddAlert, contentDescription = "Add Alert", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onNavigateToChat,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = TerminalBackground),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("chat_analyst_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Analyst", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Analyst", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timeframe Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TimeFrame.entries.forEach { tf ->
                            TimeframeChip(
                                label = tf.label,
                                isSelected = tf == selectedTimeframe,
                                onClick = { viewModel.selectTimeframe(tf) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // SMC / ICT Overlay Toggle Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OverlayChip("Order Blocks", showOrderBlocks) { viewModel.toggleOverlay("OB") }
                        OverlayChip("Fair Value Gaps", showFvg) { viewModel.toggleOverlay("FVG") }
                        OverlayChip("Liquidity Pools", showLiquidity) { viewModel.toggleOverlay("LIQ") }
                    }
                }
            }
        }

        // 3. Interactive Candlestick Chart
        item {
            Spacer(modifier = Modifier.height(6.dp))
            CandleStickChart(
                candles = candles,
                zones = currentAnalysis?.zones ?: emptyList(),
                showOrderBlocks = showOrderBlocks,
                showFvg = showFvg,
                showLiquidity = showLiquidity,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // 4. Confluence Matrix Card
        item {
            if (currentAnalysis != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    ConfluenceMatrixCard(analysis = currentAnalysis!!)
                }
            }
        }

        // 5. Detected Institutional Key Zones
        item {
            if (currentAnalysis != null && currentAnalysis!!.zones.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(
                        text = "DETECTED INSTITUTIONAL KEY ZONES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    currentAnalysis!!.zones.take(4).forEach { zone ->
                        KeyZoneCard(
                            zone = zone,
                            onSetAlert = {
                                presetAlertPrice = zone.topPrice
                                showAlertDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // 6. Detected Chart Patterns (Head & Shoulders, Flags, Double Bottoms)
        item {
            if (currentAnalysis != null && currentAnalysis!!.detectedPatterns.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(
                        text = "DETECTED CHART PATTERNS & LIQUIDITY SWEEPS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    currentAnalysis!!.detectedPatterns.forEach { pattern ->
                        PatternCard(pattern = pattern)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    // Modal Create Alert Dialog
    if (showAlertDialog) {
        CreateAlertDialog(
            symbol = selectedSymbol,
            initialPrice = presetAlertPrice ?: activeIndex?.currentPrice ?: 1000.0,
            onDismiss = { showAlertDialog = false },
            onConfirm = { targetPrice, zoneType, condition, note ->
                viewModel.createAlert(targetPrice, zoneType, condition, note)
                showAlertDialog = false
            }
        )
    }
}

@Composable
fun IndexTickerChip(
    index: SyntheticIndex,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isPositive = index.change24h >= 0
    val changeColor = if (isPositive) BullishGreen else BearishRed

    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("ticker_chip_${index.symbol}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TerminalSurfaceVariant else TerminalSurface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) CyanAccent else TerminalBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = index.symbol,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) CyanAccent else TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${if (isPositive) "+" else ""}${index.change24h}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = changeColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatPrice(index.currentPrice),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun TimeframeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("timeframe_chip_$label"),
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) CyanAccent else TerminalSurface,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) CyanAccent else TerminalBorder
            )
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) TerminalBackground else TextSecondary
        )
    }
}

@Composable
fun OverlayChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) CyanBg else TerminalBackground,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isActive) CyanAccent.copy(alpha = 0.6f) else TerminalBorder
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) CyanAccent else TextMuted
            )
        }
    }
}

@Composable
fun KeyZoneCard(
    zone: MarketZone,
    onSetAlert: () -> Unit
) {
    val isBullish = zone.type.isBullish
    val zoneColor = if (isBullish) BullishGreen else BearishRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = zone.type.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = zoneColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[${zone.timeframe.label}]",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Range: ${formatPrice(zone.bottomPrice)} - ${formatPrice(zone.topPrice)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = zone.note,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            OutlinedButton(
                onClick = onSetAlert,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Alert", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun PatternCard(pattern: com.example.data.model.ChartPattern) {
    val isBull = pattern.type.isBullish
    val patternColor = if (isBull) BullishGreen else BearishRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pattern.type.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = patternColor
                )
                Surface(
                    color = CyanBg,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${pattern.confidence}% Confidence",
                        fontSize = 10.sp,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pattern.description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Key Level: ${formatPrice(pattern.keyLevel)}",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Projected Target: ${formatPrice(pattern.targetPrice)}",
                    fontSize = 11.sp,
                    color = BullishGreen,
                    fontFamily = FontFamily.Monospace
                )
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
