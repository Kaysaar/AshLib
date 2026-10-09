package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import org.json.JSONArray;
import org.json.JSONObject;

/** Replaces the bar decoration while retaining the native frame for restoration. */
final class CampaignBarFrame {
    private static final String EXPECTED_ASSET = "graphics/ui/tripad_bot_left_button_widget2.png";
    private final UIComponentAPI frame;
    private final UIPanelAPI bar;
    private final Object sprite, texture;
    private final boolean originalResize;
    private final float originalWidth, height, spriteWidth, spriteHeight, overhang, dx, dy, opacity;
    private CampaignFrameSurface surface;
    private MethodRef localBottom;

    CampaignBarFrame(UIComponentAPI frame, UIPanelAPI bar) throws Exception {
        this.frame = frame;
        this.bar = bar;
        Object flag = CampaignUiAccess.read(frame, "isResize");
        require(flag instanceof Boolean, "MISSING_FRAME_RESIZE_STATE");
        originalResize = (Boolean) flag;
        sprite = CampaignUiAccess.read(frame, "getSprite");
        require(sprite != null, "MISSING_FRAME_SPRITE");
        texture = CampaignUiAccess.read(sprite, "getTexture");
        requireAsset();
        require(
                CampaignFrameDiagnostics.ownerReferences(bar, frame).length() == 1,
                "UNKNOWN_FRAME_OWNERSHIP");
        opacity = frame.getOpacity();
        require(near(opacity, 1), "UNKNOWN_FRAME_OPACITY");
        PositionAPI position = frame.getPosition();
        originalWidth = position.getWidth();
        height = position.getHeight();
        overhang = originalWidth - bar.getPosition().getWidth();
        dx = number(CampaignUiAccess.read(position, "getXAlignOffset"));
        dy = number(CampaignUiAccess.read(position, "getYAlignOffset"));
        spriteWidth = number(CampaignUiAccess.read(sprite, "getWidth"));
        spriteHeight = number(CampaignUiAccess.read(sprite, "getHeight"));
        require(
                overhang >= 0
                        && overhang <= 32
                        && height >= bar.getPosition().getHeight()
                        && height <= bar.getPosition().getHeight() + 8
                        && near(dx, 0)
                        && near(dy, 0),
                "UNKNOWN_FRAME_GEOMETRY");
        require(
                near(spriteWidth, originalWidth) && near(spriteHeight, height),
                "FRAME_SPRITE_BOUNDS_MISMATCH");
        require(near(originalWidth, 914) && near(height, 35), "UNKNOWN_FRAME_SLICE_DIMENSIONS");
        verify(false, bar.getPosition().getWidth());
    }

    void prepare(float width, float gap, CampaignFrameSurface.Factory factory) throws Exception {
        prepare(8, width, gap, factory);
    }

    void prepare(int count, float width, float gap, CampaignFrameSurface.Factory factory)
            throws Exception {
        require(surface == null, "FRAME_SURFACE_ALREADY_PREPARED");
        surface =
                new CampaignFrameSurface(
                        sprite, new CampaignFrameSlices(count, (int) width, (int) gap), factory);
        for (MethodRef method : CampaignHandleAccess.methods(bar.getClass())) {
            Class<?>[] args = method.getParameterTypes();
            if (!method.isBridge()
                    && method.getName().equals("sendToBottomWithinItself")
                    && method.getReturnType() == void.class
                    && args.length == 1
                    && args[0].isInstance(surface.panel)) {
                require(localBottom == null, "AMBIGUOUS_LOCAL_FRAME_ORDER");
                localBottom = method;
            }
        }
        require(localBottom != null, "MISSING_LOCAL_FRAME_ORDER");
    }

    float projectedRight(float barWidth) {
        return frame.getPosition().getX()
                + (surface == null ? barWidth + overhang : surface.plan.width);
    }

    UIComponentAPI decoration() {
        return surface == null ? null : surface.panel;
    }

    CampaignFrameSurface prepareNavigation(CampaignFrameSurface.Factory factory) throws Exception {
        requireAsset();
        return new CampaignFrameSurface(sprite, CampaignFrameSlices.navigable(), factory);
    }

    Throwable renderFailure() {
        return surface == null ? null : surface.failure();
    }

    void disposePrepared() {
        if (surface != null) {
            surface.dispose();
        }
    }

    void apply(float barWidth) throws Exception {
        require(surface != null, "FRAME_SURFACE_NOT_PREPARED");
        // Hide the native frame for restoration; zero opacity skips its sprite and GL updates.
        frame.setOpacity(0);
        bar.addComponent(surface.panel).inBL(dx, dy);
        // Public sendToBottom affects ancestors; use the validated bar-local operation.
        localBottom.invoke(bar, surface.panel);
    }

    void restore() throws Exception {
        if (surface != null) {
            if (CampaignUiAccess.children(bar).contains(surface.panel)) {
                bar.removeComponent(surface.panel);
            }
            surface.dispose();
        }
        frame.setOpacity(opacity);
    }

    void verify(boolean inserted, float barWidth) throws Exception {
        requireAsset();
        require(
                CampaignFrameDiagnostics.ownerReferences(bar, frame).length() == 1,
                "UNKNOWN_FRAME_OWNERSHIP");
        PositionAPI position = frame.getPosition();
        require(
                CampaignUiAccess.children(bar).contains(frame)
                        && CampaignUiAccess.read(position, "getParent") == bar.getPosition()
                        && CampaignUiAccess.read(position, "getBase") == null
                        && !position.isSuspendRecompute(),
                "FRAME_PARENT_OR_ANCHOR_CHANGED");
        require(
                near(number(CampaignUiAccess.read(position, "getXAlignOffset")), dx)
                        && near(number(CampaignUiAccess.read(position, "getYAlignOffset")), dy)
                        && near(position.getX(), bar.getPosition().getX() + dx)
                        && near(position.getY(), bar.getPosition().getY() + dy),
                "FRAME_POSITION_CHANGED");
        require(
                CampaignUiAccess.read(frame, "getSprite") == sprite
                        && CampaignUiAccess.read(sprite, "getTexture") == texture
                        && near(position.getWidth(), originalWidth)
                        && near(position.getHeight(), height)
                        && near(number(CampaignUiAccess.read(sprite, "getWidth")), spriteWidth)
                        && near(number(CampaignUiAccess.read(sprite, "getHeight")), spriteHeight)
                        && Boolean.valueOf(originalResize)
                                .equals(CampaignUiAccess.read(frame, "isResize"))
                        && near(frame.getOpacity(), inserted ? 0 : opacity),
                "FRAME_VISUAL_STATE_CHANGED");
        if (inserted) {
            require(
                    surface != null && CampaignUiAccess.children(bar).get(0) == surface.panel,
                    "FRAME_SURFACE_ORDER_CHANGED");
            PositionAPI surfacePosition = surface.panel.getPosition();
            require(
                    CampaignUiAccess.read(surfacePosition, "getParent") == bar.getPosition()
                            && CampaignUiAccess.read(surfacePosition, "getBase") == null
                            && near(surfacePosition.getX(), position.getX())
                            && near(surfacePosition.getY(), position.getY())
                            && near(surfacePosition.getWidth(), surface.plan.width)
                            && near(surfacePosition.getHeight(), height)
                            && near(surface.panel.getOpacity(), opacity),
                    "FRAME_SURFACE_STATE_CHANGED");
        } else {
            require(
                    surface == null || !CampaignUiAccess.children(bar).contains(surface.panel),
                    "FRAME_SURFACE_STILL_ATTACHED");
        }
    }

    JSONObject snapshot() throws Exception {
        boolean attached =
                surface != null && CampaignUiAccess.children(bar).contains(surface.panel);
        return new JSONObject()
                .put("asset", EXPECTED_ASSET)
                .put("identitySource", "TEXTURE_RESOURCE_METADATA")
                .put(
                        "componentSpriteName",
                        CampaignFrameDiagnostics.json(
                                CampaignUiAccess.read(frame, "getSpriteName")))
                .put("textureStrings", CampaignFrameDiagnostics.textureStrings(texture))
                .put("componentWidth", frame.getPosition().getWidth())
                .put("height", height)
                .put("spriteWidth", CampaignUiAccess.read(sprite, "getWidth"))
                .put("spriteHeight", CampaignUiAccess.read(sprite, "getHeight"))
                .put(
                        "right",
                        frame.getPosition().getX()
                                + (attached ? surface.plan.width : originalWidth))
                .put("originalOverhang", overhang)
                .put("composition", "PIXEL_PRESERVING_SLICES")
                .put("nativeFrameOpacity", frame.getOpacity())
                .put("surfaceAttached", attached)
                .put("composedWidth", surface == null ? JSONObject.NULL : surface.plan.width)
                .put("sliceCount", surface == null ? 0 : surface.plan.slices.size())
                .put("automaticResize", CampaignUiAccess.read(frame, "isResize"));
    }

    private void requireAsset() throws Exception {
        // Sprite-based frames may have no name; texture metadata must identify the expected asset.
        Object name = CampaignUiAccess.read(frame, "getSpriteName");
        require(
                name == null || EXPECTED_ASSET.equals(name),
                "UNKNOWN_FRAME_ASSET: conflicting component name");
        require(texture != null, "UNKNOWN_FRAME_ASSET: missing texture");
        JSONArray fields = CampaignFrameDiagnostics.textureStrings(texture);
        int matches = 0;
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            require(!field.has("unavailable"), "UNKNOWN_FRAME_ASSET: unreadable texture metadata");
            if (field.isNull("value")) {
                continue;
            }
            String value = field.getString("value");
            if (value.isEmpty()) {
                continue;
            }
            require(
                    EXPECTED_ASSET.equals(value),
                    "UNKNOWN_FRAME_ASSET: conflicting texture resource");
            matches++;
        }
        require(matches > 0, "UNKNOWN_FRAME_ASSET: no positive texture resource match");
    }

    private static float number(Object n) {
        require(n instanceof Number, "MISSING_FRAME_NUMBER");
        return ((Number) n).floatValue();
    }

    private static boolean near(float a, float b) {
        return Float.isFinite(a) && Float.isFinite(b) && Math.abs(a - b) < 0.1f;
    }

    private static void require(boolean ok, String reason) {
        if (!ok) {
            throw new IllegalStateException(reason);
        }
    }
}
