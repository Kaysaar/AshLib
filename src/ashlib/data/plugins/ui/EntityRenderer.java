package ashlib.data.plugins.ui;

import ashlib.data.plugins.ui.models.ExtendedUIPanelPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.combat.CombatViewport;
import com.fs.starfarer.combat.entities.terrain.Planet;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class EntityRenderer implements ExtendedUIPanelPlugin {
    Planet graphics;
    CustomPanelAPI mainPanel;
    SpriteAPI sprite;
    public EntityRenderer(SectorEntityToken tokenOfMarket, float boxSize) {
        mainPanel = Global.getSettings().createCustom(boxSize,boxSize,this);
        if(tokenOfMarket instanceof PlanetAPI planet){
            graphics = new Planet(planet.getTypeId(),planet.getRadius(),0f,new Vector2f());
            graphics.setTilt(planet.getSpec().getTilt());
            Planet original = (Planet) ReflectionUtilis.invokeMethodWithAutoProjection("getGraphics",planet);
            graphics.setAngle(original.getAngle());
            graphics.setPitch(original.getPitch());
            float scale = boxSize/(planet.getRadius()*2);
            graphics.setScale(scale);
        }
        else if (tokenOfMarket != null){
            CustomEntitySpecAPI spec = tokenOfMarket.getCustomEntitySpec();
            if (spec != null) {
                sprite = Global.getSettings().getSprite(spec.getSpriteName());
                float originalWidth= spec.getSpriteWidth();
                float originalHeight = spec.getSpriteHeight();
                // Guard against zero/invalid sizes
                if (originalWidth <= 0f || originalHeight <= 0f) {
                    originalWidth  = Math.max(1f, sprite.getWidth());
                    originalHeight = Math.max(1f, sprite.getHeight());
                }

                // Scale to fit inside boxSize x boxSize, preserving aspect ratio
                float scale = Math.min(boxSize / originalWidth, boxSize / originalHeight);
                float scaledW = originalWidth  * scale;
                float scaledH = originalHeight * scale;

                // Apply size to the sprite
                sprite.setSize(scaledW, scaledH);
            }
        }

    }
    @Override
    public CustomPanelAPI getMainPanel() {
        return mainPanel;
    }

    @Override
    public void createUI() {

    }

    @Override
    public void clearUI() {

    }

    @Override
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void renderBelow(float alphaMult) {

    }

    @Override
    public void render(float alphaMult) {
        if(graphics!=null){
            PositionAPI refPos = mainPanel.getPosition();
            float refX = refPos.getX();
            float refY = refPos.getY();
            float refCenterX = refPos.getCenterX();
            float refCenterY = refPos.getCenterY();
            float refWidth = refPos.getWidth();
            float refHeight = refPos.getHeight();
            CombatViewport sphereViewport = new CombatViewport(refX, refY, refWidth, refHeight);
            graphics.getLocation().set(refCenterX, refCenterY);
            graphics.renderSphere(sphereViewport);

        } else if (sprite!=null) {
            sprite.renderAtCenter(mainPanel.getPosition().getCenterX(), mainPanel.getPosition().getCenterY());
        }
    }

    @Override
    public void advance(float amount) {
        if(graphics!=null){
            graphics.advance(amount);
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }
}
