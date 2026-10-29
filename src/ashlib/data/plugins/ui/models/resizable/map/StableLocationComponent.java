package ashlib.data.plugins.ui.models.resizable.map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class StableLocationComponent extends MapEntityComponent {
    public static float minIconSize = 26f;
    public static float maxIconSize = 48f;
    public static float hitPadding = 8f;
    SpriteAPI sprite = Global.getSettings().getSprite("systemMap","icon_stable_location");

    public StableLocationComponent(SectorEntityToken token) {
        super(token);
        this.componentPanel = Global.getSettings().createCustom(JumpPointRenderer.size,JumpPointRenderer.size,this);
    }

    public float getIconSize(){
        return Math.max(minIconSize,Math.min(maxIconSize,JumpPointRenderer.size*scale));
    }

    @Override
    public void resize(float scale) {
        super.resize(scale);
        float size = getIconSize()+hitPadding;
        // Keep the original world-space center when enlarging the screen-space hitbox.
        float centerX = (originalCoords.x+originalWidth/2f)*scale-movingCords.x;
        float centerY = (originalCoords.y+originalHeight/2f)*scale+movingCords.y;
        componentPanel.getPosition().setSize(size,size);
        componentPanel.getPosition().setLocation(0,0).inTL(centerX-size/2f,centerY-size/2f);
        adjustTooltipOnHoverPanelToViewport();
    }

    @Override
    public void render(float alphaMult) {
        float size = getIconSize();
        boolean highlighted = forceHighlight || currHighlight>0f;
        if(highlighted)size *= 1.1f;
        Vector2f center = new Vector2f(componentPanel.getPosition().getCenterX(),componentPanel.getPosition().getCenterY());
        drawFilledCircle(center,size/2f+2f,new Color(0,0,0,190),alphaMult);
        sprite.setSize(size,size);
        sprite.setColor(forceHighlight?Color.cyan:highlighted?Color.white:hooks.isEmpty()?Color.white:new Color(255,210,100));
        sprite.setAlphaMult(alphaMult);
        sprite.renderAtCenter(center.x,center.y);
    }
}
