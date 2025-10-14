package ashlib.data.plugins.ui.models.resizable.map;

import ashlib.data.plugins.ui.models.TrapezoidButtonDetector;
import ashlib.data.plugins.ui.models.resizable.ResizableComponent;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MapEntityComponent extends ResizableComponent {
    SectorEntityToken token;
    LinkedHashMap<String, MapEntityOnClickHook> hooks = new LinkedHashMap<>();
    float maxHighlight = 0.15f;
    float currHighlight = 0f;

    public void addHook(MapEntityOnClickHook hook, String idOfHook) {
        hooks.put(idOfHook, hook);
    }
    public boolean forceHighlight =false;

    public void setForceHighlight(boolean forceHighlight) {
        this.forceHighlight = forceHighlight;
    }

    public LinkedHashMap<String, MapEntityOnClickHook> getHooks() {
        return hooks;
    }

    public MapEntityComponent(SectorEntityToken token) {
        this.token = token;
    }

    public SectorEntityToken getToken() {
        return token;
    }

    public void processInputForMapEntity(List<InputEventAPI> events, MapMainComponent component) {
        boolean hovers = false;
        for (InputEventAPI event : events) {
            if (event.isConsumed()) continue;
            if (event.isMouseMoveEvent() && getComponentPanel().getPosition().containsEvent(event)) {
                hovers = true;
            }
            hooks.values().stream().filter(x -> x.shouldTrigger(event)).forEach(x -> x.onClick(this, event, componentPanel, component));
        }
        if (hovers||forceHighlight) {
            currHighlight += 0.02f;
            if (currHighlight > maxHighlight) {
                currHighlight = maxHighlight;
            }

        } else {
            currHighlight -= 0.02f;
            if (currHighlight <= 0) currHighlight = 0;
        }
    }

    @Override
    public void clearUI() {
        super.clearUI();

    }

    @Override
    public void render(float alphaMult) {
        if (!hooks.isEmpty()||forceHighlight) {
            // pick a soft edge width (in px). you can tune this.
            float feather = Math.max(2f, 8f * scale);
            drawSoftHighlight(
                    new Vector2f(componentPanel.getPosition().getCenterX(), componentPanel.getPosition().getCenterY()),
                    (token.getRadius()) * scale,
                    feather,
                    Color.white,
                    currHighlight
            );
        }
    }

    public static void drawFilledCircle(Vector2f center, float radiusPx, Color color, float alpha) {
        if (radiusPx <= 0f) return;

        // ~1 vertex per 4 px of circumference, clamped
        final int segments = Math.max(24, Math.min(720, (int) (2 * Math.PI * radiusPx / 4f)));

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            final float a = (color.getAlpha() / 255f) * alpha;
            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, a);

            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            // center
            GL11.glVertex2f(center.x, center.y);
            // ring
            for (int i = 0; i <= segments; i++) {
                double t = i * (2.0 * Math.PI / segments);
                float x = center.x + (float) Math.cos(t) * radiusPx;
                float y = center.y + (float) Math.sin(t) * radiusPx;
                GL11.glVertex2f(x, y);
            }
            GL11.glEnd();
        } finally {
            GL11.glPopAttrib();
        }
    }


    public static void drawSoftHighlight(Vector2f center, float radiusPx, float featherPx,
                                         Color color, float alpha) {
        if (radiusPx <= 0f) return;

        final float innerR = Math.max(0f, radiusPx);
        final float outerR = Math.max(innerR, innerR + Math.max(0f, featherPx));

        // ~1 vertex per 5 px of circumference at outer edge
        final int segments = Math.max(24, Math.min(720, (int) (2 * Math.PI * outerR / 5f)));

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            final float baseR = color.getRed() / 255f;
            final float baseG = color.getGreen() / 255f;
            final float baseB = color.getBlue() / 255f;
            final float baseA = (color.getAlpha() / 255f) * alpha;

            // 1) solid inner disc
            GL11.glColor4f(baseR, baseG, baseB, baseA);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2f(center.x, center.y);
            for (int i = 0; i <= segments; i++) {
                double t = i * (2.0 * Math.PI / segments);
                float x = center.x + (float) Math.cos(t) * innerR;
                float y = center.y + (float) Math.sin(t) * innerR;
                GL11.glVertex2f(x, y);
            }
            GL11.glEnd();

            // 2) feather ring (innerR -> outerR alpha: baseA -> 0)
            if (outerR > innerR) {
                GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
                for (int i = 0; i <= segments; i++) {
                    double t = i * (2.0 * Math.PI / segments);
                    float cos = (float) Math.cos(t);
                    float sin = (float) Math.sin(t);

                    float ix = center.x + cos * innerR;
                    float iy = center.y + sin * innerR;
                    float ox = center.x + cos * outerR;
                    float oy = center.y + sin * outerR;

                    // inner edge (opaque)
                    GL11.glColor4f(baseR, baseG, baseB, baseA);
                    GL11.glVertex2f(ix, iy);
                    // outer edge (transparent)
                    GL11.glColor4f(baseR, baseG, baseB, 0f);
                    GL11.glVertex2f(ox, oy);
                }
                GL11.glEnd();
            }
        } finally {
            GL11.glPopAttrib();
        }
    }
}

