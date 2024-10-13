package ashlib.data.plugins.ui.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DarkHighlightPlugin implements CustomUIPanelPlugin {
    HashMap<String, ButtonAPI>skillMap = new HashMap<>();
    SpriteAPI box = Global.getSettings().getSprite("rendering","GlitchSquare");
    HashMap<String,Float> affectedPerson;
    CustomPanelAPI panelTied;

    public void setPanelTied(CustomPanelAPI panelTied) {
        this.panelTied = panelTied;
    }

    public CustomPanelAPI getPanelTied() {
        return panelTied;
    }

    float alpha = 0.4f;
    boolean needsIncrease = false;
    @Override
    public void positionChanged(PositionAPI position) {

    }
    public DarkHighlightPlugin(HashMap<String,ButtonAPI>skillMap, float alpha, HashMap<String,Float> rawMap) {
        this.skillMap = skillMap;
        this.alpha = alpha;
        this.affectedPerson = rawMap;
    }
    @Override
    public void renderBelow(float alphaMult) {

    }

    @Override
    public void render(float alphaMult) {

        if(skillMap != null&&affectedPerson!=null) {
            for (Map.Entry<String, ButtonAPI> entry : skillMap.entrySet()) {
                if(affectedPerson.get(entry.getKey())!=null&&affectedPerson.get(entry.getKey()) ==0) {
                    box.setSize(entry.getValue().getPosition().getWidth(),entry.getValue().getPosition().getHeight());
                    box.setColor(Color.BLACK);
                    box.setAlphaMult(alpha);
                    box.renderAtCenter(entry.getValue().getPosition().getCenterX(),entry.getValue().getPosition().getCenterY());
                }

            }

        }
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
