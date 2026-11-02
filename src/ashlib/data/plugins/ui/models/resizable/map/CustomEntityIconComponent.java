package ashlib.data.plugins.ui.models.resizable.map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public class CustomEntityIconComponent extends StableLocationComponent {
    public static final String SHOW_ONLY_ICON_ON_MAP = "ashlib_show_only_icon_on_map";

    public CustomEntityIconComponent(SectorEntityToken token) {
        super(token);
        String iconName = token.getCustomEntitySpec().getIconName();
        if (iconName != null && !iconName.isEmpty()) {
            sprite = Global.getSettings().getSprite(iconName);
        }
    }
}
