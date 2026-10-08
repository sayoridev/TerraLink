package com.terralink.iotfarm.data.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val type: LogType,
    val message: String
) {
    enum class LogType {
        TCP_RX, TCP_TX, UDP, SYSTEM, AUTOMATION
    }
}

@Singleton
class DebugLogManager @Inject constructor() {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(type: LogEntry.LogType, message: String) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            type = type,
            message = message
        )
        _logs.update { current ->
            (listOf(entry) + current).takeLast(200)
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
