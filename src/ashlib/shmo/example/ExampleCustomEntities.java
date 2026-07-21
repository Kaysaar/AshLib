package ashlib.shmo.example;

import com.fs.starfarer.api.campaign.LocationAPI;
import ashlib.shmo.example.campaign.ExampleStarSiphonPlugin;
import ashlib.shmo.impl.campaign.ShmoEntityToken;

public class ExampleCustomEntities {
    public static void createExampleStarSiphon(String id, LocationAPI location) {
        ShmoEntityToken customEntity = new ShmoEntityToken(id);
        customEntity.setCustomPlugin(new ExampleStarSiphonPlugin(), null);
        location.addEntity(customEntity);
    }
}
