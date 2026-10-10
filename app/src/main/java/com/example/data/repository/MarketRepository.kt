package com.example.data.repository

import android.content.Context
import com.example.data.local.AlertDao
import com.example.data.local.ChatDao
import com.example.data.local.SignalDao
import com.example.data.model.Candle
import com.example.data.model.ChatMessageEntity
import com.example.data.model.MultiTimeframeAnalysis
import com.example.data.model.PriceAlertEntity
import com.example.data.model.SyntheticIndex
import com.example.data.model.TimeFrame
import com.example.data.model.TradingSignalEntity
import com.example.data.remote.GeminiChatService
import com.example.domain.engine.SmcIctAnalysisEngine
import com.example.domain.engine.SyntheticMarketEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class MarketRepository(
    private val alertDao: AlertDao,
    private val signalDao: SignalDao,
    private val chatDao: ChatDao,
    private val geminiService: GeminiChatService
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Current synthetic indices list
    private val _indices = MutableStateFlow(SyntheticMarketEngine.supportedIndices)
    val indices: StateFlow<List<SyntheticIndex>> = _indices.asStateFlow()

    // Selected active symbol
    private val _selectedSymbol = MutableStateFlow("V75")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    // Selected active timeframe
    private val _selectedTimeframe = MutableStateFlow(TimeFrame.H1)
    val selectedTimeframe: StateFlow<TimeFrame> = _selectedTimeframe.asStateFlow()

    // In-memory cache of multi-timeframe candles: Map<Symbol, Map<Timeframe, List<Candle>>>
    private val candleStore = ConcurrentHashMap<String, MutableMap<TimeFrame, MutableList<Candle>>>()

    // Current analysis state
    private val _currentAnalysis = MutableStateFlow<MultiTimeframeAnalysis?>(null)
    val currentAnalysis: StateFlow<MultiTimeframeAnalysis?> = _currentAnalysis.asStateFlow()

    // Triggered alert notification banner event
    private val _alertNotification = MutableSharedFlow<PriceAlertEntity>(extraBufferCapacity = 5)
    val alertNotification: SharedFlow<PriceAlertEntity> = _alertNotification.asSharedFlow()

    // Room DB streams
    val allAlerts: Flow<List<PriceAlertEntity>> = alertDao.getAllAlerts()
    val allSignals: Flow<List<TradingSignalEntity>> = signalDao.getAllSignals()
    val chatMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    init {
        // Initialize candle history for all symbols and timeframes
        for (index in SyntheticMarketEngine.supportedIndices) {
            val tfMap = mutableMapOf<TimeFrame, MutableList<Candle>>()
            for (tf in TimeFrame.entries) {
                tfMap[tf] = SyntheticMarketEngine.generateHistoricalCandles(index.symbol, tf, 45)
            }
            candleStore[index.symbol] = tfMap
        }

        updateCurrentAnalysis()
        startLiveSimulationLoop()
        generateInitialSignals()
    }

    fun selectSymbol(symbol: String) {
        _selectedSymbol.value = symbol
        updateCurrentAnalysis()
    }

    fun selectTimeframe(tf: TimeFrame) {
        _selectedTimeframe.value = tf
    }

    fun getCandles(symbol: String, timeframe: TimeFrame): List<Candle> {
        return candleStore[symbol]?.get(timeframe) ?: emptyList()
    }

    private fun updateCurrentAnalysis() {
        val symbol = _selectedSymbol.value
        val currentIndex = _indices.value.find { it.symbol == symbol } ?: return
        val tfMap = candleStore[symbol] ?: return

        val analysis = SmcIctAnalysisEngine.analyze(symbol, currentIndex.currentPrice, tfMap)
        _currentAnalysis.value = analysis
    }

    private fun startLiveSimulationLoop() {
        repositoryScope.launch {
            while (isActive) {
                delay(1200) // Realistic tick interval
                val currentIndices = _indices.value.toMutableList()
                val updatedIndices = mutableListOf<SyntheticIndex>()

                for (idx in currentIndices) {
                    val updatedPrice = SyntheticMarketEngine.nextTick(idx.currentPrice, idx.symbol)
                    val priceDelta = updatedPrice - idx.currentPrice
                    val newHigh = max(idx.high24h, updatedPrice)
                    val newLow = min(idx.low24h, updatedPrice)

                    val updatedIdx = idx.copy(
                        currentPrice = updatedPrice,
                        high24h = newHigh,
                        low24h = newLow,
                        change24h = round((idx.change24h + (priceDelta / idx.currentPrice * 100)) * 100) / 100.0
                    )
                    updatedIndices.add(updatedIdx)

                    // Update live candle in H1 and selected timeframe
                    val tfMap = candleStore[idx.symbol]
                    if (tfMap != null) {
                        for (tf in listOf(TimeFrame.M15, TimeFrame.H1, TimeFrame.H4)) {
                            val candleList = tfMap[tf]
                            if (candleList != null && candleList.isNotEmpty()) {
                                val lastIndex = candleList.size - 1
                                val last = candleList[lastIndex]
                                val updatedCandle = last.copy(
                                    high = max(last.high, updatedPrice),
                                    low = min(last.low, updatedPrice),
                                    close = updatedPrice,
                                    volume = last.volume + 1.2
                                )
                                candleList[lastIndex] = updatedCandle
                            }
                        }
                    }

                    // Check live alerts
                    checkAlerts(idx.symbol, updatedPrice)
                }

                _indices.value = updatedIndices

                // Refresh active analysis for selected symbol
                updateCurrentAnalysis()
            }
        }
    }

    private suspend fun checkAlerts(symbol: String, price: Double) {
        val activeAlerts = alertDao.getActiveAlertsSnapshot()
        for (alert in activeAlerts) {
            if (alert.symbol != symbol) continue

            val threshold = price * 0.001
            val isHit = abs(price - alert.targetPrice) <= threshold

            if (isHit) {
                val updated = alert.copy(
                    isActive = false,
                    isTriggered = true,
                    triggeredAt = System.currentTimeMillis()
                )
                alertDao.updateAlert(updated)
                _alertNotification.emit(updated)
            }
        }
    }

    suspend fun createAlert(
        symbol: String,
        targetPrice: Double,
        zoneType: String,
        condition: String,
        note: String
    ) {
        val alert = PriceAlertEntity(
            symbol = symbol,
            targetPrice = targetPrice,
            zoneType = zoneType,
            condition = condition,
            isActive = true,
            isTriggered = false,
            note = note
        )
        alertDao.insertAlert(alert)
    }

    suspend fun toggleAlert(alert: PriceAlertEntity) {
        val updated = alert.copy(isActive = !alert.isActive)
        alertDao.updateAlert(updated)
    }

    suspend fun deleteAlert(alert: PriceAlertEntity) {
        alertDao.deleteAlert(alert)
    }

    suspend fun clearTriggeredAlerts() {
        alertDao.clearTriggeredAlerts()
    }

    suspend fun toggleSaveSignal(signal: TradingSignalEntity) {
        val updated = signal.copy(isSaved = !signal.isSaved)
        signalDao.updateSignal(updated)
    }

    suspend fun generateNewSignalForSymbol(symbol: String) {
        val current = _currentAnalysis.value ?: return
        val isBuy = current.primaryBias.contains("BUY")

        val signal = TradingSignalEntity(
            symbol = symbol,
            direction = if (isBuy) "BUY" else "SELL",
            entryPrice = current.entryPrice,
            stopLoss = current.stopLoss,
            takeProfit1 = current.takeProfit1,
            takeProfit2 = current.takeProfit2,
            riskRewardRatio = "1:3.2",
            confluenceScore = current.smcConfluenceScore,
            timeframeConfluence = "D1 + 4H + 1H Confluence",
            smcReasoning = current.orderFlowSummary,
            pattern = current.detectedPatterns.firstOrNull()?.type?.displayName ?: "Liquidity Sweep & OB Rejection",
            isSaved = false
        )
        signalDao.insertSignal(signal)
    }

    private fun generateInitialSignals() {
        repositoryScope.launch {
            val v75 = _indices.value.find { it.symbol == "V75" }
            if (v75 != null) {
                signalDao.insertSignal(
                    TradingSignalEntity(
                        symbol = "V75",
                        direction = "BUY",
                        entryPrice = 432580.0,
                        stopLoss = 429900.0,
                        takeProfit1 = 438000.0,
                        takeProfit2 = 444000.0,
                        riskRewardRatio = "1:3.8",
                        confluenceScore = 94,
                        timeframeConfluence = "Daily Bullish OB + 4H FVG + 1H CHoCH",
                        smcReasoning = "Sell-side liquidity sweep of previous day low followed by displacement and mitigation of H1 Bullish Order Block.",
                        pattern = "Double Bottom + Liquidity Sweep",
                        isSaved = true
                    )
                )
                signalDao.insertSignal(
                    TradingSignalEntity(
                        symbol = "BOOM1000",
                        direction = "BUY",
                        entryPrice = 9820.0,
                        stopLoss = 9760.0,
                        takeProfit1 = 10050.0,
                        takeProfit2 = 10300.0,
                        riskRewardRatio = "1:4.2",
                        confluenceScore = 89,
                        timeframeConfluence = "4H Demand Zone + M15 Accumulation",
                        smcReasoning = "Institutional accumulation at 4H Order Block. High probability spike expansion window.",
                        pattern = "Bullish Flag Continuation",
                        isSaved = false
                    )
                )
                signalDao.insertSignal(
                    TradingSignalEntity(
                        symbol = "CRASH1000",
                        direction = "SELL",
                        entryPrice = 6140.0,
                        stopLoss = 6185.0,
                        takeProfit1 = 6010.0,
                        takeProfit2 = 5920.0,
                        riskRewardRatio = "1:3.5",
                        confluenceScore = 92,
                        timeframeConfluence = "Daily Supply + 1H Bearish FVG Rejection",
                        smcReasoning = "Price retested 1H Fair Value Gap in premium pricing. Equal Highs buy stops raided before institutional drop.",
                        pattern = "Head & Shoulders Reversal",
                        isSaved = true
                    )
                )
            }

            // Create initial default alert for testing
            alertDao.insertAlert(
                PriceAlertEntity(
                    symbol = "V75",
                    targetPrice = 435000.0,
                    zoneType = "ORDER_BLOCK",
                    condition = "HITS_ZONE",
                    isActive = true,
                    note = "4H Bearish Supply Order Block Re-test"
                )
            )
        }
    }

    suspend fun sendChatMessage(userText: String): String {
        // Save user message to Room
        chatDao.insertMessage(
            ChatMessageEntity(
                role = "user",
                text = userText,
                symbolContext = _selectedSymbol.value
            )
        )

        // Query Gemini with context
        val context = _currentAnalysis.value
        val history = chatDao.getAllMessages()
        val historyList = mutableListOf<Pair<String, String>>()
        // We will collect recent 4 messages for context

        val responseText = geminiService.analyzeMarketQuery(userText, context, historyList)

        // Save model response to Room
        chatDao.insertMessage(
            ChatMessageEntity(
                role = "model",
                text = responseText,
                symbolContext = _selectedSymbol.value
            )
        )

        return responseText
    }

    suspend fun clearChat() {
        chatDao.clearHistory()
    }
}
