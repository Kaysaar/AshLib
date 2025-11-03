package ashlib.data.plugins.ui.template;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class SlipSpaceUIListener implements CampaignInputListener {
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
                Global.getSector().getCampaignUI().showInteractionDialog(new SlipspaceDialogInitalizationPlugin(),null);

//                new SlipSpaceDialog("Test",new SlipSpaceJumpCoordinateUI(1000,600,Global.getSector().getPlayerFleet().getStarSystem()));
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
