package ashlib.shmo.aotd.lib.api;

import ashlib.shmo.aotd.lib.api.campaign.CampaignData;
import ashlib.shmo.aotd.lib.api.campaign.StarSiphonManager;
import ashlib.shmo.aotd.lib.impl.campaign.MapCampaignData;
import com.fs.starfarer.api.Global;
import ashlib.shmo.aotd.lib.api.general.Utilities;
import ashlib.shmo.aotd.lib.impl.campaign.CampaignFactory;
import ashlib.shmo.aotd.lib.impl.general.GeneralFactory;

import org.apache.log4j.Logger;

public final class ShmoGlobal {
    private static final String GLOBAL_PERSISTENT_DATA_KEY = "shmo.lib.ShmoGlobal";

    private static final String LOGGER_KEY = "logger";
    private static final String CAMPAIGN_FACTORY_KEY = "campaignFactory";
    private static final String GENERAL_FACTORY_KEY = "generalFactory";
    private static final String STAR_SIPHON_MANAGER_KEY = "starSiphonManager";

    private static final String LOGGER_NAME = "ShmoLib";

    private final CampaignData data = new MapCampaignData();

    private ShmoGlobal() {}

    private static ShmoGlobal getSingleton() {
        return (ShmoGlobal) Utilities.getOrInsert(Global.getSector().getPersistentData(), GLOBAL_PERSISTENT_DATA_KEY, ShmoGlobal::new);
    }

    private static CampaignData getData() {
        return getSingleton().data;
    }

    public static Logger getLogger() {
        return getData().getOrConstruct(LOGGER_KEY, () -> Logger.getLogger(LOGGER_NAME));
    }

    public static CampaignFactory getCampaignFactory() {
        return getData().getOrConstruct(CAMPAIGN_FACTORY_KEY, CampaignFactory::new);
    }

    public static GeneralFactory getGeneralFactory() {
        return getData().getOrConstruct(GENERAL_FACTORY_KEY, GeneralFactory::new);
    }

    public static StarSiphonManager getStarSiphonManager() {
        return getData().getOrConstruct(STAR_SIPHON_MANAGER_KEY, () -> getCampaignFactory().createStarSiphonManager());
    }
}
