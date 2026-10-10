package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Candle
import com.example.data.model.ChatMessageEntity
import com.example.data.model.MultiTimeframeAnalysis
import com.example.data.model.PriceAlertEntity
import com.example.data.model.SyntheticIndex
import com.example.data.model.TimeFrame
import com.example.data.model.TradingSignalEntity
import com.example.data.remote.GeminiChatService
import com.example.data.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val geminiService = GeminiChatService()
    val repository = MarketRepository(
        alertDao = database.alertDao(),
        signalDao = database.signalDao(),
        chatDao = database.chatDao(),
        geminiService = geminiService
    )

    val indices: StateFlow<List<SyntheticIndex>> = repository.indices
    val selectedSymbol: StateFlow<String> = repository.selectedSymbol
    val selectedTimeframe: StateFlow<TimeFrame> = repository.selectedTimeframe
    val currentAnalysis: StateFlow<MultiTimeframeAnalysis?> = repository.currentAnalysis

    val alerts: StateFlow<List<PriceAlertEntity>> = repository.allAlerts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val signals: StateFlow<List<TradingSignalEntity>> = repository.allSignals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isAnalyzingChat = MutableStateFlow(false)
    val isAnalyzingChat: StateFlow<Boolean> = _isAnalyzingChat.asStateFlow()

    private val _alertBanner = MutableStateFlow<PriceAlertEntity?>(null)
    val alertBanner: StateFlow<PriceAlertEntity?> = _alertBanner.asStateFlow()

    // Interactive chart overlay options
    private val _showOrderBlocks = MutableStateFlow(true)
    val showOrderBlocks: StateFlow<Boolean> = _showOrderBlocks.asStateFlow()

    private val _showFvg = MutableStateFlow(true)
    val showFvg: StateFlow<Boolean> = _showFvg.asStateFlow()

    private val _showLiquidity = MutableStateFlow(true)
    val showLiquidity: StateFlow<Boolean> = _showLiquidity.asStateFlow()

    private val _showPatterns = MutableStateFlow(true)
    val showPatterns: StateFlow<Boolean> = _showPatterns.asStateFlow()

    init {
        // Collect real-time triggered alert notifications
        viewModelScope.launch {
            repository.alertNotification.collect { alert ->
                _alertBanner.value = alert
            }
        }
    }

    fun selectSymbol(symbol: String) {
        repository.selectSymbol(symbol)
    }

    fun selectTimeframe(tf: TimeFrame) {
        repository.selectTimeframe(tf)
    }

    fun toggleOverlay(type: String) {
        when (type) {
            "OB" -> _showOrderBlocks.value = !_showOrderBlocks.value
            "FVG" -> _showFvg.value = !_showFvg.value
            "LIQ" -> _showLiquidity.value = !_showLiquidity.value
            "PAT" -> _showPatterns.value = !_showPatterns.value
        }
    }

    fun getCurrentCandles(): List<Candle> {
        return repository.getCandles(selectedSymbol.value, selectedTimeframe.value)
    }

    fun dismissAlertBanner() {
        _alertBanner.value = null
    }

    fun createAlert(targetPrice: Double, zoneType: String, condition: String, note: String) {
        viewModelScope.launch {
            repository.createAlert(
                symbol = selectedSymbol.value,
                targetPrice = targetPrice,
                zoneType = zoneType,
                condition = condition,
                note = note
            )
        }
    }

    fun toggleAlert(alert: PriceAlertEntity) {
        viewModelScope.launch {
            repository.toggleAlert(alert)
        }
    }

    fun deleteAlert(alert: PriceAlertEntity) {
        viewModelScope.launch {
            repository.deleteAlert(alert)
        }
    }

    fun clearTriggeredAlerts() {
        viewModelScope.launch {
            repository.clearTriggeredAlerts()
        }
    }

    fun toggleSaveSignal(signal: TradingSignalEntity) {
        viewModelScope.launch {
            repository.toggleSaveSignal(signal)
        }
    }

    fun generateSignal() {
        viewModelScope.launch {
            repository.generateNewSignalForSymbol(selectedSymbol.value)
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isAnalyzingChat.value = true
            try {
                repository.sendChatMessage(text)
            } finally {
                _isAnalyzingChat.value = false
            }
        }
    }

    fun quickAnalyze(queryType: String) {
        val symbol = selectedSymbol.value
        val prompt = when (queryType) {
            "STRUCTURE" -> "Analyze current market structure (BOS & CHoCH) and order blocks for $symbol."
            "CONFLUENCE" -> "Give me higher timeframe confluence (Monthly, Weekly, Daily, 4H, 1H) for $symbol."
            "LIQUIDITY" -> "Where are the key liquidity pools, equal highs/lows (EQH/EQL), and fair value gaps (FVG) on $symbol?"
            "ENTRY" -> "Generate high-accuracy institutional entry setup with Stop Loss, TP1, and TP2 for $symbol."
            "PATTERNS" -> "Identify any active chart patterns (Head and Shoulders, Double Top/Bottom, Bullish/Bearish Flags) on $symbol."
            else -> "Explain institutional order flow for $symbol."
        }
        sendChatMessage(prompt)
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }
}
