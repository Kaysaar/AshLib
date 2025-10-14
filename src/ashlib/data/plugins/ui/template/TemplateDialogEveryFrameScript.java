package ashlib.data.plugins.ui.template;

import ashlib.data.plugins.ui.models.resizable.examples.PopUpOnStarSystems;
import ashlib.data.plugins.ui.models.resizable.map.MapMainComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class TemplateDialogEveryFrameScript implements CampaignInputListener {
    public static boolean init=false;

    @Override
    public int getListenerInputPriority() {
        return 1000;
    }

    @Override
    public void processCampaignInputPreCore(List<InputEventAPI> events) {
        if(init)return;
        events.stream().filter(x->!x.isConsumed()).forEach(x->{
            if(x.getEventValue()== Keyboard.KEY_T&&x.isKeyDownEvent()&& Global.getSettings().isDevMode()){
                x.consume();
                // specify your ExtendedUI

                new TemplateDialog("Test",new PopUpOnStarSystems(1000,600,Global.getSector().getPlayerFleet().getStarSystem()));
            }
        });
    }

    @Override
    public void processCampaignInputPreFleetControl(List<InputEventAPI> events) {

    }

    @Override
    public void processCampaignInputPostCore(List<InputEventAPI> events) {

    }
}
