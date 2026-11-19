package snd.sketchpilot.diagnostics

import android.content.Context
import android.os.Build
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import snd.sketchpilot.core.crypto.SketchwareCrypto
import snd.sketchpilot.core.data.OfflineRepository
import snd.sketchpilot.core.data.Paths
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DiagnosticsKit {
    fun buildZip(context: Context): File? {
        val dir = File(context.getExternalFilesDir(null), "diagnostics")
        dir.mkdirs()
        val zip = File(dir, "sketchpilot-diagnostics.zip")
        return try {
            ZipOutputStream(FileOutputStream(zip, false)).use { out ->
                write(out, "manifest.txt", manifest(context))
                write(out, "app_info.json", appInfo(context).toString(2))
                write(out, "device_info.json", deviceInfo().toString(2))
                write(out, "capability_matrix.json", readText(File("/storage/emulated/0/.sketchware/sketchpilot/capabilities.json")))
                write(out, "hook_breadcrumbs.txt", breadcrumbs())
                write(out, "last500.log", tail500())
                write(out, "self_test.json", selfTest().toString(2))
            }
            zip
        } catch (t: Throwable) {
            null
        }
    }

    private fun manifest(context: Context): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return "SketchPilot ${info.versionName} (${info.versionCode})\ngenerated=${System.currentTimeMillis()}\n"
    }

    private fun appInfo(context: Context): JSONObject {
        val out = JSONObject()
        listOf("pro.sketchware", "pro.sketchware.debug", "com.besome.sketch").forEach { pkg ->
            try {
                val info = context.packageManager.getPackageInfo(pkg, 0)
                out.put("sketchware", JSONObject().put("package", pkg).put("versionCode", info.versionCode).put("versionName", info.versionName))
            } catch (ignored: Throwable) { }
        }
        if (!out.has("sketchware")) out.put("sketchware", "not visible")
        return out
    }

    private fun deviceInfo(): JSONObject = JSONObject()
        .put("android", Build.VERSION.RELEASE)
        .put("sdk", Build.VERSION.SDK_INT)
        .put("device", Build.DEVICE)
        .put("model", Build.MODEL)

    private fun breadcrumbs(): String {
        val lines = readText(File(Environment.getExternalStorageDirectory(), ".sketchware/sketchpilot/sketchpilot.log")).split("\n")
        return lines.filter { "SP-HOOK" in it }.joinToString("\n")
    }

    private fun tail500(): String {
        val lines = readText(File(Environment.getExternalStorageDirectory(), ".sketchware/sketchpilot/sketchpilot.log")).split("\n")
        return lines.takeLast(500).joinToString("\n")
    }

    private fun selfTest(): JSONObject {
        val results = JSONArray()
        val crypto = SketchwareCrypto()
        val sample = "{\"sc_id\":\"test\"}"
        results.put(JSONObject().put("name", "crypto_round_trip").put("ok", crypto.decrypt(crypto.encrypt(sample)) == sample))
        val repo = OfflineRepository(Paths(File("/storage/emulated/0")))
        results.put(JSONObject().put("name", "offline_repo_read").put("ok", runCatching { repo.listProjects() }.isSuccess))
        return JSONObject().put("results", results)
    }

    private fun readText(file: File): String = if (file.isFile) file.readText(Charsets.UTF_8) else ""
    private fun write(out: ZipOutputStream, name: String, text: String) {
        out.putNextEntry(ZipEntry(name))
        out.write(text.toByteArray(StandardCharsets.UTF_8))
        out.closeEntry()
    }
}
