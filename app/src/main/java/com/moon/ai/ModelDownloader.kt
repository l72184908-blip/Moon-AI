package com.moon.ai

import android.content.Context
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Downloads the Gemma model file with progress and resume support.
 * A partial download is resumed with an HTTP Range request.
 * Runs on Dispatchers.IO; callers must be in a coroutine scope.
 */
class ModelDownloader {

    class AuthException(message: String) : IOException(message)

    suspend fun download(
        context: Context,
        token: String?,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): java.io.File = withContext(Dispatchers.IO) {
        val file = ModelManager.modelFile(context)
        file.parentFile?.mkdirs()

        var existing = if (file.exists()) file.length() else 0L
        if (existing >= ModelManager.EXPECTED_SIZE_BYTES) return@withContext file

        val connection = URL(ModelManager.MODEL_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "MoonAI/1.0")
            if (!token.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (existing > 0) {
                connection.setRequestProperty("Range", "bytes=$existing-")
            }
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw AuthException("HTTP 401 — token missing or license not accepted")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK &&
                connection.responseCode != HttpURLConnection.HTTP_PARTIAL
            ) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            // Server ignored the Range request: start over from zero.
            if (connection.responseCode == HttpURLConnection.HTTP_OK && existing > 0) {
                existing = 0
                file.delete()
                file.createNewFile()
            }

            val total = if (connection.responseCode == HttpURLConnection.HTTP_PARTIAL) {
                existing + connection.contentLengthLong
            } else {
                connection.contentLengthLong
            }

            var downloaded = existing
            var lastReport = System.currentTimeMillis()
            connection.inputStream.use { input ->
                FileOutputStream(file, existing > 0).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        ensureActive()
                        output.write(buffer, 0, read)
                        downloaded += read
                        val now = System.currentTimeMillis()
                        if (now - lastReport > 200) {
                            lastReport = now
                            onProgress(downloaded, total)
                        }
                    }
                }
            }
            onProgress(downloaded, total)

            if (file.length() != ModelManager.EXPECTED_SIZE_BYTES) {
                throw IOException(
                    "Incomplete download: ${file.length()} of " +
                        "${ModelManager.EXPECTED_SIZE_BYTES} bytes"
                )
            }
            file
        } finally {
            connection.disconnect()
        }
    }
}
