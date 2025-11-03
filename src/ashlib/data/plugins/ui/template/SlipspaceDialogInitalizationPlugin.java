package ashlib.data.plugins.ui.template;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import java.util.ArrayList;
import java.util.Map;

public class SlipspaceDialogInitalizationPlugin implements InteractionDialogPlugin {
    @Override
    public void init(InteractionDialogAPI dialog) {
        SlipSpaceEntityPicker picker = new SlipSpaceEntityPicker(dialog);
        ArrayList<SectorEntityToken>tokens = new ArrayList<>();
        for (StarSystemAPI starSystem : Global.getSector().getStarSystems()) {
            tokens.add(starSystem.getCenter());
        }
        dialog.showCampaignEntityPicker("Choose system for Slipspace Jump","Destination:","Start Procedure", Global.getSector().getPlayerFaction(),tokens,picker);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {

    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {

    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {

    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return Map.of();
    }
}
