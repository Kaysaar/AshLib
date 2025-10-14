package ashlib.data.plugins.ui.models.resizable.examples;

import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.ui.models.PopUpUI;
import ashlib.data.plugins.ui.models.resizable.map.MapEntityComponent;
import ashlib.data.plugins.ui.models.resizable.map.MapEntityOnClickHook;
import ashlib.data.plugins.ui.models.resizable.map.MapMainComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

public class BaseMouseClickHook implements MapEntityOnClickHook {

    @Override
    public void onClick(MapEntityComponent token, InputEventAPI event, CustomPanelAPI anchor, MapMainComponent component) {
        if(anchor.getPosition().containsEvent(event)){
            Global.getSoundPlayer().playUISound("ui_button_pressed", 1, 1);
            component.getPointer().setShouldRender(false);
            AshMisc.placePopUpUI(new PopUpUI(){
                @Override
                public float createUIMockup(CustomPanelAPI panelAPI) {

                    return 300f;
                }

                @Override
                public void onExit() {
                    super.onExit();
                    component.getPointer().setShouldRender(true);
                }
            },anchor,300,600);
            event.consume();
        }

    }

    @Override
    public boolean shouldTrigger(InputEventAPI event) {
        return event.isLMBDownEvent();
    }
}
