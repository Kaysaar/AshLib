package ashlib.data.plugins.coreui;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;


public class CommandTabInterceptor implements CoreUITabListener, PlayerColonizationListener {
    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId tab, Object param) {
        if (param instanceof String) {
            String s = (String) param;
            if (s.equals("income_report")) {
              CommandTabMemoryManager.getInstance().setLastCheckedTab("income");
            }
        }
        if(param instanceof MarketAPI){
            CommandTabMemoryManager.getInstance().setLastCheckedTab("colonies");
        }
        if(param instanceof CommandCustomData data){
            CommandTabMemoryManager.getInstance().setLastCheckedTab(data.getCommandId());
            if(data.getSubTabId()!=null){
                CommandTabMemoryManager.getInstance().getTabStates().put(data.getCommandId(),data.getSubTabId());
            }
        }
        CommandTabTracker.sendSignalToOpenCore = true;
    }

    @Override
    public void reportPlayerColonizedPlanet(PlanetAPI planet) {

    }

    @Override
    public void reportPlayerAbandonedColony(MarketAPI colony) {

    }
}
