package ashlib.data.scripts;

import java.util.LinkedHashMap;

public class CustomCampaignViewerListenerManager {

    public static LinkedHashMap<String,CustomCampaignViewerListener>listeners = new LinkedHashMap<>();

    /// NOTE - Only use addListener in onApplicationLoad !!!
    public static void addListener(String id,CustomCampaignViewerListener listener){
        listeners.put(id,listener);
    }
}
