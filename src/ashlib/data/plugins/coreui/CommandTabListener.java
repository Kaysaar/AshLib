package ashlib.data.plugins.coreui;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.LinkedHashMap;

public interface CommandTabListener {
    public String getNameForTab();
    public String getButtonToReplace();
    public String getButtonToBePlacedNear();
    public TooltipMakerAPI.TooltipCreator getTooltipCreatorForButton();
    public CommandUIPlugin createPlugin();
    public float getWidthOfButton();
    public int getKeyBind();
    public void performRecalculations(UIComponentAPI mainPanel);
    public int getOrder();
    public boolean shouldButtonBeEnabled();
    public void performRefresh(ButtonAPI currentTab);
}
