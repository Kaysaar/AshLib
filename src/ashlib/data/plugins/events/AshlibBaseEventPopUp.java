package ashlib.data.plugins.events;

import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.ui.models.BasePopUpDialog;
import ashlib.data.plugins.ui.models.ButtonWithCroppedImageComponent;
import ashlib.data.plugins.ui.models.ButtonWithImageComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class AshlibBaseEventPopUp extends BasePopUpDialog {

    AshlibBaseEvent event;
    boolean wasPaused = false;
    float heightV = 0;
    public AshlibBaseEventPopUp(AshlibBaseEvent event) {
        super(event.getEventName());
        wasPaused = Global.getSector().isPaused();
        cancelButtonText = "Exit";
        this.event = event;
        setConfirmText(event.getConfirmButtonText());
    }
    public void show() {
        AshMisc.initPopUpDialog(this, 512, 410);
        Global.getSector().setPaused(true);
    }

    @Override
    public void createUI(CustomPanelAPI panelAPI) {
        createHeaader(panelAPI);

        TooltipMakerAPI tooltip = panelAPI.createUIElement(panelAPI.getPosition().getWidth()-15,panelAPI.getPosition().getHeight()-y,false);
        heightV = panelAPI.getPosition().getHeight()-y;
        createContentForDialog(tooltip,panelAPI.getPosition().getWidth()-15);
        addTooltip(tooltip);
        panelAPI.addUIElement(tooltip).inTL(x-3,y);
        createConfirmAndCancelSection(panelAPI);
    }
    @Override
    public void createContentForDialog(TooltipMakerAPI tooltip, float width) {
        if (event.getImageName() != null) {
            ButtonWithCroppedImageComponent component =
                    new ButtonWithCroppedImageComponent(
                            event.getImageDisplayWidth(),
                            event.getImageDisplayHeight(),
                            event.getImageName(),
                            event.getImageSourceWidth(),
                            event.getImageSourceHeight(),
                            event.getImageCropX(),
                            event.getImageCropY(),
                            event.getImageCropWidth(),
                            event.getImageCropHeight()
                    );
            component.setClickable(false);
            CustomPanelAPI container = Global.getSettings().createCustom(width,150,null);
            container.addComponent(component.getComponentPanel()).inTL(-2,0);
            tooltip.addCustom(
                    container,
                    5f
            );
        }
        CustomPanelAPI container2 = Global.getSettings().createCustom(width,heightV-210,null);
        TooltipMakerAPI tl = container2.createUIElement(container2.getPosition().getWidth(),container2.getPosition().getHeight(),true);
        event.createTooltipForEvent(tl);
        container2.addUIElement(tl).inTL(-5,0);
        tooltip.addCustom(container2,5f);

    }

    @Override
    public void createConfirmSection(CustomPanelAPI mainPanel) {
        super.createConfirmSection(mainPanel);

        if (event.doesHaveOnHoverTooltip()) {
            mainPanel.createUIElement(1, 1, false).addTooltipTo(
                    new TooltipMakerAPI.TooltipCreator() {
                        @Override
                        public boolean isTooltipExpandable(Object tooltipParam) {
                            return false;
                        }

                        @Override
                        public float getTooltipWidth(Object tooltipParam) {
                            return 500f;
                        }

                        @Override
                        public void createTooltip(
                                TooltipMakerAPI tooltip,
                                boolean expanded,
                                Object tooltipParam
                        ) {
                            event.createOnHoverForConfirmButton(
                                    cancelButton,
                                    tooltip
                            );
                        }
                    },
                    cancelButton,
                    TooltipMakerAPI.TooltipLocation.BELOW,
                    false
            );
        }
    }

    @Override
    public ButtonAPI generateConfirmButton(TooltipMakerAPI tooltip) {
        return null;
    }

    @Override
    public void onExit() {
        AshlibBaseEventManager.getInstance().confirmEvent(event);
        Global.getSector().setPaused(wasPaused);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI inputEvent : events) {
            if (
                    inputEvent.isKeyDownEvent()
                            && !inputEvent.isMouseEvent()
                            && inputEvent.getEventValue() == Keyboard.KEY_ESCAPE
            ) {
                inputEvent.consume();
            }
        }

        super.processInput(events);
    }
}