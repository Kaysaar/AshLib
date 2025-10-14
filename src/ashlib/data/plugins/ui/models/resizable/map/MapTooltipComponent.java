package ashlib.data.plugins.ui.models.resizable.map;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

public abstract class MapTooltipComponent implements TooltipMakerAPI.TooltipCreator {
    @Override
    public boolean isTooltipExpandable(Object tooltipParam) {
        return false;
    }

    @Override
    public float getTooltipWidth(Object tooltipParam) {
        return 400;
    }

}
