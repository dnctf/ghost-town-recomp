package com.overcookedrecomp.app

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small rotating app-private log for runtime, Vulkan and DXVK diagnostics. */
object DiagnosticLog {
    private const val TAG = "GhostTownDiag"
    private const val MAX_BYTES = 1024L * 1024L

    @JvmStatic fun info(context: Context, category: String, message: String) = write(context, "INFO", category, message, null)
    @JvmStatic fun error(context: Context, category: String, message: String, error: Throwable? = null) = write(context, "ERROR", category, message, error)

    private fun write(context: Context, level: String, category: String, message: String, error: Throwable?) {
        val line = buildString {
            append(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date()))
            append(" ").append(level).append("/").append(category).append(": ").append(message)
            if (error != null) append("\n").append(Log.getStackTraceString(error))
            append("\n")
        }
        if (level == "ERROR") Log.e(TAG, "$category: $message", error) else Log.i(TAG, "$category: $message")
        runCatching {
            val dir = File(context.filesDir, "logs")
            if (!dir.exists() && !dir.mkdirs()) return@runCatching
            val file = File(dir, "graphics-runtime.log")
            synchronized(this) {
                if (file.exists() && file.length() + line.length > MAX_BYTES) {
                    val previous = File(dir, "graphics-runtime.log.1")
                    if (previous.exists()) previous.delete()
                    file.renameTo(previous)
                }
                file.appendText(line, Charsets.UTF_8)
            }
        }.onFailure { Log.e(TAG, "Unable to persist diagnostic log", it) }
    }

    @JvmStatic fun readTail(context: Context, maxChars: Int = 30000): String {
        val file = File(context.filesDir, "logs/graphics-runtime.log")
        if (!file.isFile) return "(No persistent app log recorded yet.)"
        return runCatching { file.readText(Charsets.UTF_8).let { if (it.length > maxChars) it.takeLast(maxChars) else it } }
            .getOrElse { "Unable to read persistent log: ${it.message}" }
    }
}
