package ashlib.data.plugins.ui.models;

import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;

public class BasePopUpDialog extends PopUpUI {
    TooltipMakerAPI headerTooltip;
    String title;
    public float x,y;
    public BasePopUpDialog(String headerTitle) {
        this.title = headerTitle;
        x=15;
        y=45;
    }

    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);
        if(headerTooltip != null) {
            AshMisc.drawRectangleFilledForTooltip(headerTooltip,fader.getBrightness(),Global.getSector().getPlayerFaction().getDarkUIColor().darker());
        }
    }

    @Override
    public void createUI(CustomPanelAPI panelAPI) {
        createHeaader(panelAPI);

        TooltipMakerAPI tooltip = panelAPI.createUIElement(panelAPI.getPosition().getWidth()-30,panelAPI.getPosition().getHeight()-y,true);
        createContentForDialog(tooltip,panelAPI.getPosition().getWidth()-30);
        addTooltip(tooltip);
        panelAPI.addUIElement(tooltip).inTL(x,y);
        createConfirmAndCancelSection(panelAPI);
    }

    public void createHeaader(CustomPanelAPI panelAPI) {
        if(AshMisc.isStringValid(title)) {
            headerTooltip = panelAPI.createUIElement(panelAPI.getPosition().getWidth()-30,20,false);
            headerTooltip.setParaFont(Fonts.ORBITRON_20AABOLD);
            LabelAPI label = headerTooltip.addPara(title,Misc.getBasePlayerColor(),5f);
            panelAPI.addUIElement(headerTooltip).inTL(15,10);
            float width = label.computeTextWidth(label.getText());
            label.getPosition().setLocation(0,0).inTL(0,3);
            label.setAlignment(Alignment.MID);
        }
        else {
            y = 10;
        }
    }

    public void createContentForDialog(TooltipMakerAPI tooltip,float width){
    }

    @Override
    public void applyConfirmScript() {

    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
    }

    // VoK GatheringPointDialog usage in NidavelirMainPanelPlugin
    public static void popUpDialog(BasePopUpDialog dialog, float width, float height){
        CustomPanelAPI panelAPI = Global.getSettings().createCustom(width, height, dialog);
        UIPanelAPI panelAPI1 = PopUpUI.ProductionUtil.getCoreUI();
        dialog.init(panelAPI, panelAPI1.getPosition().getCenterX() - (panelAPI.getPosition().getWidth() / 2), panelAPI1.getPosition().getCenterY() - (panelAPI.getPosition().getHeight() / 2), true);
    }
}
