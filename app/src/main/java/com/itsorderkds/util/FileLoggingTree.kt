package com.itsorderkds.util // lub inny pakiet

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FileLoggingTree(private val context: Context) : Timber.DebugTree() {

    private val logDirectory = File(context.getExternalFilesDir(null), "logs")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    init {
        if (!logDirectory.exists()) {
            logDirectory.mkdirs()
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Zapisuj tylko logi o priorytecie INFO lub wyższym
        if (priority < Log.INFO) {
            return
        }

        try {
            val fileName = "log_${dateFormat.format(System.currentTimeMillis())}.txt"
            val logFile = File(logDirectory, fileName)

            val logMessage = buildString {
                append(timeFormat.format(System.currentTimeMillis()))
                append(" | ")
                append(getPriorityString(priority))
                append(" | ")
                append(tag ?: "NoTag")
                append(" | ")
                append(message)
                append("\n") // nowa linia
                if (t != null) {
                    append(Log.getStackTraceString(t))
                    append("\n")
                }
            }

            logFile.appendText(logMessage)

        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Błąd zapisu loga do pliku", e)
        }
    }

    /** Usuwa pliki logów starsze niż 24 godziny. */
    fun deleteOldLogs() {
        try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1) // Ustawia datę na wczoraj
            val yesterday = calendar.time

            logDirectory.listFiles()?.forEach { file ->
                try {
                    val fileDateStr = file.name.substringAfter("log_").substringBefore(".txt")
                    val fileDate = dateFormat.parse(fileDateStr)
                    if (fileDate != null && fileDate.before(yesterday)) {
                        file.delete()
                        Log.i("FileLoggingTree", "Usunięto stary plik loga: ${file.name}")
                    }
                } catch (e: Exception) {
                    // Ignoruj błędy parsowania dla plików o innych nazwach
                }
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Błąd podczas usuwania starych logów", e)
        }
    }

    private fun getPriorityString(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "?"
        }
    }
}