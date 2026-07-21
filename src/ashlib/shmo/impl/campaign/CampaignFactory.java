package ashlib.shmo.impl.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import ashlib.shmo.api.campaign.CampaignData;
import ashlib.shmo.api.campaign.StarSiphonEffect;
import ashlib.shmo.api.campaign.StarSiphonParams;
import ashlib.shmo.api.campaign.StarSiphonManager;

import java.util.EnumSet;

public class CampaignFactory {
    public CampaignData createCampaignData() {
        return new MapCampaignData();
    }

    public StarSiphonEffect createStarSiphonEffect(StarSiphonParams params) {
        return new StarSiphonEffectImpl(params);
    }

    public StarSiphonEffect createStarSiphonEffect() {
        return new StarSiphonEffectImpl();
    }

    public StarSiphonManager createStarSiphonManager() {
        ShmoEntityToken entityToken = new ShmoEntityToken("shmolib_star_siphon_manager");
        StarSiphonManagerImpl plugin = new StarSiphonManagerImpl();
        entityToken.setCustomPlugin(plugin, null);
        entityToken.setActiveLayers(EnumSet.of(CampaignEngineLayers.TERRAIN_1, CampaignEngineLayers.BELOW_STATIONS));
        Global.getSector().getHyperspace().addEntity(entityToken);
        return plugin;
    }
}
