package ashlib.data.plugins.ui.models;

import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;

//Inspired by grandeur alex design
public class ProgressBarComponentV2 implements ExtendedUIPanelPlugin {
    CustomPanelAPI mainPanel,componentPanel;
    LabelAPI progressLabel;
    Color barBrackets,barColor;
    boolean haveMovedToAnotherSegment = true;
    public int currentSection;
    boolean passFirst = true;
    float progress;
    public int sections;
    private final transient BaseEventIntel influencer;
    String barTextFont;
    EventProgressBarAPI progressBar;
    public float getProgress() {
        return progress;
    }
    transient SpriteAPI arrows = Global.getSettings().getSprite("ui","sortIcon");
    boolean sliderMode = false;
    int minSection;
    String barText;
    TrapezoidButtonDetector detector = new TrapezoidButtonDetector();
    public void setProgress(float progress) {
        this.progress = progress;
        createUI();
    }
    boolean pressingMouse = false;
    boolean detectedOnceInPerimiters = false;
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
        Global.getSector().removeScript(influencer);
        Global.getSector().getListenerManager().removeListener(influencer);
        influencer.setMaxProgress(100);
        this.progress = currProgress;

        createUI();

    }
    public ProgressBarComponentV2(float width, float height, String barText,String barTextFont, Color barColor, Color barBrackets,int currSegment,int maxSegments,int minSegments){
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
        this.minSection = minSegments;
        influencer.setMaxProgress(maxSegments);
        sliderMode = true;
        this.currentSection = currSegment;
        this.sections = maxSegments;
        Global.getSector().removeScript(influencer);
        Global.getSector().getListenerManager().removeListener(influencer);
        createUI();

    }
    public CustomPanelAPI getRenderingPanel(){
        return getMainPanel();
    }
    public ProgressBarComponentV2(float width, float height, float currProgress,Color barColor){
        this(width, height, null, null, Misc.getDarkPlayerColor().brighter(), Misc.getBasePlayerColor(), currProgress);
    }

    public ProgressBarComponentV2(float width, float height,int currentSegment, int maxSegments,Color progressionColor,int minSection) {
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
                return Misc.getBasePlayerColor();
            }

            @Override
            public Color getBarColor() {
                Color color = progressionColor;
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
        this.minSection = minSection;
        influencer.setMaxProgress(maxSegments);
        sliderMode = true;
        this.currentSection = currentSegment;
        this.sections = maxSegments;
        Global.getSector().removeScript(influencer);
        Global.getSector().getListenerManager().removeListener(influencer);
        createUI();

    }
    @Override
    public CustomPanelAPI getMainPanel() {
        return mainPanel;
    }

    @Override
    public void createUI() {

        if(sliderMode){
            influencer.setProgress(currentSection);
        }
        else{
            int percentage = Math.round(progress*100);
            influencer.setProgress(percentage);
        }

        CustomPanelAPI componentPanel = Global.getSettings().createCustom(mainPanel.getPosition().getWidth(),mainPanel.getPosition().getHeight(),null);
        TooltipMakerAPI tooltip = componentPanel.createUIElement(componentPanel.getPosition().getWidth(),componentPanel.getPosition().getHeight(),false);
        progressBar = tooltip.addEventProgressBar(influencer,0f);
        tooltip.setParaFont(barTextFont);
        if(barText!=null){
            progressLabel = tooltip.createLabel(barText,Misc.getTextColor());
            influenceLabel();
            float y = progressBar.getPosition().getHeight();
            float middle = y/2;
            progressBar.addComponent((UIComponentAPI) progressLabel).inMid();
        }

        componentPanel.addUIElement(tooltip).inTL(0,0);
        mainPanel.addComponent(componentPanel).inTL(0,0);
        if(this.componentPanel!=null){
            mainPanel.removeComponent(this.componentPanel);
        }
        this.componentPanel = componentPanel;


    }

    public void setBarText(String barText) {
        this.barText = barText;
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
        if(sliderMode){
            float x =progressBar.getXCoordinateForProgress(currentSection);
            arrows.setColor(Misc.getBasePlayerColor());
            arrows.setAngle(0);
            arrows.renderAtCenter(x,progressBar.getPosition().getY()+progressBar.getPosition().getHeight()+5);
            arrows.setAngle(180);
            arrows.renderAtCenter(x,progressBar.getPosition().getY()-5);
        }

    }
    public boolean haveMovedToAnotherSegment(){
        return haveMovedToAnotherSegment;
    }

    public void setHaveMovedToAnotherSegment(boolean haveMovedToAnotherSegment) {
        this.haveMovedToAnotherSegment = haveMovedToAnotherSegment;
    }

    @Override
    public void render(float alphaMult) {

    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if(sliderMode){
            float affectedWidth = progressBar.getPosition().getWidth()-7;
            float sectionWidth = affectedWidth/sections;
            float mouseX = Global.getSettings().getMouseX();
            float mouseY = Global.getSettings().getMouseY();

            for (InputEventAPI event : events) {
                float topY = progressBar.getPosition().getY()+progressBar.getPosition().getHeight();
                float bottomY = progressBar.getPosition().getY();
                if(event.isConsumed())continue;
                if(event.isLMBDownEvent())pressingMouse = true;
                if(event.isLMBUpEvent()){
                    pressingMouse = false;
                    detectedOnceInPerimiters = false;
                }
                if(pressingMouse){
                    float prevX = progressBar.getPosition().getX()+sectionWidth+10;
                    if(detectedOnceInPerimiters){
                        topY = Global.getSettings().getScreenHeight();
                        bottomY = 0;
                    }
                    for (int i = minSection; i <= sections; i++) {
                        float currSection = sectionWidth*i;
                        if(detector.determineIfHoversOverButton(prevX,topY,currSection,topY,prevX,bottomY,currSection,bottomY,mouseX,mouseY)){
                            detectedOnceInPerimiters = true;
                            currentSection = i;
                            createUI();
                            haveMovedToAnotherSegment = true;
                            break;
                        }
                        prevX += sectionWidth;
                    }
                }
                if(event.getEventValue()== Keyboard.KEY_LEFT){
                    if(!passFirst){
                        currentSection--;

                        if(currentSection<=minSection){
                            currentSection = minSection;
                       ;
                        }
                        createUI();
                        haveMovedToAnotherSegment = true;
                        passFirst = true;
                    }
                    else{
                        passFirst = false;
                    }

                }
                if(event.getEventValue()== Keyboard.KEY_RIGHT){
                    if(!passFirst){
                        currentSection++;

                        if(currentSection >= sections){
                            currentSection = sections;

                        }
                        haveMovedToAnotherSegment = true;
                        passFirst = true;
                        createUI();
                    }
                    else{
                        passFirst = false;
                    }


                }

                event.consume();

            }
        }

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }
}
