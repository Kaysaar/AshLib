package ashlib.data.plugins.ui.models;

import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.ui.plugins.UILinesRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;

public class CommsPopUpDialog extends PopUpUI {

    TooltipMakerAPI headerTooltip;
    String title;
    public float x,y;

    public CustomPanelAPI mainPanel;
    private final PersonAPI commsPerson;
    Description eventDescription;
    UILinesRenderer imageOutlineRenderer = new UILinesRenderer(0f);

    /**
     * Base/Default EventPUDType.COMMS_EVENT
     * @param commsPerson Person trying to communicate with you
     * @param eventDescription The event description, utilizes the descriptions.csv - use "Global.getSettings().getDescription("yourDescriptionId", Description.Type.CUSTOM)"
     */
    public CommsPopUpDialog(PersonAPI commsPerson, Description eventDescription) {
        this.title = "Incoming Transmission";
        x=15;
        y=45;
        this.commsPerson = commsPerson;
        this.eventDescription = eventDescription;
    }
    /**
     * EventPUDType.COMMS_EVENT with custom title
     * @param headerTitle Custom title of the "Incoming Intermission" event
     * @param commsPerson Person trying to communicate with you
     * @param eventDescription The event description, utilizes the descriptions.csv - use "Global.getSettings().getDescription("yourDescriptionId", Description.Type.CUSTOM)"
     */
    public CommsPopUpDialog(String headerTitle, PersonAPI commsPerson, Description eventDescription) {
        this.title = headerTitle;
        x=15;
        y=45;
        this.commsPerson = commsPerson;
        this.eventDescription = eventDescription;
    }

    @Override
    public void createUI(CustomPanelAPI panelAPI) {
        createHeader(panelAPI);

        TooltipMakerAPI tooltip = panelAPI.createUIElement(panelAPI.getPosition().getWidth()-30,panelAPI.getPosition().getHeight()-y,true);
        createContentForDialog(tooltip,panelAPI.getPosition().getWidth()-30);
        addTooltip(tooltip);
        panelAPI.addUIElement(tooltip).inTL(x,y);
        if (confirmOnly) createConfirmSection(panelAPI);
        else createConfirmAndCancelSection(panelAPI);
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



        int iconSize = 128;
        int separationMargin = 7;
        float textAreaWidth = width-iconSize-separationMargin;
        float titleContentHeight = 35;
        float contentMaxHeight = panelToInfluence.getPosition().getHeight()-95;

        mainPanel = Global.getSettings().createCustom(width, contentMaxHeight, this);

        TooltipMakerAPI imageRelTooltip = mainPanel.createUIElement(iconSize, contentMaxHeight, false);

        CustomPanelAPI imagePanel = Global.getSettings().createCustom(iconSize, iconSize, null);
        TooltipMakerAPI imageTooltip = imagePanel.createUIElement(iconSize,iconSize,false);
        imageTooltip.addImage(commsPerson.getPortraitSprite(),iconSize,0f);
        imagePanel.addUIElement(imageTooltip).inTL(-5,0);
        imageRelTooltip.addCustom(imagePanel,0f);
        imageRelTooltip.addRelationshipBar(commsPerson,iconSize,5f);

        mainPanel.addUIElement(imageRelTooltip).inTL(0,0);

        imageOutlineRenderer.setPanel(imagePanel);
        imageOutlineRenderer.setBoxColor(commsPerson.getFaction().getDarkUIColor());
//        linesRenderers.add(imageOutlineRenderer);
        addInternalLinesRenderer(imageOutlineRenderer);

        TooltipMakerAPI personTitleTooltip = mainPanel.createUIElement(textAreaWidth, 40, false);
        // Set up person title string
        String tempString = commsPerson.getNameString() + ", " + commsPerson.getPost();
        String personTitleString = "";
        if (tempString.length()>40) {
            personTitleString = tempString.substring(0,39);
            personTitleString+="...";
        }
        else {
            personTitleString = tempString;
        }
        // Try to load a font
        try {
            Global.getSettings().loadFont("graphics/fonts/insignia21LTaa.fnt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        personTitleTooltip.setParaFont("graphics/fonts/insignia21LTaa.fnt");
        personTitleTooltip.addPara(personTitleString,commsPerson.getFaction().getBaseUIColor(),0f);
        personTitleTooltip.setParaFontDefault();
        personTitleTooltip.addPara(commsPerson.getFaction().getDisplayName(),commsPerson.getFaction().getBaseUIColor(),0f);
        mainPanel.addUIElement(personTitleTooltip).inTL(135f,0);


        TooltipMakerAPI textTooltip = mainPanel.createUIElement(textAreaWidth, contentMaxHeight-titleContentHeight, true);
        // Set text font
        try {
            Global.getSettings().loadFont("graphics/fonts/insignia17LTaa.fnt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        textTooltip.setParaFont("graphics/fonts/insignia17LTaa.fnt");

        // Set text and highlights
        if (eventDescription != null && eventDescription.hasText1() && !eventDescription.getText1().isEmpty()) {
            if (eventDescription.hasText2() && !eventDescription.getText2().isEmpty()) {
                String[] highlights = getDescHighlights(eventDescription);
                textTooltip.addPara(eventDescription.getText1(),5f, Misc.getHighlightColor(),highlights);
            }
            else {
                textTooltip.addPara(eventDescription.getText1(),5f);
            }
        }
        else {
            textTooltip.addPara("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",5f);
        }
        textTooltip.setParaFontDefault();
        mainPanel.addUIElement(textTooltip).inTL(135f,titleContentHeight);
        tooltip.addCustom(mainPanel,0f).getPosition().inTL(0,0);
    }

    /**
     * Get and parse description highlights from the Description object's "text2"
     * @param eventDescription Description object
     * @return String array with highlights
     */
    public String[] getDescHighlights(Description eventDescription) {

        String[] initHighlights = eventDescription.getText2().split(",");
        String[] finalHighlights = new String[initHighlights.length];

        for (int i = 0; i < initHighlights.length; i++) {
            String currString = initHighlights[i];
            // Check if current string starts with $ marking a memKey, compares to commsPerson PersonAPI object's memory
            if (initHighlights[i].charAt(0) == '$') {
                try {
                    finalHighlights[i] = parseHighlight(currString);
                } catch (Exception e) {
                    finalHighlights[i] = currString;
                }
            }
            else {
                finalHighlights[i] = currString;
            }
        }
        return finalHighlights;
    }

    public String parseHighlight(String highlightType) {
        switch (highlightType) {
            case "$Name","$name","$personName" -> {
                String nameString = commsPerson.getMemoryWithoutUpdate().getString(highlightType);
                if (nameString != null) return nameString;
                else return commsPerson.getNameString();
            }
            case "$Post","$post","$personPost" -> {
                String postString = commsPerson.getMemoryWithoutUpdate().getString(highlightType);
                if (postString != null) return postString;
                else return commsPerson.getPost();
            }
            case "$Rank","$rank","$personRank" -> {
                String rankString = commsPerson.getMemoryWithoutUpdate().getString(highlightType);
                if (rankString != null) return rankString;
                return commsPerson.getRank();
            }
            default -> {
                return highlightType;
            }
        }
    }

    @Override
    public void applyConfirmScript() {

    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
    }
}
