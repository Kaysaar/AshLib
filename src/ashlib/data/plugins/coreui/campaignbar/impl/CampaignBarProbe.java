package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.FieldRef;
import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Bounded, read-only UI snapshot that retains no component references. */
public final class CampaignBarProbe {
    private static final int MAX_NODES = 512, MAX_DEPTH = 8;

    private static final class Node {
        final UIComponentAPI component;
        final Node parent;
        final String id;
        final int depth;

        Node(UIComponentAPI c, Node p, String id) {
            component = c;
            parent = p;
            this.id = id;
            depth = p == null ? 0 : p.depth + 1;
        }
    }

    public JSONObject capture(
            UIPanelAPI core,
            String context,
            String version,
            float logicalW,
            float logicalH,
            float pixelW,
            float pixelH,
            float scale)
            throws Exception {
        return capture(core, context, version, logicalW, logicalH, pixelW, pixelH, scale, true);
    }

    /** Captures geometry without inspecting each component's fields. */
    public JSONObject captureLayout(
            UIPanelAPI core,
            String context,
            String version,
            float logicalW,
            float logicalH,
            float pixelW,
            float pixelH,
            float scale)
            throws Exception {
        return capture(core, context, version, logicalW, logicalH, pixelW, pixelH, scale, false);
    }

    /** Captures only the bar subtree; cannot validate expansion beyond its inspected bounds. */
    public JSONObject captureBarLayout(
            UIPanelAPI core,
            String context,
            String version,
            float logicalW,
            float logicalH,
            float pixelW,
            float pixelH,
            float scale)
            throws Exception {
        return capture(
                core, context, version, logicalW, logicalH, pixelW, pixelH, scale, false, true);
    }

    private JSONObject capture(
            UIPanelAPI core,
            String context,
            String version,
            float logicalW,
            float logicalH,
            float pixelW,
            float pixelH,
            float scale,
            boolean diagnostics)
            throws Exception {
        return capture(
                core,
                context,
                version,
                logicalW,
                logicalH,
                pixelW,
                pixelH,
                scale,
                diagnostics,
                false);
    }

    private JSONObject capture(
            UIPanelAPI core,
            String context,
            String version,
            float logicalW,
            float logicalH,
            float pixelW,
            float pixelH,
            float scale,
            boolean diagnostics,
            boolean fixed)
            throws Exception {
        JSONObject report = new JSONObject();
        report.put("snapshotScope", fixed ? "NATIVE_BAR_SUBTREE" : "WHOLE_CORE");
        report.put("captureMode", diagnostics ? "DIAGNOSTIC_INVENTORY" : "FUNCTIONAL_GEOMETRY");
        report.put("schema", "ashlib.campaignbar.probe.v2")
                .put("context", context)
                .put("gameVersion", version);
        report.put(
                "coordinateSystem",
                "UI logical units, screen origin bottom-left; raw native positions, not screenshot pixels");
        report.put("logicalWidth", logicalW)
                .put("logicalHeight", logicalH)
                .put("pixelWidth", pixelW)
                .put("pixelHeight", pixelH)
                .put("uiScale", scale);
        report.put("safeInsertionCertified", false);
        report.put("safeInsertionWidth", JSONObject.NULL);
        JSONArray limitations = new JSONArray();
        limitations.put("Geometry is a snapshot; no UI mutations or insertion are performed.");
        limitations.put(
                "GPU scissor, animated sprite footprints and unknown mod overlays are not certified by these bounds.");
        report.put("limitations", limitations);
        if (core == null) {
            report.put("status", "NO_CORE");
            return report;
        }
        Object targetBar = fixed ? CampaignUiAccess.read(core, "getButtons") : null;
        if (fixed) {
            limitations.put(
                    "Only the bar subtree and direct Core siblings are inventoried; no expanded footprint is authorized.");
        }

        List<Node> nodes = new ArrayList<Node>();
        IdentityHashMap<Object, Node> byComponent = new IdentityHashMap<Object, Node>();
        IdentityHashMap<Object, Node> byPosition = new IdentityHashMap<Object, Node>();
        Node root = new Node(core, null, "root");
        nodes.add(root);
        byComponent.put(core, root);
        boolean truncated = false, nodeLimit = false, depthLimit = false;
        int observedDepth = 0;
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            observedDepth = Math.max(observedDepth, n.depth);
            byPosition.put(n.component.getPosition(), n);
            if (!(n.component instanceof UIPanelAPI)) {
                continue;
            }
            if (fixed && n != root) {
                boolean inBar = false;
                for (Node p = n; p != null; p = p.parent) {
                    if (p.component == targetBar) {
                        inBar = true;
                    }
                }
                if (!inBar) {
                    continue;
                }
            }
            List<?> children;
            try {
                children = CampaignUiAccess.children(n.component);
            } catch (Exception error) {
                truncated = true;
                limitations.put(n.id + "/children: " + CampaignDiagnostics.trace(error));
                continue;
            }
            if (n.depth >= MAX_DEPTH) {
                if (!children.isEmpty()) {
                    truncated = true;
                    depthLimit = true;
                }
                continue;
            }
            int childIndex = 0;
            for (Object child : children) {
                String id = n.id + "/" + childIndex++;
                if (!(child instanceof UIComponentAPI) || byComponent.containsKey(child)) {
                    continue;
                }
                if (nodes.size() >= MAX_NODES) {
                    truncated = true;
                    nodeLimit = true;
                    break;
                }
                Node next = new Node((UIComponentAPI) child, n, id);
                nodes.add(next);
                byComponent.put(child, next);
            }
        }
        report.put("truncated", truncated);
        report.put("visitedNodes", nodes.size())
                .put("observedDepth", observedDepth)
                .put("nodeLimitReached", nodeLimit)
                .put("depthLimitReached", depthLimit);
        if (truncated) {
            limitations.put(
                    "Tree limit reached; absence of additional components cannot be inferred.");
        }

        Node bar = null;
        Map<?, ?> registry = null;
        int matches = 0;
        for (Node n : nodes) {
            MethodRef method = CampaignUiAccess.method(n.component.getClass(), "getButtons");
            if (method == null || !Map.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            Map<?, ?> candidate = (Map<?, ?>) method.invoke(n.component);
            if (candidate == null
                    || !(candidate.get(CoreUITabId.REFIT) instanceof ButtonAPI)
                    || !(candidate.get(CoreUITabId.CARGO) instanceof ButtonAPI)) {
                continue;
            }
            matches++;
            bar = n;
            registry = candidate;
        }
        report.put("barCandidates", matches);
        JSONArray componentData = new JSONArray();
        if (diagnostics) {
            for (Node n : nodes) {
                componentData.put(describe(n, byPosition, limitations));
            }
        }
        report.put("components", componentData);
        if (matches != 1) {
            report.put("status", matches == 0 ? "BAR_NOT_FOUND" : "AMBIGUOUS_BAR");
            return report;
        }
        report.put("barNode", bar.id).put("barBounds", rect(bar.component.getPosition()));
        final IdentityHashMap<Object, Object> ids = new IdentityHashMap<Object, Object>();
        List<ButtonAPI> buttons = new ArrayList<ButtonAPI>();
        for (Map.Entry<?, ?> entry : registry.entrySet()) {
            if (entry.getValue() instanceof ButtonAPI) {
                if (ids.containsKey(entry.getValue())) {
                    throw new IllegalStateException("Duplicate registry identity");
                }
                ids.put(entry.getValue(), entry.getKey());
                buttons.add((ButtonAPI) entry.getValue());
            }
        }
        Collections.sort(
                buttons,
                new Comparator<ButtonAPI>() {
                    public int compare(ButtonAPI a, ButtonAPI b) {
                        int x = Float.compare(a.getPosition().getX(), b.getPosition().getX());
                        return x != 0
                                ? x
                                : Float.compare(a.getPosition().getY(), b.getPosition().getY());
                    }
                });
        JSONArray visual = new JSONArray();
        float right = bar.component.getPosition().getX();
        int order = 0;
        for (ButtonAPI button : buttons) {
            Node node = byComponent.get(button);
            JSONObject item = new JSONObject();
            item.put("order", order++)
                    .put("id", id(ids.get(button)))
                    .put("label", button.getText())
                    .put("enabled", button.isEnabled())
                    .put("node", node == null ? JSONObject.NULL : node.id)
                    .put("bounds", rect(button.getPosition()));
            PositionAPI pos = button.getPosition();
            item.put(
                            "anchorNode",
                            reference(optional(pos, "getBase", limitations, "button"), byPosition))
                    .put(
                            "positionParentNode",
                            reference(
                                    optional(pos, "getParent", limitations, "button"), byPosition))
                    .put(
                            "xAlignOffset",
                            json(optional(pos, "getXAlignOffset", limitations, "button")))
                    .put(
                            "yAlignOffset",
                            json(optional(pos, "getYAlignOffset", limitations, "button")))
                    .put(
                            "declaredAncestorClipModel",
                            node == null
                                    ? JSONObject.NULL
                                    : clipModel(node, logicalW, logicalH, limitations));
            visual.put(item);
            right = Math.max(right, button.getPosition().getX() + button.getPosition().getWidth());
        }
        report.put("buttonsVisualOrder", visual);
        ButtonAPI refit = (ButtonAPI) registry.get(CoreUITabId.REFIT),
                cargo = (ButtonAPI) registry.get(CoreUITabId.CARGO);
        float gap =
                cargo.getPosition().getX()
                        - refit.getPosition().getX()
                        - refit.getPosition().getWidth();
        report.put("refitCargoGap", gap).put("rightmostButtonRight", right);
        PositionAPI rp = core.getPosition(), bp = bar.component.getPosition();
        float candidateRight = Math.min(rp.getX() + rp.getWidth(), logicalW);
        report.put("screenTrailingSpaceUpperBound", Math.max(0, candidateRight - right));
        JSONArray obstacles = new JSONArray(),
                radarCandidates = new JSONArray(),
                declaredClips = new JSONArray();
        Float radarLeft = null;
        for (Node n : nodes) {
            PositionAPI position = n.component.getPosition();
            if (position == null) {
                continue;
            }
            Object clip = optional(n.component, "isClipping", limitations, n.id);
            if (Boolean.TRUE.equals(clip)) {
                declaredClips.put(new JSONObject().put("node", n.id).put("bounds", rect(position)));
            }
            if (n == root || n == bar || descendant(n, bar) || descendant(bar, n)) {
                continue;
            }
            Object opacity = optional(n.component, "getOpacity", limitations, n.id);
            if (opacity instanceof Number && ((Number) opacity).floatValue() <= 0) {
                continue;
            }
            float rightMargin = rp.getX() + rp.getWidth() - (position.getX() + position.getWidth());
            float bottomMargin = position.getY() - rp.getY();
            if (position.getWidth() >= 90
                    && Math.abs(position.getWidth() - position.getHeight()) < 3
                    && rightMargin >= 0
                    && rightMargin <= 64
                    && bottomMargin >= 0
                    && bottomMargin <= 40) {
                radarCandidates.put(
                        new JSONObject()
                                .put("node", n.id)
                                .put("bounds", rect(position))
                                .put("confidence", "HEURISTIC_ONLY")
                                .put("distanceFromRightmostButton", position.getX() - right)
                                .put("rightMargin", rightMargin)
                                .put("bottomMargin", bottomMargin));
                radarLeft =
                        radarLeft == null ? position.getX() : Math.min(radarLeft, position.getX());
            }
            boolean sameBand =
                    position.getY() < bp.getY() + bp.getHeight()
                            && position.getY() + position.getHeight() > bp.getY();
            if (sameBand
                    && position.getX() + position.getWidth() > right
                    && position.getWidth() > 0) {
                obstacles.put(new JSONObject().put("node", n.id).put("bounds", rect(position)));
                candidateRight = Math.min(candidateRight, Math.max(right, position.getX()));
            }
        }
        report.put("radarCandidates", radarCandidates)
                .put("bottomBandObstructions", obstacles)
                .put("declaredClippingNodes", declaredClips);
        report.put(
                "trailingSpaceToRadarCandidate",
                radarLeft == null ? JSONObject.NULL : Math.max(0, radarLeft - right));
        report.put("barDeclaredAncestorClipModel", clipModel(bar, logicalW, logicalH, limitations));
        report.put("trailingSpaceToObservedObstruction", Math.max(0, candidateRight - right));
        limitations.put(
                "Radar classification is heuristic. Frame/sprite components are inventoried, not semantically certified.");
        report.put("status", "SNAPSHOT_ONLY");
        return report;
    }

    /** Intersects declared bounds; does not inspect GL clipping or actual sprite extents. */
    private static JSONObject clipModel(
            Node node, float screenW, float screenH, JSONArray limitations) throws Exception {
        float left = 0, bottom = 0, right = screenW, top = screenH;
        JSONArray ancestors = new JSONArray();
        boolean unknown = false;
        for (Node n = node.parent; n != null; n = n.parent) {
            Object clip = optional(n.component, "isClipping", limitations, n.id);
            if (!(clip instanceof Boolean)) {
                unknown = true;
                continue;
            }
            if (!Boolean.TRUE.equals(clip)) {
                continue;
            }
            PositionAPI position = n.component.getPosition();
            if (position == null) {
                unknown = true;
                continue;
            }
            ancestors.put(n.id);
            left = Math.max(left, position.getX());
            bottom = Math.max(bottom, position.getY());
            right = Math.min(right, position.getX() + position.getWidth());
            top = Math.min(top, position.getY() + position.getHeight());
        }
        return new JSONObject()
                .put("ancestorNodes", ancestors)
                .put("unknownAncestorCapability", unknown)
                .put("x", left)
                .put("y", bottom)
                .put("width", Math.max(0, right - left))
                .put("height", Math.max(0, top - bottom))
                .put("gpuScissorVerified", false);
    }

    private static JSONObject describe(
            Node n, IdentityHashMap<Object, Node> positions, JSONArray limitations)
            throws Exception {
        JSONObject data = new JSONObject();
        PositionAPI position = n.component.getPosition();
        data.put("node", n.id)
                .put("parent", n.parent == null ? JSONObject.NULL : n.parent.id)
                .put("type", n.component.getClass().getName())
                .put("bounds", rect(position));
        data.put("clipping", json(optional(n.component, "isClipping", limitations, n.id)));
        data.put("opacity", json(optional(n.component, "getOpacity", limitations, n.id)));
        if (CampaignUiAccess.method(n.component.getClass(), "getSprite") != null) {
            data.put("sprite", CampaignFrameDiagnostics.sprite(n.component));
        }
        if (position == null) {
            return data;
        }
        Object base = optional(position, "getBase", limitations, n.id);
        Object parent = optional(position, "getParent", limitations, n.id);
        data.put("anchorNode", reference(base, positions))
                .put("positionParentNode", reference(parent, positions));
        data.put("xAlignOffset", json(optional(position, "getXAlignOffset", limitations, n.id)))
                .put(
                        "yAlignOffset",
                        json(optional(position, "getYAlignOffset", limitations, n.id)));
        JSONArray fields = new JSONArray();
        for (FieldRef field : CampaignUiAccess.fields(position.getClass())) {
            if (!field.getType().isPrimitive()
                    && !PositionAPI.class.isAssignableFrom(field.getType())) {
                continue;
            }
            JSONObject entry =
                    new JSONObject()
                            .put("field", field.getName())
                            .put("declaringType", field.getDeclaringClass().getName());
            try {
                Object value = CampaignUiAccess.value(field, position);
                entry.put(
                        "value",
                        value instanceof PositionAPI ? reference(value, positions) : json(value));
            } catch (Exception error) {
                entry.put("unavailable", CampaignDiagnostics.trace(error));
            }
            fields.put(entry);
        }
        data.put("rawPositionFields", fields);
        return data;
    }

    private static Object optional(Object o, String name, JSONArray limitations, String node) {
        try {
            return CampaignUiAccess.read(o, name);
        } catch (Exception error) {
            limitations.put(node + "." + name + ": " + CampaignDiagnostics.trace(error));
            return null;
        }
    }

    private static Object json(Object o) {
        return o == null ? JSONObject.NULL : o;
    }

    private static Object reference(Object o, IdentityHashMap<Object, Node> positions) {
        if (o == null) {
            return JSONObject.NULL;
        }
        Node n = positions.get(o);
        return n == null ? "OUTSIDE_SNAPSHOT:" + o.getClass().getName() : n.id;
    }

    private static String id(Object o) {
        return o instanceof Enum<?>
                ? ((Enum<?>) o).name()
                : o instanceof String ? (String) o : o == null ? "null" : o.getClass().getName();
    }

    private static boolean descendant(Node n, Node ancestor) {
        for (Node p = n.parent; p != null; p = p.parent) {
            if (p == ancestor) {
                return true;
            }
        }
        return false;
    }

    private static Object rect(PositionAPI p) throws Exception {
        if (p == null) {
            return JSONObject.NULL;
        }
        return new JSONObject()
                .put("x", p.getX())
                .put("y", p.getY())
                .put("width", p.getWidth())
                .put("height", p.getHeight());
    }
}
