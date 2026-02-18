package ashlib.data.plugins.ui.models;

import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.ui.plugins.UILinesRenderer;
import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.vok.plugins.ReflectionUtilis;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CenterPopUpUI implements CustomUIPanelPlugin {

    public IntervalUtil betweenCodex = null;
    public boolean detectedCodex = false;
    public int limit = 10;
    protected Fader fader = null; // Purple Nebula

    SpriteAPI blackBackground = Global.getSettings().getSprite("rendering","GlitchSquare");
    SpriteAPI borders = Global.getSettings().getSprite("rendering","GlitchSquare");
    SpriteAPI panelBackground  = Global.getSettings().getSprite("ui","panel00_center");
    SpriteAPI bottomPanelEdge = Global.getSettings().getSprite("ui","panel00_bot");
    SpriteAPI topPanelEdge = Global.getSettings().getSprite("ui","panel00_top");
    SpriteAPI leftPanelEdge = Global.getSettings().getSprite("ui","panel00_left");
    SpriteAPI rightPanelEdge = Global.getSettings().getSprite("ui","panel00_right");
    SpriteAPI topLeftPanelCorner = Global.getSettings().getSprite("ui","panel00_top_left");
    SpriteAPI topRightPanelCorner = Global.getSettings().getSprite("ui","panel00_top_right");
    SpriteAPI bottomLeftPanelCorner = Global.getSettings().getSprite("ui","panel00_bot_left");
    SpriteAPI bottomRightPanelCorner = Global.getSettings().getSprite("ui","panel00_bot_right");

    public static float buttonConfirmWidth = 160;
    UIPanelAPI parentUIPanel;
    public float frames;
    public CustomPanelAPI panelToInfluence;
    public ArrayList<TooltipMakerAPI> mainTooltips = new ArrayList<>();
    public ArrayList<CustomPanelAPI> mainPanels = new ArrayList<>();
    public UILinesRenderer borderRenderer = new UILinesRenderer(0f);
    public ButtonAPI confirmButton;
    public ButtonAPI cancelButton;

    public ButtonAPI getConfirmButton() {
        return confirmButton;
    }
    public ButtonAPI getCancelButton() {
        return cancelButton;
    }

    public CustomPanelAPI getPanelToInfluence() {
        return panelToInfluence;
    }

    public boolean isDialog =true;
    public boolean reachedMaxHeight =  false;
    public boolean pressedConfirmCancel = false; // Purple Nebula

    float goalSizeX, goalSizeY;
    float x,y;
    float initX,initY; // Purple Nebula

    public List<UILinesRenderer> linesRenderers = new ArrayList<>();

    /**
     * Pop Up UI based on Ashlib PopUpUI
     */
    public CenterPopUpUI() {
    }

    public void addTooltip(TooltipMakerAPI tooltipMakerAPI){
        if(mainTooltips==null){
            mainTooltips = new ArrayList<>();
        }
        mainTooltips.add(tooltipMakerAPI);
    }
    public void addPanel(CustomPanelAPI customPanelAPI) {
        if (mainPanels==null){
            mainPanels = new ArrayList<>();
        }
        mainPanels.add(customPanelAPI);
    }
    //  Purple Nebula
    public void removeUI() {
        linesRenderers.clear();

        for (TooltipMakerAPI mainTooltip : mainTooltips) {
            mainTooltip.setOpacity(0f);
        }
        mainTooltips.clear();

        for (CustomPanelAPI mainPanel : mainPanels) {
            mainPanel.setOpacity(0f);
        }
        mainPanels.clear();

        if (confirmButton != null) {
            confirmButton.setOpacity(0);
        }
        if (cancelButton != null) {
            cancelButton.setOpacity(0);
        }
    }


    public void init(CustomPanelAPI panelAPI, float x, float y, boolean isDialog) {
        panelToInfluence = panelAPI;
        parentUIPanel =  PopUpUI.ProductionUtil.getCoreUI();
        //  Purple Nebula
        if (ReflectionUtilis.hasMethodOfName("getFader",this.panelToInfluence)) {
            this.fader = (Fader) ReflectionUtilis.invokeMethod("getFader",this.panelToInfluence);
        }
        goalSizeX = panelAPI.getPosition().getWidth();
        goalSizeY = panelAPI.getPosition().getHeight();

        panelToInfluence.getPosition().setSize(16,16);
        this.isDialog = isDialog;

        this.x = x;
        this.y = y;
        initX = x; // Purple Nebula
        initY = this.parentUIPanel.getPosition().getHeight() - y; // Purple Nebula

        parentUIPanel.addComponent(panelToInfluence).inTL(x, (parentUIPanel.getPosition().getHeight()-y)*2);
        parentUIPanel.bringComponentToTop(panelToInfluence);
        borderRenderer.setPanel(panelToInfluence);
        if (fader != null) fader.setBrightness(0.1f);

    }
    public void initForDialog(CustomPanelAPI panelAPI,float x, float y,boolean isDialog) {
        panelToInfluence = panelAPI;
        parentUIPanel =  PopUpUI.ProductionUtil.getCoreUIForDialog();
        //  Purple Nebula
        if (ReflectionUtilis.hasMethodOfName("getFader",this.panelToInfluence)) {
            this.fader = (Fader) ReflectionUtilis.invokeMethod("getFader",this.panelToInfluence);
        }
        goalSizeX = panelAPI.getPosition().getWidth();
        goalSizeY = panelAPI.getPosition().getHeight();

        panelToInfluence.getPosition().setSize(16,16);
        this.isDialog = isDialog;

        initX = x; // Purple Nebula
        initY = this.parentUIPanel.getPosition().getHeight() - y; // Purple Nebula

        parentUIPanel.addComponent(panelToInfluence).inTL(x, (parentUIPanel.getPosition().getHeight()-y)*2);
        parentUIPanel.bringComponentToTop(panelToInfluence);
        borderRenderer.setPanel(panelToInfluence);
        if (fader != null) fader.setBrightness(0.1f); // Purple Nebula

    }

    public void createUI(CustomPanelAPI panelAPI){
        //Note here is where you create UI : Methods you need to change is advance , createUI, and inputEvents handler
        //Also remember super.apply()
    }
    public float createUIMockup(CustomPanelAPI panelAPI){
        return 0f;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if(panelToInfluence != null){
            TiledTextureRenderer renderer = new TiledTextureRenderer(panelBackground.getTextureId());
            if(isDialog){
                blackBackground.setSize(PopUpUI.ProductionUtil.getCoreUI().getPosition().getWidth(), PopUpUI.ProductionUtil.getCoreUI().getPosition().getHeight());
                blackBackground.setColor(Color.black);
                blackBackground.setAlphaMult(fader.getBrightness()/2);
                blackBackground.renderAtCenter(PopUpUI.ProductionUtil.getCoreUI().getPosition().getCenterX(), PopUpUI.ProductionUtil.getCoreUI().getPosition().getCenterY());
                renderer.renderTiledTexture(panelToInfluence.getPosition().getX(), panelToInfluence.getPosition().getY(), panelToInfluence.getPosition().getWidth(),  panelToInfluence.getPosition().getHeight(), panelBackground.getTextureWidth(),  panelBackground.getTextureHeight(),(frames/limit)*0.9F,Color.BLACK);
            }
            else {
                renderer.renderTiledTexture(panelToInfluence.getPosition().getX(), panelToInfluence.getPosition().getY(), panelToInfluence.getPosition().getWidth(),  panelToInfluence.getPosition().getHeight(), panelBackground.getTextureWidth(),  panelBackground.getTextureHeight(),(frames/limit),panelBackground.getColor());

            }
            if(isDialog){
                renderBorders(panelToInfluence);
            }
            else{
                borderRenderer.render(alphaMult);
            }
        }
    }

    @Override
    public void render(float alphaMult) {
        for (UILinesRenderer linesRenderer : linesRenderers) {
            linesRenderer.render(alphaMult);
        }
    }

    float goalYOffset;
    float expandOffset;
    boolean didOnceOne = false;
    boolean didOnceTwo = false;
    @Override
    public void advance(float amount) {

        if(betweenCodex!=null){
            betweenCodex.advance(amount);
            if(betweenCodex.intervalElapsed()){
                betweenCodex = null;
            }
        }
        /**
         * A = ReflectionUtils.getCoreUI().getPosition().getCenterY()*2 = full height res of gameplay screen
         * B = panelAPI.getPosition().getHeight(); = 250 = (max) size of panel
         * C = A - B = 900 - 250 = 650 (in case of full heigh res being 900) leftover Y spacing
         * D = 650 / 2 = 325 Y spacing above and below the panel = goalYOffset
         * E = ReflectionUtils.getCoreUI().getPosition().getCenterY() - D = 125 = top and bottom panel edge distance from center
         * ^ Distance top and bottom need to travel to reach target Y position
         */
        float screenHeight = PopUpUI.ProductionUtil.getCoreUI().getPosition().getCenterY()*2; /// A
        float dialogPanelHeight = goalSizeY; /// B
        float leftoverYSpacingTotal = screenHeight - dialogPanelHeight; /// C
        float leftoverYSpacingPartial = leftoverYSpacingTotal/2; /// D
        float yDistanceFromCenter = PopUpUI.ProductionUtil.getCoreUI().getPosition().getCenterY() - leftoverYSpacingPartial; /// E

        /**
         * A = Global.getSettings().getScreenHeight() = full height res of gameplay screen = screenHeight2
         * B = goalSizeY = panelAPI.getPosition().getHeight(); = 250 = (max) height of panel = dialogPanelHeight2
         * C = Y location of expanded panel = y = yPostExpanded
         * D = distance to expand from C = B / 2 = yDistanceToExpand
         * E = Y location of retracted panel = C + D = yPosRetracted
         */
        float screenHeight2 = Global.getSettings().getScreenHeight(); /// A
        float dialogPanelHeight2 = goalSizeY; /// B
        float yPostExpanded = y; /// C
        float yDistanceToExpand = dialogPanelHeight2/2; /// D
        float yPosRetracted = yPostExpanded + yDistanceToExpand; /// E
        float leftoverBottomYSpacing2 = screenHeight2 - yPostExpanded - dialogPanelHeight2;

        // Purple Nebula
        // Draw and resize panel
        if (!didOnceOne) {
//            this.panelToInfluence.getPosition().setYAlignOffset(-initY*2);
            goalYOffset = -yPostExpanded; // -initY // Expanded Y
            expandOffset = -yPosRetracted; // goalYOffset*2 // Retracted Y
            didOnceOne = true;
        }
        if (!pressedConfirmCancel) {
            if (this.frames <= (float) this.limit) {
                ++this.frames;
                expandOffset += Math.abs(yDistanceToExpand / limit);
                float progress = this.frames / (float) this.limit;
                if (this.frames < (float) this.limit && !this.reachedMaxHeight) {
                    fader.setDurationIn((float) limit / 20);
                    fader.fadeIn();
                    this.panelToInfluence.getPosition().setYAlignOffset(expandOffset);
                    this.panelToInfluence.getPosition().setSize(this.goalSizeX, this.goalSizeY * progress);
                    return;
                }

                if (this.frames >= (float) this.limit && !this.reachedMaxHeight) {
                    this.reachedMaxHeight = true;
                    this.panelToInfluence.getPosition().setYAlignOffset(-yPostExpanded);
                    this.panelToInfluence.getPosition().setSize(this.goalSizeX, this.goalSizeY);
                    this.createUI(this.panelToInfluence);
                    return;
                }
            }
        }


        // Default/Original behaviour
//        if(frames<=limit){
//            frames++;
//            float progress = frames/limit;
//            if(frames<limit&&!reachedMaxHeight){
//                panelToInfluence.getPosition().setSize(originalSizeX,originalSizeY*progress);
//                return;
//            }
//            if(frames>=limit&&!reachedMaxHeight){
//                reachedMaxHeight = true;
//                panelToInfluence.getPosition().setSize(originalSizeX,originalSizeY);
//                createUI(panelToInfluence);
//                return;
//
//            }
//        }

        if(confirmButton!=null){
            if(confirmButton.isChecked()){
                confirmButton.setChecked(false);
                applyConfirmScript();
                pressedConfirmCancel = true; // Purple Nebula
//                this.parent.removeComponent(this.panelToInfluence);
//                this.onExit();
            }
        }
        if(cancelButton!=null){
            if(cancelButton.isChecked()){
                cancelButton.setChecked(false);
                pressedConfirmCancel = true; // Purple Nebula
//                this.parent.removeComponent(this.panelToInfluence);
//                this.onExit();
            }

        }
        if (pressedConfirmCancel) {
            if (!didOnceTwo) {
                removeUI();
                this.frames = 10;
                didOnceTwo = true;
            }
            if (this.frames >= 0) {
                --this.frames;
                expandOffset -= Math.abs(yDistanceToExpand / limit); // goalYOffset / ((float) limit *2)
                float progress = this.frames / (float) (limit);
                if (this.frames > 0) {
//                    fader.setDurationOut((float) limit / 60);
                    fader.setDurationOut((float) 0.05);
                    fader.fadeOut();
//                fader.setBrightness(test);
//                test+=per;
                    this.panelToInfluence.getPosition().setYAlignOffset(expandOffset);
                    this.panelToInfluence.getPosition().setSize(this.goalSizeX, this.goalSizeY * progress);

                    return;
                }
            }
//            this.blackBackground.setAlphaMult(this.blackBackground.getAlphaMult()-toRemoveAlpha);
            if (!fader.isFadingOut()) {
                pressedConfirmCancel = false;
                this.parentUIPanel.removeComponent(this.panelToInfluence);
                this.onExit();
            }

        }


        if(Global.CODEX_TOOLTIP_MODE){
            detectedCodex = true;
        }
        if(!Global.CODEX_TOOLTIP_MODE&&detectedCodex){
            detectedCodex = false;
            betweenCodex = new IntervalUtil(0.1f,0.1f);
        }

    }

    public void applyConfirmScript(){

    }

    @Override
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if(betweenCodex!=null)return;
        for (InputEventAPI event : events) {
            if(frames>=limit-1&&reachedMaxHeight){
                if(event.isMouseDownEvent()&&!isDialog){
                    TrapezoidButtonDetector detector = new TrapezoidButtonDetector();
                    float xLeft = panelToInfluence.getPosition().getX();
                    float xRight = panelToInfluence.getPosition().getX()+panelToInfluence.getPosition().getWidth();
                    float yBot = panelToInfluence.getPosition().getY();
                    float yTop = panelToInfluence.getPosition().getY()+panelToInfluence.getPosition().getHeight();
                    boolean hovers = detector.determineIfHoversOverButton(xLeft,yTop,xRight,yTop,xLeft,yBot,xRight,yBot,Global.getSettings().getMouseX(),Global.getSettings().getMouseY());
                    if(!hovers){
                        if(cancelButton!=null){
                            cancelButton.setChecked(true);
                        }
                        else{
                            pressedConfirmCancel = true;
                        }
                    }
                }
                if(!event.isConsumed()){
                    if(event.getEventValue()== Keyboard.KEY_ESCAPE&&!event.isMouseEvent()&&event.isKeyDownEvent()){
                        if(cancelButton!=null){
                            cancelButton.setChecked(true);
                        }
                        else{
                            pressedConfirmCancel = true;
                        }

                        event.consume();
                        break;




                    }
                }
            }
            event.consume();
        }

    }

    public void forceDismiss(){
        parentUIPanel.removeComponent(panelToInfluence);
        onExit();
    }
    public void onExit(){

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }

    public void renderBorders(CustomPanelAPI panelAPI) {
        float leftX = panelAPI.getPosition().getX()+16;
        float currAlpha = (frames/limit)*0.9F;
        if(currAlpha>=1)currAlpha =1;

        topPanelEdge.setSize(16,16);
        bottomPanelEdge.setSize(16,16);
        topLeftPanelCorner.setSize(16,16);
        topRightPanelCorner.setSize(16,16);
        bottomLeftPanelCorner.setSize(16,16);
        bottomRightPanelCorner.setSize(16,16);
        leftPanelEdge.setSize(16,16);
        rightPanelEdge.setSize(16,16);

        topPanelEdge.setAlphaMult(currAlpha);
        bottomPanelEdge.setAlphaMult(currAlpha);
        topLeftPanelCorner.setAlphaMult(currAlpha);
        topRightPanelCorner.setAlphaMult(currAlpha);
        bottomLeftPanelCorner.setAlphaMult(currAlpha);
        bottomRightPanelCorner.setAlphaMult(currAlpha);
        leftPanelEdge.setAlphaMult(currAlpha);
        rightPanelEdge.setAlphaMult(currAlpha);

        float rightX = panelAPI.getPosition().getX()+panelAPI.getPosition().getWidth()-16;
        float botX = panelAPI.getPosition().getY()+16;
        AshMisc.startStencilWithXPad(panelAPI,8);
        for (float i = leftX; i <= panelAPI.getPosition().getX()+panelAPI.getPosition().getWidth() ; i+= topPanelEdge.getWidth()) {
            topPanelEdge.renderAtCenter(i,panelAPI.getPosition().getY()+panelAPI.getPosition().getHeight());
            bottomPanelEdge.renderAtCenter(i,panelAPI.getPosition().getY());
        }
        AshMisc.endStencil();
        AshMisc.startStencilWithYPad(panelAPI,8);
        for (float i = botX; i <= panelAPI.getPosition().getY()+panelAPI.getPosition().getHeight();  i+= topPanelEdge.getWidth()) {
            leftPanelEdge.renderAtCenter(panelAPI.getPosition().getX(),i);
            rightPanelEdge.renderAtCenter(panelAPI.getPosition().getX()+panelAPI.getPosition().getWidth(),i);
        }
        AshMisc.endStencil();
        topLeftPanelCorner.renderAtCenter(leftX-16,panelAPI.getPosition().getY()+panelAPI.getPosition().getHeight());
        topRightPanelCorner.renderAtCenter( panelAPI.getPosition().getX()+panelAPI.getPosition().getWidth(),panelAPI.getPosition().getY()+panelAPI.getPosition().getHeight());
        bottomLeftPanelCorner.renderAtCenter(leftX-16,panelAPI.getPosition().getY());
        bottomRightPanelCorner.renderAtCenter( panelAPI.getPosition().getX()+panelAPI.getPosition().getWidth(),panelAPI.getPosition().getY());
    }

    public ButtonAPI generateConfirmButton(TooltipMakerAPI tooltip){
        ButtonAPI button = tooltip.addButton("Confirm","confirm", Misc.getBasePlayerColor(),Misc.getDarkPlayerColor(),Alignment.MID,CutStyle.TL_BR,160,25,0f);
        button.setShortcut(Keyboard.KEY_G,true);
        confirmButton = button;
        return button;
    }

    public ButtonAPI generateCancelButton(TooltipMakerAPI tooltip){
        ButtonAPI button = tooltip.addButton("Cancel","cancel", Misc.getBasePlayerColor(),Misc.getDarkPlayerColor(),Alignment.MID,CutStyle.TL_BR,buttonConfirmWidth,25,0f);
        button.setShortcut(Keyboard.KEY_ESCAPE,true);
        cancelButton = button;
        return button;
    }

    public void createConfirmAndCancelSection(CustomPanelAPI mainPanel){
        float totalWidth = buttonConfirmWidth*2+10;
        TooltipMakerAPI tooltip = mainPanel.createUIElement(totalWidth,25,false);
        tooltip.setButtonFontOrbitron20();
        generateConfirmButton(tooltip);
        generateCancelButton(tooltip);
        confirmButton.getPosition().inTL(0,0);
        cancelButton.getPosition().inTL(buttonConfirmWidth+5,0);
        float bottom = goalSizeY;
        mainPanel.addUIElement(tooltip).inTL(mainPanel.getPosition().getWidth()-(totalWidth)-10,bottom-40);
    }

}
