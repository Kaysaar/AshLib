package ashlib.data.plugins.ui.models.resizable.map;

import ashlib.data.plugins.ui.models.resizable.ResizableComponent;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public class MapEntityComponent extends ResizableComponent {
    SectorEntityToken token;
    public MapEntityComponent(SectorEntityToken token) {
        this.token = token;
    }
    public SectorEntityToken getToken() {
        return token;
    }
}
