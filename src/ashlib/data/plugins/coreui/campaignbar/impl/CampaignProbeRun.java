package ashlib.data.plugins.coreui.campaignbar.impl;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Runs one read-only capture; missing or ambiguous bars are reported as failures. */
public final class CampaignProbeRun {
    public interface Capture {
        JSONObject capture() throws Exception;
    }

    public interface Sink {
        void event(String message);

        void failure(String operation, Throwable error);
    }

    private CampaignProbeRun() {}

    public static boolean run(
            int sample, String source, String context, Capture capture, Sink sink) {
        String tag = "sample=" + sample + " source=" + source + " context=" + context;
        sink.event("PROBE_STARTED " + tag);
        try {
            JSONObject data = capture.capture();
            data.put("sample", sample).put("source", source).put("build", CampaignBarManager.BUILD);
            JSONObject summary = new JSONObject();
            String[] keys = {
                "schema",
                "status",
                "gameVersion",
                "logicalWidth",
                "logicalHeight",
                "pixelWidth",
                "pixelHeight",
                "uiScale",
                "activeContext",
                "publicCoreTab",
                "showingMenu",
                "showingDialog",
                "barNode",
                "barBounds",
                "refitCargoGap",
                "rightmostButtonRight",
                "screenTrailingSpaceUpperBound",
                "trailingSpaceToObservedObstruction",
                "trailingSpaceToRadarCandidate",
                "radarCandidates",
                "declaredClippingNodes",
                "truncated",
                "safeInsertionCertified",
                "safeInsertionWidth"
            };
            for (String key : keys) {
                if (data.has(key)) {
                    summary.put(key, data.get(key));
                }
            }
            sink.event("SUMMARY " + tag + " data=" + summary.toString());
            JSONArray buttons = data.optJSONArray("buttonsVisualOrder");
            if (buttons != null) {
                for (int i = 0; i < buttons.length(); i++) {
                    sink.event("BUTTON " + tag + " data=" + buttons.get(i));
                }
            }
            // Reassemble the JSON by concatenating chunk data in index order.
            String json = data.toString();
            List<String> chunks = new ArrayList<String>();
            for (int start = 0; start < json.length(); ) {
                int end = Math.min(json.length(), start + 3000);
                if (end < json.length() && Character.isHighSurrogate(json.charAt(end - 1))) {
                    end--;
                }
                chunks.add(json.substring(start, end));
                start = end;
            }
            for (int i = 0; i < chunks.size(); i++) {
                sink.event(
                        "JSON_CHUNK "
                                + tag
                                + " index="
                                + (i + 1)
                                + " total="
                                + chunks.size()
                                + " data="
                                + chunks.get(i));
            }
            String status = data.optString("status", "MISSING_STATUS");
            boolean ok = "SNAPSHOT_ONLY".equals(status);
            sink.event(
                    (ok ? "PROBE_SUCCEEDED " : "PROBE_FAILED ")
                            + tag
                            + " status="
                            + status
                            + " geometryOnly=true safeInsertionCertified=false");
            return ok;
        } catch (Throwable error) {
            sink.event("PROBE_FAILED " + tag + " status=EXCEPTION");
            sink.failure("PROBE_CAPTURE " + tag, error);
            return false;
        }
    }
}
