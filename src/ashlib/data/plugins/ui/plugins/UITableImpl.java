package ashlib.data.plugins.ui.plugins;

import ashlib.data.plugins.ui.models.CustomButton;
import ashlib.data.plugins.ui.models.DropDownButton;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.ArrayList;
import java.util.List;

public class UITableImpl implements CustomUIPanelPlugin {
    public ArrayList<DropDownButton> dropDownButtons;
    public  TooltipMakerAPI tooltipOfImpl;
    public CustomPanelAPI panelToWorkWith;
    public CustomPanelAPI mainPanel;
    public TooltipMakerAPI tooltipOfButtons;
    public  float width,height,xCord,yCord;
    public boolean doesHaveScroller;
    public UITableImpl(float width, float height, CustomPanelAPI panelToPlace, boolean doesHaveScroller, float xCord, float yCord) {
        this.width = width;
        this.height = height;
        this.doesHaveScroller = doesHaveScroller;
        this.xCord = xCord;
        this.yCord = yCord;
        dropDownButtons = new ArrayList<>();
        mainPanel = panelToPlace;
        panelToWorkWith = mainPanel.createCustomPanel(width,height,null);

        tooltipOfImpl = panelToWorkWith.createUIElement(width, height-22, doesHaveScroller);
        tooltipOfButtons = mainPanel.createUIElement(width, 22, false);
    }

    @Override
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void renderBelow(float alphaMult) {

    }

    @Override
    public void render(float alphaMult) {

    }
    public void createSections(){

    }
    @Override
    public void advance(float amount) {
        for (DropDownButton dropDownButton : dropDownButtons) {
            dropDownButton.advance(amount);
        }

    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }

    public void createTable() {
        panelToWorkWith = mainPanel.createCustomPanel(width,height,null);
        tooltipOfImpl = panelToWorkWith.createUIElement(width,height,doesHaveScroller);

    }

    public void recreateTable() {
        clearTable();
        createTable();
    }

    public void clearTable() {
        panelToWorkWith.removeComponent(tooltipOfImpl);
        mainPanel.removeComponent(panelToWorkWith);

    }

    public void reportButtonPressed(CustomButton buttonPressed) {

    }
}
