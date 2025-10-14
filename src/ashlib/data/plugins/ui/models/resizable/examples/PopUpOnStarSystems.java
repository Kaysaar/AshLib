package ashlib.data.plugins.ui.models.resizable.examples;

import ashlib.data.plugins.ui.models.resizable.map.MapEntityComponent;
import ashlib.data.plugins.ui.models.resizable.map.MapMainComponent;
import com.fs.starfarer.api.campaign.StarSystemAPI;

public class PopUpOnStarSystems extends MapMainComponent {
    public PopUpOnStarSystems(float width, float height, StarSystemAPI systemAPI) {
        super(width, height,systemAPI);
    }

    @Override
    protected void afterAddPlanets() {
        for (MapEntityComponent comp : getMapZoom().getAllEntitiesComponents()) {
                comp.addHook(new BaseMouseClickHook(),"test");

        }
    }
}
