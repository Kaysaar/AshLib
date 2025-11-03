package ashlib.data.plugins.coreui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;

import java.util.LinkedHashMap;

public class CommandTabMemoryManager {
    public LinkedHashMap<String,String>tabStates = new LinkedHashMap<>();
    public static CommandTabMemoryManager getInstance(){
        if(!Global.getSector().getPersistentData().containsKey("$ashlib_command_tab_manager"))setInstance();
        return (CommandTabMemoryManager) Global.getSector().getPersistentData().get("$ashlib_command_tab_manager");
    }

    public String lastCheckedTab = null;

    public void setLastCheckedTab(String lastCheckedTab) {
        this.lastCheckedTab = lastCheckedTab;
    }

    public String getLastCheckedTab() {
        return lastCheckedTab;
    }

    public static boolean sendSignalToOpenCore = false;
    public LinkedHashMap<String, String> getTabStates() {
        return tabStates;
    }

    public void updateTabState(String tabId,String stateID){
        tabStates.put(tabId,stateID);
    }

    public static void setInstance(){
        CommandTabMemoryManager instance = new CommandTabMemoryManager();
        Global.getSector().getPersistentData().put("$ashlib_command_tab_manager", instance);
    }
    public static void generatedCoreUI(){

    }
}
