package ashlib.data.plugins.ui.models.resizable.map;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;

public interface MapEntityOnClickHook {
    public void onClick(MapEntityComponent token, InputEventAPI event, CustomPanelAPI anchor,MapMainComponent component);
    public boolean shouldTrigger(InputEventAPI event);

}
