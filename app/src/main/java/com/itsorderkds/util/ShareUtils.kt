package com.itsorderkds.util

// w pliku np. ShareUtils.kt
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

fun shareLogs(context: Context) {
    val logDir = File(context.getExternalFilesDir(null), "logs")
    if (!logDir.exists() || logDir.listFiles()?.isEmpty() == true) {
        Toast.makeText(context, "Brak logów do wysłania.", Toast.LENGTH_SHORT).show()
        return
    }

    // 1. Spakuj pliki do ZIP
    val zipFile = File(context.cacheDir, "logs.zip")
    if (zipFile.exists()) zipFile.delete()

    ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
        logDir.listFiles()?.forEach { file ->
            zipOut.putNextEntry(java.util.zip.ZipEntry(file.name))
            file.inputStream().use { it.copyTo(zipOut) }
            zipOut.closeEntry()
        }
    }

    // 2. Udostępnij plik ZIP za pomocą FileProvider
    val authority = "${context.packageName}.provider"
    val contentUri = FileProvider.getUriForFile(context, authority, zipFile)

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_SUBJECT, "Logi aplikacji - ${context.packageName}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Wyślij logi za pomocą..."))
}