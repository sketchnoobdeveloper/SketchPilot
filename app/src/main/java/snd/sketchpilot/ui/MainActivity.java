package snd.sketchpilot.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import snd.sketchpilot.project.OfflineRepository;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.project.model.ProjectInfo;

/** Companion app entry point (P1 shell; provider/MCP screens arrive in P4/P6). */
public final class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = 24;
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("SketchPilot — Phase 1");
        title.setTextSize(20f);
        root.addView(title);

        TextView summary = new TextView(this);
        summary.setText("Offline projects discovered: " + countProjects()
                + "\nSketchware: " + installedSketchware());
        root.addView(summary);

        final TextView status = new TextView(this);
        status.setText("Ready.");
        root.addView(status);

        Button diagnostics = new Button(this);
        diagnostics.setText("Run diagnostics");
        diagnostics.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                runDiagnostics(status);
            }
        });
        root.addView(diagnostics);

        Button capabilities = new Button(this);
        capabilities.setText("Capability Matrix");
        capabilities.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CapabilityActivity.class));
            }
        });
        root.addView(capabilities);

        setContentView(root);
    }

    private void runDiagnostics(final TextView status) {
        status.setText("Building diagnostics zip...");
        new Thread(new Runnable() {
            public void run() {
                final java.io.File zip = snd.sketchpilot.diagnostics.DiagnosticsKit.buildZip(MainActivity.this);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        if (zip == null) {
                            status.setText("Diagnostics failed - see sketchpilot.log");
                        } else {
                            status.setText("Diagnostics zip: " + zip.getAbsolutePath());
                        }
                    }
                });
            }
        }, "SketchPilotDiagnostics").start();
    }

    private String installedSketchware() {
        org.json.JSONObject info = snd.sketchpilot.diagnostics.InstalledApps.getSketchwareInfo(this);
        org.json.JSONObject found = info.optJSONObject("installed");
        if (found == null) {
            return "not visible";
        }
        return found.optString("package") + " " + found.optString("versionName")
                + " (" + found.optInt("versionCode") + ")";
    }

    private static int countProjects() {
        try {
            OfflineRepository repo = new OfflineRepository(new SketchwarePaths(new File("/storage/emulated/0")),
                    new SketchwareCrypto());
            List<ProjectInfo> projects = repo.listProjects();
            return projects.size();
        } catch (Throwable t) {
            return 0;
        }
    }
}
