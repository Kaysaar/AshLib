package ashlib.data.plugins.ui.template;

import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static ashlib.data.plugins.ui.models.PopUpUI.buttonConfirmWidth;

public class SlipSpaceDialog implements CustomUIPanelPlugin {
    SlipSpaceJumpCoordinateUI map;
    CustomPanelAPI mainPanel;
    CustomPanelAPI panelOfInfoMain;
    CustomPanelAPI contentForInfo;
    StarSystemAPI location;
    InteractionDialogAPI dialog;
    CustomVisualDialogDelegate.DialogCallbacks callbacks;
    ButtonAPI confirm, cancel;
    TooltipMakerAPI headerTooltip;

    public void init(CustomPanelAPI panel, CustomVisualDialogDelegate.DialogCallbacks callbacks, InteractionDialogAPI dialog, StarSystemAPI location) {
        this.mainPanel = panel;
        this.callbacks = callbacks;
        this.dialog = dialog;
        this.location = location;
        headerTooltip = panel.createUIElement(panel.getPosition().getWidth(), panel.getPosition().getHeight(), false);
        headerTooltip = panel.createUIElement(panel.getPosition().getWidth() - 30, 20, false);
        headerTooltip.setParaFont(Fonts.ORBITRON_20AABOLD);
        LabelAPI label = headerTooltip.addPara("Choose Coordinates for Jump", Misc.getTooltipTitleAndLightHighlightColor(), 5f);
        panel.addUIElement(headerTooltip).inTL(15, 10);
        float width = label.computeTextWidth(label.getText());
        label.getPosition().setLocation(0, 0).inTL((panel.getPosition().getWidth() / 2) - (width / 2), 3);
        TooltipMakerAPI content = panel.createUIElement(panel.getPosition().getWidth() - 30, panel.getPosition().getHeight() - 45, false);
        createContent(content, panel.getPosition().getWidth() - 30);
        panel.addUIElement(content).inTL(15, 45);
        createConfirmAndCancelSection(panel);

    }

    public ButtonAPI generateConfirmButton(TooltipMakerAPI tooltip) {
        ButtonAPI button = tooltip.addButton("Confirm", "confirm", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.TL_BR, 160, 25, 0f);
        button.setShortcut(Keyboard.KEY_G, true);
        confirm = button;
        return button;
    }

    public ButtonAPI generateCancelButton(TooltipMakerAPI tooltip) {
        ButtonAPI button = tooltip.addButton("Cancel", "cancel", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.TL_BR, buttonConfirmWidth, 25, 0f);
        button.setShortcut(Keyboard.KEY_ESCAPE, true);
        cancel = button;
        return button;
    }

    public void createConfirmAndCancelSection(CustomPanelAPI mainPanel) {
        float totalWidth = buttonConfirmWidth * 2 + 10;
        TooltipMakerAPI tooltip = mainPanel.createUIElement(totalWidth, 25, false);
        tooltip.setButtonFontOrbitron20();
        generateConfirmButton(tooltip);
        generateCancelButton(tooltip);
        confirm.getPosition().inTL(0, 0);
        cancel.getPosition().inTL(buttonConfirmWidth + 5, 0);
        float bottom = mainPanel.getPosition().getHeight();
        mainPanel.addUIElement(tooltip).inTL(mainPanel.getPosition().getWidth() - (totalWidth) - 10, bottom - 40);
    }

    public void createContent(TooltipMakerAPI tooltip, float width) {
        map = new SlipSpaceJumpCoordinateUI(width - 10, 500, location);
        panelOfInfoMain = Global.getSettings().createCustom(width - 10, 100, null);
        contentForInfo = Global.getSettings().createCustom(width - 10, 100, null);
        panelOfInfoMain.addComponent(contentForInfo).inTL(0, 0);
        tooltip.addCustom(map.getMainPanel(), 5f);
        tooltip.addCustom(panelOfInfoMain, 10f);
        tooltip.setHeightSoFar(0f);
    }

    public void updateInfo() {
        panelOfInfoMain.removeComponent(contentForInfo);
        contentForInfo = Global.getSettings().createCustom(panelOfInfoMain.getPosition().getWidth(), panelOfInfoMain.getPosition().getHeight(), null);
        TooltipMakerAPI tooltip = contentForInfo.createUIElement(contentForInfo.getPosition().getWidth(), contentForInfo.getPosition().getHeight(), false);
        String font = "graphics/fonts/insignia21LTaa.fnt";
        tooltip.setParaFont(font);
        Vector2f vector2f = map.getCurrentCoordinates();
        int xL = (int) vector2f.x;
        int yL = (int) vector2f.y;
        tooltip.addPara("Saved Coordinates : %s , %s", 0f, Color.ORANGE, Integer.toString(xL), Integer.toString(yL));
        contentForInfo.addUIElement(tooltip);
        panelOfInfoMain.addComponent(contentForInfo);

    }

    @Override
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void renderBelow(float alphaMult) {
        if(headerTooltip != null) {
            AshMisc.drawRectangleFilledForTooltip(headerTooltip,1f,  Global.getSector().getPlayerFaction().getDarkUIColor().darker());
        }
    }

    @Override
    public void render(float alphaMult) {

    }

    @Override
    public void advance(float amount) {
        if (map != null && map.needsToUpdateUI) {
            map.needsToUpdateUI = false;
            updateInfo();
        }
        if (confirm != null && cancel != null && map != null) {
            confirm.setEnabled(map.getCurrentCoordinates() != null);
            if (confirm.isChecked()) {
                map.clearUI();
                confirm.setChecked(false);
                dialog.dismiss();
                JumpPointAPI.JumpDestination destination = new JumpPointAPI.JumpDestination(location.createToken(map.getCurrentCoordinates()), null);
                Global.getSector().doHyperspaceTransition(Global.getSector().getPlayerFleet(), Global.getSector().getPlayerFleet(), destination);
            }
            if (cancel.isChecked()) {
                cancel.setChecked(false);
                callbacks.dismissDialog();
                SlipSpaceEntityPicker picker = new SlipSpaceEntityPicker(dialog);
                ArrayList<SectorEntityToken> tokens = new ArrayList<>();
                for (StarSystemAPI starSystem : Global.getSector().getStarSystems()) {
                    tokens.add(starSystem.getCenter());
                }
                map.clearUI();
                dialog.showCampaignEntityPicker("Choose system for Slipspace Jump", "Destination:", "Start Procedure", Global.getSector().getPlayerFaction(), tokens, picker);


            }
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }
}
