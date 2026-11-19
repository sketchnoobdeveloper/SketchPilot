package snd.sketchpilot.core.bridge;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.core.hook.CapabilityMatrix;
import snd.sketchpilot.project.LiveRepository;
import snd.sketchpilot.project.model.BlockNode;
import snd.sketchpilot.project.model.ProjectInfo;
import snd.sketchpilot.project.model.ViewNode;

/** Registers the read-only bridge calls exposed to the companion app and panel. */
public final class BridgeHandlers {

    private BridgeHandlers() {
    }

    public static void registerAll(LiveBridgeServer server, final LiveRepository repository,
                                   final CapabilityMatrix matrix) {
        server.register(new BridgeHandler() {
            public String getType() {
                return "ping";
            }

            public BridgeMessage handle(BridgeMessage message) {
                JSONObject payload = new JSONObject();
                try {
                    payload.put("ok", true);
                    payload.put("live", repository.hasLiveData());
                } catch (Throwable ignored) {
                    // safe
                }
                return response("ping", payload, message);
            }
        });

        server.register(new BridgeHandler() {
            public String getType() {
                return "capabilities";
            }

            public BridgeMessage handle(BridgeMessage message) {
                JSONObject payload = new JSONObject();
                try {
                    payload.put("matrix", new JSONArray(matrix.toJson()));
                } catch (Throwable ignored) {
                    // safe
                }
                return response("capabilities", payload, message);
            }
        });

        server.register(new BridgeHandler() {
            public String getType() {
                return "project.list";
            }

            public BridgeMessage handle(BridgeMessage message) throws BridgeException {
                JSONObject payload = new JSONObject();
                try {
                    java.util.List<ProjectInfo> projects = repository.listProjects();
                    JSONArray array = new JSONArray();
                    for (int i = 0; i < projects.size(); i++) {
                        ProjectInfo info = projects.get(i);
                        JSONObject row = new JSONObject();
                        row.put("id", info.getId());
                        row.put("name", info.getWorkspaceName());
                        row.put("package", info.getPackageName());
                        array.put(row);
                    }
                    payload.put("projects", array);
                } catch (Throwable t) {
                    throw new BridgeException("project.list failed", t);
                }
                return response("project.list", payload, message);
            }
        });

        server.register(new BridgeHandler() {
            public String getType() {
                return "view.get_tree";
            }

            public BridgeMessage handle(BridgeMessage message) throws BridgeException {
                JSONObject payload = new JSONObject();
                try {
                    String scId = message.getPayload().optString("scId", "");
                    String layout = message.getPayload().optString("layout", "");
                    java.util.List<ViewNode> views = repository.getViews(scId, layout);
                    JSONArray array = new JSONArray();
                    for (int i = 0; i < views.size(); i++) {
                        array.put(views.get(i).getRaw());
                    }
                    payload.put("views", array);
                } catch (Throwable t) {
                    throw new BridgeException("view.get_tree failed", t);
                }
                return response("view.get_tree", payload, message);
            }
        });

        server.register(new BridgeHandler() {
            public String getType() {
                return "logic.get";
            }

            public BridgeMessage handle(BridgeMessage message) throws BridgeException {
                JSONObject payload = new JSONObject();
                try {
                    String scId = message.getPayload().optString("scId", "");
                    java.util.Map<String, java.util.List<BlockNode>> logic = repository.getLogic(scId);
                    JSONObject sections = new JSONObject();
                    java.util.Iterator<String> keys = logic.keySet().iterator();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        JSONArray blocks = new JSONArray();
                        java.util.List<BlockNode> list = logic.get(key);
                        for (int i = 0; i < list.size(); i++) {
                            blocks.put(list.get(i).getRaw());
                        }
                        sections.put(key, blocks);
                    }
                    payload.put("sections", sections);
                } catch (Throwable t) {
                    throw new BridgeException("logic.get failed", t);
                }
                return response("logic.get", payload, message);
            }
        });
    }

    private static BridgeMessage response(String type, JSONObject payload, BridgeMessage request) {
        return new BridgeMessage("response:" + type, payload, request.getRequestId());
    }
}
