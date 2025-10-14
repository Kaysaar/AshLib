package ashlib.data.plugins.ui.template;

import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.ui.models.BasePopUpDialog;
import ashlib.data.plugins.ui.models.ExtendedUIPanelPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class TemplateDialog extends BasePopUpDialog {
    ExtendedUIPanelPlugin testPlugin;
    boolean wasPaused;
   boolean wasRenderingEveryting;
   LocationAPI prevLocation;

    public TemplateDialog(String headerTitle, ExtendedUIPanelPlugin testPlugin) {
        super(headerTitle);
        this.testPlugin = testPlugin;
         wasPaused = Global.getSector().isPaused();
         wasRenderingEveryting =  Global.getSector().getViewport().isEverythingNearViewport();
        Global.getSector().setPaused(true);
        AshMisc.initPopUpDialog(this,testPlugin.getMainPanel().getPosition().getWidth()+100,testPlugin.getMainPanel().getPosition().getHeight()+100);
        TemplateDialogEveryFrameScript.init = true;
    }

    @Override
    public void createContentForDialog(TooltipMakerAPI tooltip, float width) {
        tooltip.addCustom(testPlugin.getMainPanel(),10f).getPosition().inTL(45,5);

    }


    @Override
    public void onExit() {
        super.onExit();
        Global.getSector().getViewport().setEverythingNearViewport(wasRenderingEveryting);
        Global.getSector().setPaused(wasPaused);
        TemplateDialogEveryFrameScript.init = false;
    }
}
