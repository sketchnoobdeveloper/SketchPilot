package snd.sketchpilot.core.build

import org.json.JSONArray
import org.json.JSONObject

object BuildDiagnostics {
    fun parse(lines: List<String>): JSONArray {
        val result = JSONArray()
        lines.forEachIndexed { index, line ->
            val lower = line.lowercase()
            val isError = listOf("error:", " error ", "failed", "exception", "cannot find symbol", "no resource found", "duplicate class")
                .any { it in lower }
            if (!isError) return@forEachIndexed
            result.put(JSONObject()
                .put("severity", if ("exception" in lower || "fatal" in lower) "fatal" else "error")
                .put("stage", when {
                    "aapt" in lower || "resource" in lower -> "resources"
                    "eclipse" in lower || "java" in lower || "cannot find symbol" in lower -> "java"
                    "dex" in lower || "d8" in lower -> "dex"
                    "r8" in lower || "proguard" in lower -> "shrink"
                    else -> "unknown"
                })
                .put("message", line.take(600))
                .put("line", index + 1))
        }
        return result
    }
}
