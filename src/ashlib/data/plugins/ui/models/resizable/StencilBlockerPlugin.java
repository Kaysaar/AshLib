package ashlib.data.plugins.ui.models.resizable;

import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.List;

public class StencilBlockerPlugin implements CustomUIPanelPlugin {
    CustomPanelAPI panelToStencil;
    public StencilBlockerPlugin(CustomPanelAPI panelToStencil) {
        this.panelToStencil = panelToStencil;
    }
    @Override
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void renderBelow(float alphaMult) {

    }

    @Override
    public void render(float alphaMult) {
        AshMisc.startStencil(panelToStencil,1f);
    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }
}
