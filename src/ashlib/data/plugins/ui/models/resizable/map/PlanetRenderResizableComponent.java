package ashlib.data.plugins.ui.models.resizable.map;

import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.ui.models.resizable.ResizableComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.CombatViewport;
import com.fs.starfarer.combat.entities.terrain.Planet;
import data.kaysaar.aotd.vok.campaign.econ.globalproduction.impl.nidavelir.NidavelirComplexMegastructure;
import data.kaysaar.aotd.vok.misc.AoTDMisc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class PlanetRenderResizableComponent extends MapEntityComponent {
    String planetType;
    float originalSize;
    boolean isStar;
    float angle = 0f;
    PlanetAPI planet;
    boolean discovered = true;

    public static void renderPlanet(String spec, Vector2f point, float size, float facing, float pitch,
                                    float surfaceAngle, float atmoAngle, float alpha, boolean isStar,
                                    float tilt, float scale) {

        CombatViewport vv = new CombatViewport(point.x, point.y, 200, 200);
        vv.setAlphaMult(alpha);

        Planet planet = new Planet(spec, size, 0, point);
        planet.setScale(scale);
        planet.setRadius(size);
        planet.setAngle(surfaceAngle);
        planet.setCloudAngle(atmoAngle);
        planet.setFacing(facing - 90f);
        planet.setTilt(tilt);
        planet.setPitch(pitch);

        planet.renderSphere(vv);
        planet.renderStarGlow(vv);
    }

    public PlanetRenderResizableComponent(float size, String type, boolean isStar) {
        super(null);
        this.originalSize = size;
        this.planetType = type;
        this.isStar = isStar;
        this.componentPanel = Global.getSettings().createCustom(size * 2f, size * 2f, this);
    }

    public PlanetRenderResizableComponent(PlanetAPI reference, boolean discovered) {
        super(reference);
        this.originalSize = reference.getRadius();
        this.planet = reference;
        this.isStar = reference.isStar();
        this.discovered = discovered;
        this.componentPanel = Global.getSettings().createCustom(reference.getRadius() * 2f, reference.getRadius() * 2f, this);
    }

    @Override
    public void render(float alphaMult) {
        super.render(alphaMult);

        float cx = componentPanel.getPosition().getCenterX();
        float cy = componentPanel.getPosition().getCenterY();


            renderImpl(alphaMult, cx, cy);


    }

    private void renderImpl(float alphaMult, float cx, float cy) {
        if (!discovered && !isStar) {
            drawPlaceholderCircle(cx, cy, originalSize * scale, alphaMult);
            return;
        }

        // Otherwise render planet as usual.
        if (planet == null) {
            // Safe defaults when we only have a type string
            float tilt = 0f;
            float pitch = 0f;
            float facing = 90f;
            renderPlanet(
                    planetType,
                    new Vector2f(cx, cy),
                    originalSize,
                    facing,
                    pitch,
                    angle,
                    0f,
                    alphaMult,
                    isStar,
                    tilt,
                    scale
            );
        } else {
            renderPlanet(
                    planet.getTypeId(),
                    new Vector2f(cx, cy),
                    originalSize,
                    planet.getFacing(),
                    planet.getSpec().getPitch(),
                    angle,
                    angle / 2f,
                    alphaMult,
                    isStar,
                    planet.getSpec().getTilt(),
                    scale
            );

            // Optional: overlay megastructure if present
            if (Global.getSettings().getModManager().isModEnabled("aotd_vok")) {
                NidavelirComplexMegastructure mega = AoTDMisc.getNidavelir();
                if (mega != null && mega.getEntityTiedTo() != null && mega.getEntityTiedTo().equals(planet)) {
                    renderPlanet(
                            mega.shipyard.type,
                            new Vector2f(cx, cy),
                            originalSize + 35f,
                            planet.getFacing(),
                            planet.getSpec().getPitch(),
                            0f,
                            angle / 2f,
                            alphaMult,
                            isStar,
                            planet.getSpec().getTilt(),
                            scale
                    );
                }
            }
        }

        super.render(alphaMult);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        // Guard for null planet in the "type-only" renderer
        float rot = (planet != null && planet.getSpec() != null) ? planet.getSpec().getRotation() : 10f;
        angle += rot * amount;
        if (angle > 360f) {
            angle -= 360f;
        }
    }
    public float getHaloRadius(PlanetAPI planet){
        float rad = Math.max(planet.getRadius()*planet.getSpec().getAtmosphereThickness(),planet.getSpec().getAtmosphereThicknessMin()+planet.getRadius());
        if(planet.isStar()){
            rad = planet.getRadius()*planet.getSpec().getCoronaSize();
        }
        return rad;
    }
    // --- Helpers -------------------------------------------------------------

    /** Draws a simple filled gray circle as a placeholder for undiscovered non-star planets. */
    private void drawPlaceholderCircle(float cx, float cy, float radius, float alphaMult) {
        if (radius <= 0f) return;

        final Color fill = new Color(120, 120, 120, 220);
        final Color stroke = new Color(90, 90, 90, 255);

        int segments = Math.max(24, Math.min(256, (int) (2 * Math.PI * radius / 6f)));

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT);
        try {
            // Fill
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            float af = (fill.getAlpha() / 255f) * alphaMult;
            GL11.glColor4f(fill.getRed() / 255f, fill.getGreen() / 255f, fill.getBlue() / 255f, af);

            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2f(cx, cy);
            for (int i = 0; i <= segments; i++) {
                double t = i * (2.0 * Math.PI / segments);
                float x = cx + (float) Math.cos(t) * radius;
                float y = cy + (float) Math.sin(t) * radius;
                GL11.glVertex2f(x, y);
            }
            GL11.glEnd();

            // Thin outline
            float ao = (stroke.getAlpha() / 255f) * alphaMult;
            GL11.glColor4f(stroke.getRed() / 255f, stroke.getGreen() / 255f, stroke.getBlue() / 255f, ao);
            GL11.glLineWidth(Math.max(1f, 1f * scale));
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i < segments; i++) {
                double t = i * (2.0 * Math.PI / segments);
                float x = cx + (float) Math.cos(t) * radius;
                float y = cy + (float) Math.sin(t) * radius;
                GL11.glVertex2f(x, y);
            }
            GL11.glEnd();
        } finally {
            GL11.glPopAttrib();
        }
    }

    public PlanetAPI getPlanet() {
        return planet;
    }
}
