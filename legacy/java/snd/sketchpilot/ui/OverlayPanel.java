package snd.sketchpilot.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import snd.sketchpilot.core.bridge.BridgeConfig;
import snd.sketchpilot.core.bridge.BridgeMessage;
import snd.sketchpilot.core.bridge.SocketBridgeTransport;
import snd.sketchpilot.core.hook.HookContext;
import snd.sketchpilot.util.Logger;

/**
 * Draggable FAB injected into Sketchware Pro screens, expanding into a quick
 * action panel. All UI work is posted to the main thread with Handler.
 */
public final class OverlayPanel {

    private OverlayPanel() {
    }

    public static void attach(final Activity activity, final HookContext context) {
        if (activity == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                try {
                    build(activity, context);
                } catch (Throwable t) {
                    Logger.hookFailure("OverlayPanel.attach", t);
                }
            }
        });
    }

    private static void build(Activity activity, final HookContext context) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        final Button fab = new Button(activity);
        fab.setText("SP");
        fab.setTextColor(Color.WHITE);
        fab.setBackgroundColor(Color.parseColor("#3F51B5"));
        final FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        fabParams.setMargins(0, 0, 32, 160);

        final LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(Color.parseColor("#F5F5F5"));
        panel.setVisibility(View.GONE);
        final TextView status = new TextView(activity);
        status.setText("SketchPilot — " + context.getPackageName());
        panel.addView(status);

        Button capabilities = new Button(activity);
        capabilities.setText("Capabilities");
        capabilities.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                send("capabilities", status);
            }
        });
        panel.addView(capabilities);

        Button projects = new Button(activity);
        projects.setText("Projects");
        projects.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                send("project.list", status);
            }
        });
        panel.addView(projects);

        final FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        panelParams.setMargins(0, 0, 32, 230);

        content.addView(panel, panelParams);
        content.addView(fab, fabParams);

        final boolean[] dragging = {false};
        final int[] last = {0, 0};
        fab.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                try {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        last[0] = (int) event.getRawX();
                        last[1] = (int) event.getRawY();
                        dragging[0] = false;
                        return true;
                    }
                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        int dx = (int) event.getRawX() - last[0];
                        int dy = (int) event.getRawY() - last[1];
                        if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
                            dragging[0] = true;
                            fabParams.leftMargin += dx;
                            fabParams.topMargin -= dy;
                            fab.setLayoutParams(fabParams);
                        }
                        last[0] = (int) event.getRawX();
                        last[1] = (int) event.getRawY();
                        return true;
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (!dragging[0]) {
                            panel.setVisibility(panel.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                        }
                        return true;
                    }
                } catch (Throwable t) {
                    Logger.hookFailure("OverlayPanel.onTouch", t);
                }
                return false;
            }
        });
    }

    private static void send(final String type, final TextView status) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    final SocketBridgeTransport transport =
                            new SocketBridgeTransport(BridgeConfig.HOST, BridgeConfig.PORT);
                    BridgeMessage reply = transport.call(new BridgeMessage(type, new JSONObject(), "panel"));
                    final String text = type + ": " + reply.getPayload().toString().substring(0,
                            Math.min(400, reply.getPayload().toString().length()));
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            status.setText(text);
                        }
                    });
                } catch (Throwable t) {
                    Logger.hookFailure("OverlayPanel.send." + type, t);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            status.setText("Bridge unavailable — offline mode");
                        }
                    });
                }
            }
        }, "SketchPilotPanel").start();
    }
}
