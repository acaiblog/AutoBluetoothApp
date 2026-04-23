package com.bluetooth.control

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO, SUCCESS, WARNING, ERROR
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
)

object LogManager {

    private const val MAX_MEMORY_LOGS = 500
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun i(tag: String, message: String) = append(LogLevel.INFO, tag, message)
    fun s(tag: String, message: String) = append(LogLevel.SUCCESS, tag, message)
    fun w(tag: String, message: String) = append(LogLevel.WARNING, tag, message)
    fun e(tag: String, message: String) = append(LogLevel.ERROR, tag, message)

    private fun append(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message
        )
        val current = _logs.value
        _logs.value = if (current.size >= MAX_MEMORY_LOGS) {
            current.drop(1) + entry
        } else {
            current + entry
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }

    /** 导出日志为文本 */
    fun export(): String {
        return _logs.value.joinToString("\n") { entry ->
            val levelStr = when (entry.level) {
                LogLevel.INFO    -> "I"
                LogLevel.SUCCESS -> "S"
                LogLevel.WARNING -> "W"
                LogLevel.ERROR   -> "E"
            }
            "[${fileDateFormat.format(Date(entry.timestamp))}] $levelStr/${entry.tag}: ${entry.message}"
        }
    }

    /** 保存日志到文件 */
    fun saveToFile(context: Context): String? {
        return try {
            val fileName = "bluetooth_log_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val file = java.io.File(context.getExternalFilesDir(null), fileName)
            file.writeText(export())
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
