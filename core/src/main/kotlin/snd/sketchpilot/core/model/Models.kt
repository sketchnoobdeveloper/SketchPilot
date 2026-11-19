package snd.sketchpilot.core.model

import org.json.JSONObject

data class ProjectInfo(
    val id: String = "",
    val workspaceName: String = "",
    val appName: String = "",
    val packageName: String = "",
    val versionCode: String = "",
    val versionName: String = "",
    val buildSystem: String = "",
) {
    companion object {
        fun fromJson(json: String): ProjectInfo = try {
            val o = JSONObject(json)
            ProjectInfo(
                id = o.optString("sc_id"),
                workspaceName = o.optString("my_ws_name"),
                appName = o.optString("my_app_name"),
                packageName = o.optString("my_sc_pkg_name"),
                versionCode = o.optString("sc_ver_code"),
                versionName = o.optString("sc_ver_name"),
                buildSystem = o.optString("build_system"),
            )
        } catch (t: Throwable) {
            ProjectInfo()
        }
    }
}

data class ViewNode(val raw: JSONObject) {
    val id: String get() = raw.optString("id")
    val type: Int get() = raw.optInt("type", -1)
    val parent: String get() = raw.optString("parent")
}

data class BlockNode(val raw: JSONObject) {
    val opCode: String get() = raw.optString("opCode")
    val spec: String get() = raw.optString("spec")
}
