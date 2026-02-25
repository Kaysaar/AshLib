package ashlib.data.plugins.ui.models;

import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.ui.plugins.UILinesRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;

public class OptionsEventPopUpDialog extends PopUpUI {

    TooltipMakerAPI headerTooltip;
    String title;
    public float x,y;

    private String optionOneText = "Option 1";
    private String optionTwoText = "Option 2";
    private String optionThreeText = "Option 3";

    private BaseTooltipCreator optionOneTooltip = null;
    private BaseTooltipCreator optionTwoTooltip = null;
    private BaseTooltipCreator optionThreeTooltip = null;

    public CustomPanelAPI mainPanel;
    private final String illustrationSprite;
    Description eventDescription;
    UILinesRenderer imageOutlineRenderer = new UILinesRenderer(0f);

    private ButtonAPI optionOne, optionTwo, optionThree;
    private LabelAPI optionOneLabel, optionTwoLabel, optionThreeLabel;

    /**
     * Base/Default EventPUDType.COMMS_EVENT
     * @param illustrationSprite Sprite object of an illustration
     * @param eventDescription The event description, utilizes the descriptions.csv - use "Global.getSettings().getDescription("yourDescriptionId", Description.Type.CUSTOM)"
     */
    public OptionsEventPopUpDialog(String illustrationSprite, Description eventDescription) {
        this.title = "Incoming Transmission";
        x=15;
        y=45;
        this.illustrationSprite = illustrationSprite;
        this.eventDescription = eventDescription;
    }
    /**
     * EventPUDType.COMMS_EVENT with custom title
     * @param headerTitle Custom title of the "Incoming Intermission" event
     * @param illustrationSprite Sprite object of an illustration
     * @param eventDescription The event description, utilizes the descriptions.csv - use "Global.getSettings().getDescription("yourDescriptionId", Description.Type.CUSTOM)"
     */
    public OptionsEventPopUpDialog(String headerTitle, String illustrationSprite, Description eventDescription) {
        this.title = headerTitle;
        x=15;
        y=45;
        this.illustrationSprite = illustrationSprite;
        this.eventDescription = eventDescription;
    }

    @Override
    public void createUI(CustomPanelAPI panelAPI) {
        createHeader(panelAPI);

        TooltipMakerAPI tooltip = panelAPI.createUIElement(panelAPI.getPosition().getWidth()-30,panelAPI.getPosition().getHeight()-y,true);
        createContentForDialog(tooltip,panelAPI.getPosition().getWidth()-30);
        addTooltip(tooltip);
        panelAPI.addUIElement(tooltip).inTL(x,y);
//        createConfirmAndCancelSection(panelAPI);
    }

    public void createOptionsSection(CustomPanelAPI mainPanel) {
//        float totalWidth = buttonConfirmWidth * 2.0F + 10.0F;
        float totalWidth = mainPanel.getPosition().getWidth() - 10.0F;
        TooltipMakerAPI tooltip = mainPanel.createUIElement(totalWidth, 25.0F, false);
        tooltip.setButtonFontOrbitron20();
        this.generateOptionButtons(tooltip);
        this.optionOne.getPosition().inTL(0.0F, -25.0F).setSize(totalWidth, 25.0F);
        this.optionTwo.getPosition().inTL(0.0F, 10.0F).setSize(totalWidth, 25.0F);
        this.optionThree.getPosition().inTL(0.0F, 45).setSize(totalWidth, 25.0F);
        this.optionOneLabel.getPosition().inTL(45f, -25.0f + 2.5f);
        this.optionTwoLabel.getPosition().inTL(45f, 10.0f + 2.5f);
        this.optionThreeLabel.getPosition().inTL(45f, 45f + 2.5f);

        float bottom = mainPanel.getPosition().getHeight();
        mainPanel.addUIElement(tooltip).inTL(mainPanel.getPosition().getWidth() - totalWidth - 10.0F, bottom - 40.0F);
    }
    public void generateOptionButtons(TooltipMakerAPI tooltip) {
        optionOne = tooltip.addButton("", "optionOne", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.LMID, CutStyle.TL_BR, 160.0F, 25.0F, 0.0F);
        optionTwo = tooltip.addButton("", "optionTwo", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.LMID, CutStyle.TL_BR, 160.0F, 25.0F, 0.0F);
        optionThree = tooltip.addButton("", "optionThree", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.LMID, CutStyle.TL_BR, 160.0F, 25.0F, 0.0F);

        try {
            Global.getSettings().loadFont("graphics/fonts/insignia21LTaa.fnt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        tooltip.setParaFont("graphics/fonts/insignia21LTaa.fnt");
        optionOneLabel = tooltip.addPara(" " + optionOneText,Misc.getBasePlayerColor(),0f);
        optionTwoLabel = tooltip.addPara(" " + optionTwoText,Misc.getBasePlayerColor(),0f);
        optionThreeLabel = tooltip.addPara(" " + optionThreeText,Misc.getBasePlayerColor(),0f);
        tooltip.setParaFontDefault();

        optionOne.setShortcut(Keyboard.KEY_1, false);
        optionTwo.setShortcut(Keyboard.KEY_2, false);
        optionThree.setShortcut(Keyboard.KEY_3, false);


        if (optionOneTooltip != null) tooltip.addTooltipTo(optionOneTooltip,optionOne, TooltipMakerAPI.TooltipLocation.ABOVE);
        if (optionTwoTooltip != null) tooltip.addTooltipTo(optionTwoTooltip,optionTwo, TooltipMakerAPI.TooltipLocation.ABOVE);
        if (optionThreeTooltip != null) tooltip.addTooltipTo(optionThreeTooltip,optionThree, TooltipMakerAPI.TooltipLocation.ABOVE);
    }

    public void setOptionsTooltips(BaseTooltipCreator optionOneTooltip, BaseTooltipCreator optionTwoTooltip, BaseTooltipCreator optionThreeTooltip) {
        this.optionOneTooltip = optionOneTooltip;
        this.optionTwoTooltip = optionTwoTooltip;
        this.optionThreeTooltip = optionThreeTooltip;
    }

    public void createHeader(CustomPanelAPI panelAPI) {
        if(AshMisc.isStringValid(title)) {
            headerTooltip = panelAPI.createUIElement(panelAPI.getPosition().getWidth()-30,20,false);
            headerTooltip.setParaFont(Fonts.ORBITRON_20AABOLD);
            LabelAPI label = headerTooltip.addPara(title, Misc.getBasePlayerColor(),5f);
            panelAPI.addUIElement(headerTooltip).inTL(15,10); // 15 10
            float width = label.computeTextWidth(label.getText());
            label.getPosition().setLocation(0,0).inTL(0,0);
            label.setAlignment(Alignment.LMID);
        }
        else {
            y = 10;
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);
        if (headerTooltip != null) {
//            AshMisc.drawRectangleFilledForTooltip(headerTooltip,fader.getBrightness(), Global.getSector().getPlayerFaction().getDarkUIColor().darker());
            Color uiColor = Global.getSector().getPlayerFaction().getDarkUIColor().darker();
            float x = headerTooltip.getPosition().getX();
            float y = headerTooltip.getPosition().getY();
            float w = headerTooltip.getPosition().getWidth();
            float h = headerTooltip.getPosition().getHeight() / 8;
            GL11.glPushMatrix();
            GL11.glDisable(3553);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glColor4f((float) uiColor.getRed() / 255.0F, (float) uiColor.getGreen() / 255.0F, (float) uiColor.getBlue() / 255.0F, (float) uiColor.getAlpha() / 255.0F * fader.getBrightness() * 23.0F);
            GL11.glRectf(x, y, x + w, y + h);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }
    }

    public void createContentForDialog(TooltipMakerAPI tooltip, float width){

        int illuWidth = 240;
        int illuHeight = 150;


        int separationMargin = 7;
        float textAreaXpos = illuWidth + separationMargin;
        float textAreaWidth = width-illuWidth-separationMargin;
        float titleContentHeight = 40;
        float contentMaxHeight = panelToInfluence.getPosition().getHeight()-95;

        mainPanel = Global.getSettings().createCustom(width, contentMaxHeight, this);

        TooltipMakerAPI illustrationTooltip = mainPanel.createUIElement(illuWidth, contentMaxHeight, false);
        CustomPanelAPI imagePanel = Global.getSettings().createCustom(illuWidth, illuHeight, null);

        TooltipMakerAPI imageTooltip = imagePanel.createUIElement(illuWidth, illuHeight,false);
        imageTooltip.addImage(illustrationSprite,illuWidth, illuHeight,0f);
        imagePanel.addUIElement(imageTooltip).inTL(-5,0);
        illustrationTooltip.addCustom(imagePanel,0f);

        mainPanel.addUIElement(illustrationTooltip).inTL(-5,0);

        imageOutlineRenderer.setPanel(imagePanel);
        imageOutlineRenderer.setBoxColor(Global.getSettings().getDarkPlayerColor());
        addInternalLinesRenderer(imageOutlineRenderer);

        TooltipMakerAPI textTooltip = mainPanel.createUIElement(textAreaWidth, illuHeight+7.5f, true);
        try {
            Global.getSettings().loadFont("graphics/fonts/insignia17LTaa.fnt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        textTooltip.setParaFont("graphics/fonts/insignia17LTaa.fnt");

        if (eventDescription != null && eventDescription.hasText1() && !eventDescription.getText1().isEmpty()) {
            if (eventDescription.hasText2() && !eventDescription.getText2().isEmpty()) {
                String[] highlights = getDescHighlights(eventDescription);
                textTooltip.addPara(eventDescription.getText1(),5f,Misc.getHighlightColor(),highlights);
            }
            else {
                textTooltip.addPara(eventDescription.getText1(),5f);
            }
        }
        else {

            textTooltip.addPara("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",5f);
            textTooltip.addPara("Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.",5f);
        }
        textTooltip.setParaFontDefault();
        mainPanel.addUIElement(textTooltip).inTL(textAreaXpos,-5);


        createOptionsSection(mainPanel);

        tooltip.addCustom(mainPanel,0f).getPosition().inTL(0,0);
    }

    public String[] getDescHighlights(Description eventDescription) {
        return eventDescription.getText2().split(",");
    }


    @Override
    public void applyConfirmScript() {

    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
    }


    public void setOptionOneText(String optionOneText) {
        this.optionOneText = optionOneText;
    }

    public void setOptionTwoText(String optionTwoText) {
        this.optionTwoText = optionTwoText;
    }

    public void setOptionThreeText(String optionThreeText) {
        this.optionThreeText = optionThreeText;
    }
}
