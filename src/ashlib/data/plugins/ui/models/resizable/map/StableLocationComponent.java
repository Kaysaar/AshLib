package ashlib.data.plugins.ui.models.resizable.map;

import ashlib.data.plugins.ui.models.resizable.ResizableComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.graphics.SpriteAPI;

public class StableLocationComponent extends MapEntityComponent {
    SpriteAPI sprite = Global.getSettings().getSprite("systemMap","icon_stable_location");
    public StableLocationComponent(SectorEntityToken token) {
        super(token);

        this.componentPanel = Global.getSettings().createCustom(JumpPointRenderer.size,JumpPointRenderer.size,this);

    }

    @Override
    public void render(float alphaMult) {
        float iconSize = JumpPointRenderer.size;
        float trueIconSize = iconSize*scale;
        float currAlpha = alphaMult;
        if(!hooks.isEmpty()){
            currAlpha = (0.8f+this.currHighlight)*alphaMult;

        }
        sprite.setSize(trueIconSize, trueIconSize);
        sprite.setAlphaMult(currAlpha);
        sprite.renderAtCenter(componentPanel.getPosition().getCenterX(),componentPanel.getPosition().getCenterY());
    }

}
