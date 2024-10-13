package ashlib.data.plugins.ui.plugins;

import ashlib.data.plugins.ui.models.HorizontalTooltipMaker;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.List;

public class HorizontalTooltipPlugin implements CustomUIPanelPlugin {
    HorizontalTooltipMaker maker;

    public HorizontalTooltipMaker getHorizontalTooltipMaker() {
        return maker;
    }


    public HorizontalTooltipPlugin(){
     this.maker = new HorizontalTooltipMaker();
        
    }
    public void init(CustomPanelAPI panel, float width, float height, boolean hasVerticalScrollbar, float trueWidth, float trueHeight){
        maker.init(panel, width, height, hasVerticalScrollbar, trueWidth, trueHeight);
    }
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void renderBelow(float alphaMult) {

    }

    @Override
    public void render(float alphaMult) {
        if (maker != null && maker.getHorizontalScrollbar() != null&&maker.mainPanel!=null) {
            maker.getHorizontalScrollbar().moveTooltip(maker.mainPanel.getPosition().getWidth(),maker.horizontalScrollbar.startingX);
            maker.getHorizontalScrollbar().handleMouseDragging(maker.mainPanel.getPosition().getWidth());
        }
    }

    @Override
    public void advance(float amount) {
        if (maker.getHorizontalScrollbar() != null) {
            maker.horizontalScrollbar.detectIfRightMouse();
            maker.getMainTooltip().getExternalScroller().setXOffset(maker.horizontalScrollbar.getCurrOffset());
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (maker.getHorizontalScrollbar() != null) {
            maker.getHorizontalScrollbar().processInputForScrollbar(events, maker.mainPanel.getPosition().getWidth());
        }
    }

    @Override
    public void buttonPressed(Object buttonId) {

    }
}
