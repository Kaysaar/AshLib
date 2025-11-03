package ashlib.data.plugins.ui.template;

import ashlib.data.plugins.ui.models.resizable.ResizableComponent;
import ashlib.data.plugins.ui.models.resizable.map.MapEntityComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class RestrictedArea extends ResizableComponent {
    float radius;

    public RestrictedArea( float radius) {
        this.radius = radius;
        componentPanel = Global.getSettings().createCustom(radius*2,radius*2,this);
    }

    @Override
    public void render(float alphaMult) {
        super.render(alphaMult);
        float feather = Math.max(2f, 8f * scale);
        MapEntityComponent.drawSoftHighlight(new Vector2f(componentPanel.getPosition().getCenterX(),componentPanel.getPosition().getCenterY()),radius*scale,feather, Misc.getNegativeHighlightColor().darker(),0.5f*alphaMult);
    }
}
