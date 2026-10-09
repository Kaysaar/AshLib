package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.ArrayList;
import java.util.List;

/** Input-transparent decoration using private sprites with shared texture data. */
final class CampaignFrameSurface extends BaseCustomUIPanelPlugin {
    interface Factory {
        UIPanelAPI create(float width, float height, CampaignFrameSurface plugin) throws Exception;
    }

    static final Factory NATIVE =
            new Factory() {
                public UIPanelAPI create(float width, float height, CampaignFrameSurface plugin) {
                    return Global.getSettings().createCustom(width, height, plugin);
                }
            };
    final CampaignFrameSlices plan;
    final UIPanelAPI panel;
    private final List<Object> sprites = new ArrayList<Object>();
    private final MethodRef render, alpha;
    private Throwable failure;
    private boolean disposed;

    CampaignFrameSurface(Object source, CampaignFrameSlices plan, Factory factory)
            throws Exception {
        this.plan = plan;
        Class<?> type = source.getClass();
        MethodRef clone = required(type, "clone"),
                size = required(type, "setSize", float.class, float.class);
        MethodRef texX = required(type, "setTexX", float.class),
                texWidth = required(type, "setTexWidth", float.class);
        render = required(type, "render", float.class, float.class);
        alpha = required(type, "setAlphaMult", float.class);
        Object texture = CampaignUiAccess.read(source, "getTexture");
        float u = number(source, "getTexX"), uw = number(source, "getTexWidth");
        require(Float.isFinite(u) && Float.isFinite(uw) && uw > 0, "INVALID_FRAME_UV");
        require(
                number(source, "getImageWidth") == CampaignFrameSlices.SOURCE_WIDTH
                        && number(source, "getImageHeight") == CampaignFrameSlices.HEIGHT,
                "UNKNOWN_FRAME_IMAGE_SIZE");
        require(
                number(source, "getAngle") == 0
                        && number(source, "getOffsetX") == 0
                        && number(source, "getOffsetY") == 0,
                "UNKNOWN_FRAME_TRANSFORM");
        for (CampaignFrameSlices.Slice slice : plan.slices) {
            Object copy = clone.invoke(source);
            require(
                    copy != null
                            && copy != source
                            && copy.getClass() == type
                            && !sprites.contains(copy)
                            && CampaignUiAccess.read(copy, "getTexture") == texture,
                    "FRAME_CLONE_NOT_ISOLATED");
            size.invoke(copy, (float) slice.width, (float) CampaignFrameSlices.HEIGHT);
            texX.invoke(copy, u + uw * slice.sourceX / CampaignFrameSlices.SOURCE_WIDTH);
            texWidth.invoke(copy, uw * slice.width / CampaignFrameSlices.SOURCE_WIDTH);
            require(
                    number(copy, "getWidth") == slice.width
                            && number(copy, "getHeight") == CampaignFrameSlices.HEIGHT,
                    "FRAME_SLICE_SIZE_REJECTED");
            sprites.add(copy);
        }
        panel = factory.create(plan.width, CampaignFrameSlices.HEIGHT, this);
        require(panel != null, "FRAME_SURFACE_UNAVAILABLE");
    }

    @Override
    public void renderBelow(float opacity) {
        if (disposed || failure != null || opacity <= 0) {
            return;
        }
        try {
            PositionAPI position = panel.getPosition();
            for (int i = 0; i < sprites.size(); i++) {
                Object sprite = sprites.get(i);
                alpha.invoke(sprite, opacity);
                render.invoke(sprite, position.getX() + plan.slices.get(i).x, position.getY());
            }
        } catch (Throwable error) {
            failure = error;
        } // Defer rollback to the next campaign update.
    }

    Throwable failure() {
        return failure;
    }

    void dispose() {
        disposed = true;
        sprites.clear();
    }

    private static MethodRef required(Class<?> type, String name, Class<?>... args)
            throws Exception {
        MethodRef method = CampaignUiAccess.method(type, name, args);
        require(method != null, "MISSING_FRAME_SLICE_CAPABILITY " + name);
        return method;
    }

    private static float number(Object object, String name) throws Exception {
        Object value = CampaignUiAccess.read(object, name);
        require(value instanceof Number, "MISSING_FRAME_SLICE_NUMBER " + name);
        return ((Number) value).floatValue();
    }

    private static void require(boolean ok, String reason) {
        if (!ok) {
            throw new IllegalStateException(reason);
        }
    }
}
