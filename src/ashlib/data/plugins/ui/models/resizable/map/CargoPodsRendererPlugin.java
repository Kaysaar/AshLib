package ashlib.data.plugins.ui.models.resizable.map;

import ashlib.data.plugins.ui.models.resizable.ResizableComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.GenericFieldItemManager;
import com.fs.starfarer.combat.CombatViewport;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import static ashlib.data.plugins.ui.models.resizable.map.EntityRendererComponent.*;


public class CargoPodsRendererPlugin extends ResizableComponent {

    private final SectorEntityToken token;
    private final GenericFieldItemManager manager;


    public CargoPodsRendererPlugin(SectorEntityToken token) {
        this.token = token;
        this.manager = (GenericFieldItemManager) ReflectionUtilis.findFieldByType(token.getCustomPlugin(), GenericFieldItemManager.class);
        // Size doesn't matter much; parent positions this component so its center is where we want to render.
        this.componentPanel = Global.getSettings().createCustom(token.getRadius() * 2f, token.getRadius() * 2f, this);
        manager.advance(1f);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

    }

    @Override
    public void render(float alphaMult) {
        // Where we want the entity center to appear (screen space)
        float sx = componentPanel.getPosition().getCenterX();
        float sy = componentPanel.getPosition().getCenterY();

        // World-space entity center that the manager uses internally
        float wx = token.getLocation().x;
        float wy = token.getLocation().y;

        // Pixels-per-world (your UI zoom). ResizableComponent.scale is propagated by your zoom panel.
        float px = Math.max(1e-6f, this.scale);

        // Viewport: manager only reads alpha from it; keep it simple.
        ViewportAPI vp = new CombatViewport(sx, sy, 0f, 0f);
        float a = alphaMult
                * token.getSensorFaderBrightness()
                * token.getSensorContactFaderBrightness();
        vp.setAlphaMult(a);

        // Matrix sandwich: place->scale->cancel-world-translate
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(sx, sy, 0f);      // place at our component center
            GL11.glScalef(px, px, 1f);          // world units → pixels
            GL11.glTranslatef(-wx, -wy, 0f);    // cancel manager's internal world translate

            // Render exactly like vanilla, but under our transform
            manager.render(CampaignEngineLayers.RINGS, vp);

        } finally {
            GL11.glPopMatrix();
        }
        float orbitR = Math.max(1f, token.getRadius() * this.scale);
        drawOrbit(new Vector2f(sx, sy), orbitR, orbitColor, orbitLineWidth, alphaMult);
        super.render(alphaMult);
    }
}
