package snd.sketchpilot.project.model;

import org.json.JSONObject;

import snd.sketchpilot.util.JsonUtil;

/** Metadata from .sketchware/mysc/list/<id>/project (evidence E5, E8). */
public final class ProjectInfo {

    private String id = "";
    private String workspaceName = "";
    private String appName = "";
    private String packageName = "";
    private String versionCode = "";
    private String versionName = "";
    private String buildSystem = "";
    private int sketchwareVersion = 0;
    private int colorPrimary = 0;
    private int colorAccent = 0;
    private boolean customIcon = false;

    public static ProjectInfo fromJson(String json) {
        ProjectInfo info = new ProjectInfo();
        JSONObject object = JsonUtil.tryParseObject(json);
        if (object == null) {
            return info;
        }
        info.id = JsonUtil.optString(object, "sc_id", "");
        info.workspaceName = JsonUtil.optString(object, "my_ws_name", "");
        info.appName = JsonUtil.optString(object, "my_app_name", "");
        info.packageName = JsonUtil.optString(object, "my_sc_pkg_name", "");
        info.versionCode = JsonUtil.optString(object, "sc_ver_code", "");
        info.versionName = JsonUtil.optString(object, "sc_ver_name", "");
        info.buildSystem = JsonUtil.optString(object, "build_system", "");
        info.sketchwareVersion = JsonUtil.optInt(object, "sketchware_ver", 0);
        info.colorPrimary = (int) (double) object.optDouble("color_primary", 0.0);
        info.colorAccent = (int) (double) object.optDouble("color_accent", 0.0);
        info.customIcon = JsonUtil.optBoolean(object, "custom_icon", false);
        return info;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getBuildSystem() {
        return buildSystem;
    }

    public int getSketchwareVersion() {
        return sketchwareVersion;
    }

    public int getColorPrimary() {
        return colorPrimary;
    }

    public int getColorAccent() {
        return colorAccent;
    }

    public boolean isCustomIcon() {
        return customIcon;
    }
}
