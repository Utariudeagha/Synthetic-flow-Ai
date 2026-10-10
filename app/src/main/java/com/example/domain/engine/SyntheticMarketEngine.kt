package com.example.domain.engine

import com.example.data.model.Candle
import com.example.data.model.SyntheticIndex
import com.example.data.model.TimeFrame
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object SyntheticMarketEngine {
    private val random = Random(42)

    val supportedIndices = listOf(
        SyntheticIndex(
            symbol = "V75",
            name = "Volatility 75 Index",
            currentPrice = 432580.40,
            change24h = +3.42,
            high24h = 445200.00,
            low24h = 421100.00,
            volatilityRating = "Extreme (75% Volatility)",
            description = "Constant volatility index with high tick movement and institutional liquidity sweeps."
        ),
        SyntheticIndex(
            symbol = "V100",
            name = "Volatility 100 Index",
            currentPrice = 2418.65,
            change24h = -1.85,
            high24h = 2490.20,
            low24h = 2380.10,
            volatilityRating = "High (100% Volatility)",
            description = "Rapid intraday trends with frequent Fair Value Gaps and order block mitigations."
        ),
        SyntheticIndex(
            symbol = "BOOM1000",
            name = "Boom 1000 Index",
            currentPrice = 9840.30,
            change24h = +5.12,
            high24h = 10120.00,
            low24h = 9650.00,
            volatilityRating = "Spike Driven (Spike Up)",
            description = "Characterized by steady tick declines punctuated by sudden explosive upward spikes."
        ),
        SyntheticIndex(
            symbol = "CRASH1000",
            name = "Crash 1000 Index",
            currentPrice = 6120.75,
            change24h = -4.30,
            high24h = 6390.00,
            low24h = 6010.50,
            volatilityRating = "Crash Driven (Drop Down)",
            description = "Smooth upward price appreciation with aggressive institutional crash drops."
        ),
        SyntheticIndex(
            symbol = "STEP",
            name = "Step Index",
            currentPrice = 8520.10,
            change24h = +0.78,
            high24h = 8610.00,
            low24h = 8450.00,
            volatilityRating = "Step Constant",
            description = "Equal probability discrete steps. Outstanding for clear market structure and BOS."
        ),
        SyntheticIndex(
            symbol = "V25",
            name = "Volatility 25 Index",
            currentPrice = 1845.20,
            change24h = +1.15,
            high24h = 1890.00,
            low24h = 1810.00,
            volatilityRating = "Balanced Volatility",
            description = "Smooth swing transitions, clean double tops/bottoms, and institutional trendlines."
        )
    )

    fun generateHistoricalCandles(symbol: String, timeframe: TimeFrame, count: Int = 45): MutableList<Candle> {
        val basePrice = when (symbol) {
            "V75" -> 430000.0
            "V100" -> 2400.0
            "BOOM1000" -> 9800.0
            "CRASH1000" -> 6100.0
            "STEP" -> 8500.0
            "V25" -> 1840.0
            else -> 1000.0
        }

        val volatilityFactor = when (symbol) {
            "V75" -> 3500.0
            "V100" -> 25.0
            "BOOM1000" -> 70.0
            "CRASH1000" -> 60.0
            "STEP" -> 20.0
            "V25" -> 15.0
            else -> 10.0
        } * when (timeframe) {
            TimeFrame.MN -> 8.0
            TimeFrame.W1 -> 4.5
            TimeFrame.D1 -> 2.5
            TimeFrame.H4 -> 1.5
            TimeFrame.H1 -> 1.0
            TimeFrame.M15 -> 0.5
        }

        val candleList = mutableListOf<Candle>()
        val currentTime = System.currentTimeMillis()
        val intervalMillis = timeframe.minutes * 60 * 1000L

        var currentClose = basePrice
        // Generate historical trend waves with ICT swings
        for (i in (count - 1) downTo 0) {
            val candleTime = currentTime - (i * intervalMillis)
            val trendBias = when {
                (i % 14) < 7 -> 0.6  // Bullish wave
                else -> -0.55        // Bearish wave
            }

            // Boom / Crash specialized spikes
            var delta = (random.nextGaussian() * volatilityFactor * 0.7) + (trendBias * volatilityFactor * 0.5)
            if (symbol == "BOOM1000" && i % 8 == 2) {
                delta += volatilityFactor * 2.5 // Spike up
            } else if (symbol == "CRASH1000" && i % 8 == 5) {
                delta -= volatilityFactor * 2.6 // Crash drop
            }

            val open = currentClose
            val close = open + delta
            val high = max(open, close) + (abs(random.nextGaussian()) * volatilityFactor * 0.4)
            val low = min(open, close) - (abs(random.nextGaussian()) * volatilityFactor * 0.4)
            val volume = 50.0 + abs(random.nextGaussian()) * 150.0

            candleList.add(
                Candle(
                    timestamp = candleTime,
                    open = round(open * 100) / 100.0,
                    high = round(high * 100) / 100.0,
                    low = round(low * 100) / 100.0,
                    close = round(close * 100) / 100.0,
                    volume = round(volume * 10) / 10.0
                )
            )
            currentClose = close
        }

        return candleList
    }

    fun nextTick(currentPrice: Double, symbol: String): Double {
        val volatility = when (symbol) {
            "V75" -> 180.0
            "V100" -> 1.5
            "BOOM1000" -> 3.0
            "CRASH1000" -> 2.5
            "STEP" -> 0.5
            "V25" -> 0.8
            else -> 1.0
        }

        var delta = random.nextGaussian() * volatility

        // Occasional spike for Boom and Crash
        if (symbol == "BOOM1000" && random.nextInt(40) == 7) {
            delta += volatility * 25.0
        } else if (symbol == "CRASH1000" && random.nextInt(40) == 13) {
            delta -= volatility * 25.0
        }

        val updated = currentPrice + delta
        return round(updated * 100) / 100.0
    }
}
