package ashlib.data.plugins.coreui;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;


public class CommandTabInterceptor implements CoreUITabListener, PlayerColonizationListener {
    /*
    We need this because if the player has the colony management screen open, the "param" argument gets overridden
    by the MarketAPI object even if you try to set the argument yourself, like the below snippet:

    CommandCustomData commandCustomData = new CommandCustomData("terraforming", null);
    Global.getSector().getCampaignUI().showCoreUITab(CoreUITabId.OUTPOSTS, commandCustomData);

     With the param argument getting overridden with the MarketAPI object, we must use some other mechanism to track
     which tab to open, so I'm using this static variable here. To use this, the following snippet works:

     import ashlib.data.plugins.coreui.CommandTabInterceptor;
     CommandTabInterceptor.tabIdToSwitchToUponOpen = "terraforming";
     */
    public static String tabIdToSwitchToUponOpen = null;

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId tab, Object param) {
        if (param instanceof String) {
            String s = (String) param;
            if (s.equals("income_report")) {
              CommandTabMemoryManager.getInstance().setLastCheckedTab("income");
            }
        }
        if(tabIdToSwitchToUponOpen != null)
        {
            CommandTabMemoryManager.getInstance().setLastCheckedTab(tabIdToSwitchToUponOpen);
            tabIdToSwitchToUponOpen = null;
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
        if(tabIdToSwitchToUponOpen != null)
        {
            CommandTabMemoryManager.getInstance().setLastCheckedTab(tabIdToSwitchToUponOpen);
            tabIdToSwitchToUponOpen = null;
        }
        CommandTabTracker.sendSignalToOpenCore = true;
    }
    public static String tabIdToSwitchToUponOpen = null;
    @Override
    public void reportPlayerColonizedPlanet(PlanetAPI planet) {

    }

    @Override
    public void reportPlayerAbandonedColony(MarketAPI colony) {

    }
}
