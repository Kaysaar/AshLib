package ashlib.data.plugins;

import ashlib.data.plugins.handlers.AICoreSkillPollHandler;
import ashlib.data.scripts.AshReplaceAISkills;;
import com.fs.starfarer.api.BaseModPlugin;
import ashlib.data.plugins.repositories.ShipRenderInfoRepo;
import com.fs.starfarer.api.Global;

public class AshLibPlugin extends BaseModPlugin {
    public static String fontInsigniaMedium = "graphics/fonts/insignia17LTaa.fnt";
    @Override
    public void onApplicationLoad() throws Exception {


        ShipRenderInfoRepo.populateRenderInfoRepo();
        AICoreSkillPollHandler.getInstance();


    }

    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);
        Global.getSector().addTransientScript(new AshReplaceAISkills());

    }
}
