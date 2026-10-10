package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TimeFrame(val label: String, val displayName: String, val minutes: Long) {
    MN("MN", "Monthly", 30 * 24 * 60L),
    W1("W1", "Weekly", 7 * 24 * 60L),
    D1("D1", "Daily", 24 * 60L),
    H4("4H", "4 Hours", 4 * 60L),
    H1("1H", "1 Hour", 60L),
    M15("15M", "15 Mins", 15L)
}

data class SyntheticIndex(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val change24h: Double,
    val high24h: Double,
    val low24h: Double,
    val volatilityRating: String,
    val description: String
)

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 100.0
)

enum class MarketZoneType(val label: String, val isBullish: Boolean) {
    ORDER_BLOCK_BULLISH("Bullish OB", true),
    ORDER_BLOCK_BEARISH("Bearish OB", false),
    FVG_BULLISH("Bullish FVG", true),
    FVG_BEARISH("Bearish FVG", false),
    LIQUIDITY_BUY_SIDE("Buy-Side Liquidity (BSL)", false),
    LIQUIDITY_SELL_SIDE("Sell-Side Liquidity (SSL)", true),
    EQUAL_HIGHS("Equal Highs (EQH)", false),
    EQUAL_LOWS("Equal Lows (EQL)", true),
    OLD_HIGH("Old High / PDH", false),
    OLD_LOW("Old Low / PDL", true)
}

data class MarketZone(
    val id: String,
    val symbol: String,
    val timeframe: TimeFrame,
    val type: MarketZoneType,
    val topPrice: Double,
    val bottomPrice: Double,
    val createdTimestamp: Long,
    val isMitigated: Boolean = false,
    val isSwept: Boolean = false,
    val note: String = ""
)

enum class PatternType(val displayName: String, val isBullish: Boolean) {
    HEAD_AND_SHOULDERS("Head & Shoulders", false),
    INVERSE_H_AND_S("Inverse Head & Shoulders", true),
    DOUBLE_TOP("Double Top Reversal", false),
    DOUBLE_BOTTOM("Double Bottom Reversal", true),
    BULL_FLAG("Bullish Flag Continuation", true),
    BEAR_FLAG("Bearish Flag Continuation", false),
    ASCENDING_TRENDLINE("Ascending Trendline Support", true),
    DESCENDING_TRENDLINE("Descending Trendline Resistance", false)
}

data class ChartPattern(
    val type: PatternType,
    val confidence: Int,
    val keyLevel: Double,
    val targetPrice: Double,
    val description: String
)

data class TimeframeConfluence(
    val timeframe: TimeFrame,
    val trend: String, // "BULLISH", "BEARISH", "RANGING"
    val marketStructure: String, // "BOS (Break of Structure)", "CHoCH (Change of Char)", "Consolidation"
    val keyZoneNearby: String,
    val fvgPresent: Boolean,
    val orderBlockAligned: Boolean,
    val liquiditySwept: Boolean
)

data class MultiTimeframeAnalysis(
    val symbol: String,
    val primaryBias: String, // "STRONG BUY", "BUY", "NEUTRAL", "SELL", "STRONG SELL"
    val machineLearningProbability: Int,
    val smcConfluenceScore: Int,
    val sessionState: String,
    val orderFlowSummary: String,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val timeframes: List<TimeframeConfluence>,
    val detectedPatterns: List<ChartPattern>,
    val zones: List<MarketZone>
)

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val targetPrice: Double,
    val zoneType: String,
    val condition: String,
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val triggeredAt: Long? = null,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trading_signals")
data class TradingSignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val direction: String, // "BUY" or "SELL"
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val riskRewardRatio: String,
    val confluenceScore: Int,
    val timeframeConfluence: String,
    val smcReasoning: String,
    val pattern: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val symbolContext: String? = null
)
