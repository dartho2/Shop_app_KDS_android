package com.itsorderkds.ui.settings.log

import android.content.Context
import com.itsorderkds.ui.settings.log.LogEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLogSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logDirectory = File(context.getExternalFilesDir(null), "logs")

    // Format: HH:mm:ss.SSS | LEVEL | TAG | Wiadomość
    private val logPattern = """^(\d{2}:\d{2}:\d{2}\.\d{3})\s\|\s(.)\s\|\s(.*?)\s\|\s(.*)""".toRegex()

    suspend fun getLogsForDate(date: String?): List<LogEntry> = withContext(Dispatchers.IO) {
        // ... (reszta kodu bez zmian) ...
        val fileName = "log_$date.txt"
        val logFile = File(logDirectory, fileName)

        if (!logFile.exists()) {
            return@withContext emptyList()
        }

        val parsedLogs = mutableListOf<LogEntry>()
        logFile.forEachLine { line ->
            logPattern.find(line)?.let { matchResult ->
                val (time, levelChar, tag, message) = matchResult.destructured

                val level = when (levelChar) {
                    "E" -> "error"
                    "W" -> "warn"
                    "I" -> "info"
                    "D" -> "debug"
                    else -> "verbose"
                }

                parsedLogs.add(
                    LogEntry(
                        // ✅ POPRAWKA: Format ISO 8601 (T zamiast spacji i Z na końcu)
                        timestamp = "${date}T${time}Z",
                        level = level,
                        message = "[$tag] $message"
                    )
                )
            }
        }
        return@withContext parsedLogs
    }
}