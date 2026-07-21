package ashlib.data.plugins;

import ashlib.data.plugins.coreui.CommandTabInterceptor;
import ashlib.data.plugins.coreui.CommandTabTracker;
import ashlib.data.plugins.handlers.AICoreSkillPollHandler;
import ashlib.data.plugins.ui.CustomCampaignFleetViewer;
import ashlib.data.scripts.AiCoreLevelUpHijacker;
import ashlib.data.scripts.AshReplaceAISkills;;
import ashlib.shmo.api.general.Option;
import ashlib.shmo.example.ExampleCustomEntities;
import com.fs.starfarer.api.BaseModPlugin;
import ashlib.data.plugins.repositories.ShipRenderInfoRepo;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.CampaignFleetView;

import java.awt.*;

public class AshLibPlugin extends BaseModPlugin {
    public static String fontInsigniaMedium = "graphics/fonts/insignia17LTaa.fnt";
    private static final String EXAMPLE_STAR_SIPHON_INSTANCE_ID = "shmolib_example_star_siphon_instance";

    @Override
    public void onApplicationLoad() throws Exception {


        ShipRenderInfoRepo.populateRenderInfoRepo();
        AICoreSkillPollHandler.getInstance();


    }

    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);
        Global.getSector().addTransientScript(new AshReplaceAISkills());
        Global.getSector().addTransientScript(new AiCoreLevelUpHijacker());
        Global.getSector().addTransientScript(new CommandTabTracker());
        Global.getSector().getListenerManager().addListener(new CommandTabInterceptor(),true);
        CustomCampaignFleetViewer.replacePlayerFleetView();
        Option.of(Global.getSector().getEntityById(EXAMPLE_STAR_SIPHON_INSTANCE_ID)).match(
                (e) -> {},
                this::initializeExampleStarSiphonInstance
        );
//        Global.getSector().getListenerManager().addListener(new SlipSpaceUIListener(),true);
    }
    private void initializeExampleStarSiphonInstance() {
        ExampleCustomEntities.createExampleStarSiphon(
                EXAMPLE_STAR_SIPHON_INSTANCE_ID,
                Global.getSector().getHyperspace()
        );
    }
}
