package snd.sketchpilot.log

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object KotlinLogger {
    private val lock = Any()
    private val file = File(Environment.getExternalStorageDirectory(), ".sketchware/sketchpilot/sketchpilot.log")

    fun d(tag: String, message: String) = write("D", tag, message, null)
    fun w(tag: String, message: String) = write("W", tag, message, null)
    fun e(tag: String, message: String, error: Throwable? = null) = write("E", tag, message, error)

    fun hookFailure(name: String, error: Throwable) = e("Hook", "hook failed: $name", error)

    fun breadcrumb(name: String, ok: Boolean) {
        d("Hook", "SP-HOOK $name ${if (ok) "OK" else "FAIL"}")
    }

    private fun write(level: String, tag: String, message: String, error: Throwable?) {
        val line = "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())} $level/$tag: $message"
        try { Log.println(Log.DEBUG, "SketchPilot", line) } catch (ignored: Throwable) { }
        synchronized(lock) {
            try {
                file.parentFile?.mkdirs()
                FileWriter(file, true).use { writer ->
                    writer.write(line); writer.newLine()
                    if (error != null) error.printStackTrace(writer)
                }
            } catch (ignored: Throwable) { }
        }
    }
}
