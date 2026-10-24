package com.gl1ch5.linuxdesk.log

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Process-wide log: keeps the last [MAX_LINES] lines in memory for the Logs screen
 * and appends everything to `files/logs/app.log`, so the file survives a crash.
 */
object AppLog {
    private const val MAX_LINES = 2000
    private const val MAX_FILE_BYTES = 2L * 1024 * 1024

    private val lines = ArrayDeque<String>(MAX_LINES)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var file: File? = null

    fun init(context: Context) {
        val dir = File(context.filesDir, "logs").apply { mkdirs() }
        val f = File(dir, "app.log")
        // Keep the file bounded: start fresh when it grows past the limit.
        if (f.length() > MAX_FILE_BYTES) f.delete()
        file = f
    }

    fun i(tag: String, message: String) = write('I', tag, message, null)

    fun w(tag: String, message: String) = write('W', tag, message, null)

    fun e(tag: String, message: String, t: Throwable? = null) = write('E', tag, message, t)

    @Synchronized
    fun snapshot(maxLines: Int = MAX_LINES): String = lines.takeLast(maxLines).joinToString("\n")

    @Synchronized
    private fun write(level: Char, tag: String, message: String, t: Throwable?) {
        val line = "${timeFormat.format(Date())} $level/$tag: $message"
        when (level) {
            'E' -> Log.e(tag, message, t)
            'W' -> Log.w(tag, message)
            else -> Log.i(tag, message)
        }
        if (lines.size == MAX_LINES) lines.removeFirst()
        lines.addLast(line)
        t?.let { lines.addLast(Log.getStackTraceString(it).trimEnd()) }
        file?.let { f ->
            runCatching {
                f.appendText(line + "\n")
                t?.let { f.appendText(Log.getStackTraceString(it) + "\n") }
            }
        }
    }
}
