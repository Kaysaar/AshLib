package ashlib.data.plugins.ui.models;

import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;

//Inspired by grandeur alex design
public class ProgressBarComponentV2 implements ExtendedUIPanelPlugin {
    CustomPanelAPI mainPanel,componentPanel;
    LabelAPI progressLabel;
    Color barBrackets,barColor;
    float progress;
    protected BaseEventIntel influencer;
    String barTextFont;
    public float getProgress() {
        return progress;
    }
    String barText;

    public void setProgress(float progress) {
        this.progress = progress;
        createUI();
    }

    public void setBarBrackets(Color barBrackets) {
        this.barBrackets = barBrackets;
    }

    public void setBarColor(Color barColor) {
        this.barColor = barColor;
    }

    public LabelAPI getProgressLabel() {
        return progressLabel;
    }


    public ProgressBarComponentV2(float width, float height, String barText,String barTextFont, Color barColor, Color barBrackets, float currProgress){
        mainPanel = Global.getSettings().createCustom(width,height,this);
        if(!AshMisc.isStringValid(barTextFont)){
            this.barTextFont = Fonts.DEFAULT_SMALL;
        }
        else{
            this.barTextFont = barTextFont;
        }
        this.barText = barText;
        influencer = new BaseEventIntel(){
            @Override
            public float getBarWidth() {
                return width-15;
            }

            @Override
            public float getBarHeight() {
                return height-4;
            }

            @Override
            public Color getBarBracketColor() {
               return barBrackets;
            }

            @Override
            public Color getBarColor() {
                Color color = barColor;
                color = Misc.interpolateColor(color, Color.black, 0.25f);
                return color;
            }
            public float getBarProgressIndicatorHeight() {
                return 10f;
            }
            public float getBarProgressIndicatorWidth() {
                return 10f;
            }
            public Color getBarProgressIndicatorLabelColor() {
                return Misc.getHighlightColor();
            }
            public Color getBarProgressIndicatorColor() {
                return getBarColor();
            }
        };
        influencer.setMaxProgress(100);
        this.progress = currProgress;

        createUI();

    }

    @Override
    public CustomPanelAPI getMainPanel() {
        return mainPanel;
    }

    @Override
    public void createUI() {

        int percentage = Math.round(progress*100);
        influencer.setProgress(percentage);
        CustomPanelAPI componentPanel = Global.getSettings().createCustom(mainPanel.getPosition().getWidth(),mainPanel.getPosition().getHeight(),null);
        TooltipMakerAPI tooltip = componentPanel.createUIElement(componentPanel.getPosition().getWidth(),componentPanel.getPosition().getHeight(),false);
        EventProgressBarAPI api = tooltip.addEventProgressBar(influencer,0f);
        tooltip.setParaFont(barTextFont);
        if(barText!=null){
            progressLabel = tooltip.createLabel(barText,Misc.getTextColor());
            float y = api.getPosition().getHeight();
            float middle = y/2;
            api.addComponent((UIComponentAPI) progressLabel).inMid();
        }

        componentPanel.addUIElement(tooltip).inTL(0,0);
        mainPanel.addComponent(componentPanel).inTL(0,0);
        if(this.componentPanel!=null){
            mainPanel.removeComponent(this.componentPanel);
        }
        this.componentPanel = componentPanel;


    }
    public void influenceLabel(){

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

    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }
}
