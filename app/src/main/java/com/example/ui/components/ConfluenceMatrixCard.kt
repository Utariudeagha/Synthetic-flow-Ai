package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MultiTimeframeAnalysis
import com.example.data.model.TimeframeConfluence
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

@Composable
fun ConfluenceMatrixCard(
    analysis: MultiTimeframeAnalysis,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val isBullish = analysis.primaryBias.contains("BUY")
    val isBearish = analysis.primaryBias.contains("SELL")
    val biasColor = when {
        isBullish -> BullishGreen
        isBearish -> BearishRed
        else -> CyanAccent
    }
    val biasBg = when {
        isBullish -> BullishBg
        isBearish -> BearishBg
        else -> CyanBg
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("confluence_matrix_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyanBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Analysis",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ML & SMC CONFLUENCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${analysis.symbol} Higher Timeframe Matrix",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                // Primary Bias Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = biasBg,
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(biasColor.copy(alpha = 0.5f)))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isBullish) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = biasColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = analysis.primaryBias,
                            color = biasColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Score Meters: Machine Learning Probability & SMC Confluence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ML Probability Meter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(TerminalSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ML Trend Prob.", fontSize = 11.sp, color = TextSecondary)
                            Text("${analysis.machineLearningProbability}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { analysis.machineLearningProbability / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyanAccent,
                            trackColor = TerminalBorder
                        )
                    }
                }

                // SMC Confluence Score Meter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(TerminalSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("SMC Confluence", fontSize = 11.sp, color = TextSecondary)
                            Text("${analysis.smcConfluenceScore}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { analysis.smcConfluenceScore / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = BullishGreen,
                            trackColor = TerminalBorder
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Institutional Flow Highlight
            Text(
                text = analysis.orderFlowSummary,
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 17.sp,
                modifier = Modifier
                    .background(TerminalBackground, RoundedCornerShape(8.dp))
                    .padding(10.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Toggle Expand Button for 6-Timeframe breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide Timeframe Confluences" else "View 6-Timeframe Confluence (MN to M15)",
                    fontSize = 12.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expandable Timeframe Details Table
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                ) {
                    HorizontalDivider(color = TerminalBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    analysis.timeframes.forEach { tfConfluence ->
                        TimeframeConfluenceRow(tfConfluence)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TimeframeConfluenceRow(tf: TimeframeConfluence) {
    val isBull = tf.trend == "BULLISH"
    val isBear = tf.trend == "BEARISH"
    val trendColor = if (isBull) BullishGreen else if (isBear) BearishRed else TextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalSurfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = TerminalBackground,
                shape = RoundedCornerShape(4.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
            ) {
                Text(
                    text = tf.timeframe.label,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tf.marketStructure,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tf.trend,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = trendColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (tf.orderBlockAligned) {
                Text(
                    text = "OB",
                    fontSize = 9.sp,
                    color = PurpleInstitutional,
                    modifier = Modifier
                        .background(PurpleInstitutional.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (tf.fvgPresent) {
                Text(
                    text = "FVG",
                    fontSize = 9.sp,
                    color = CyanAccent,
                    modifier = Modifier
                        .background(CyanBg, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (tf.liquiditySwept) {
                Text(
                    text = "SWEEP",
                    fontSize = 9.sp,
                    color = GoldLiquidity,
                    modifier = Modifier
                        .background(GoldLiquidity.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
