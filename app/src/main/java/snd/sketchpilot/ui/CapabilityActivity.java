package snd.sketchpilot.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import snd.sketchpilot.project.crypto.SketchwareCrypto;

/** Displays the capability matrix snapshot produced by the hook (or an empty state). */
public final class CapabilityActivity extends Activity {

    private static final File SNAPSHOT =
            new File("/storage/emulated/0/.sketchware/sketchpilot/capabilities.json");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView list = new ListView(this);
        List<String> rows = readRows();
        list.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, rows));
        setContentView(list);
        setTitle("SketchPilot Capability Matrix (" + rows.size() + ")");
    }

    private static List<String> readRows() {
        List<String> rows = new ArrayList<String>();
        if (!SNAPSHOT.isFile()) {
            rows.add("No snapshot yet — open Sketchware Pro with the module enabled.");
            return rows;
        }
        try {
            String json = new String(SketchwareCrypto.readAll(new java.io.FileInputStream(SNAPSHOT)), "UTF-8");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject row = array.getJSONObject(i);
                rows.add(row.optString("label") + "  [" + row.optString("mode") + "] "
                        + row.optString("detail"));
            }
        } catch (Throwable t) {
            rows.add("Snapshot unreadable: " + t.getClass().getSimpleName());
        }
        return rows;
    }
}
