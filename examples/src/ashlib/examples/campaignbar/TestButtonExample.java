package ashlib.examples.campaignbar;

import ashlib.data.plugins.coreui.campaignbar.api.CampaignBar;
import ashlib.data.plugins.coreui.campaignbar.api.TabSpec;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

/** Optional consumer example; disabled by default and independent of the library core. */
public final class TestButtonExample {
    public static final boolean TEST_BUTTON_DISABLED = true;
    private static TabSpec registration;

    private TestButtonExample() {}

    /** Call from onApplicationLoad; set TEST_BUTTON_DISABLED to false and rebuild to enable. */
    public static void registerIfEnabled() {
        if (TEST_BUTTON_DISABLED) {
            return;
        }

        if (registration == null) {
            registration = new TabSpec(
                    "test:generic", /* Unique tab ID: modid:tabid. */
                    "Test", /* Button title. */
                    CoreUITabId.OUTPOSTS, /* Insert after Command; change this to reposition. */
                    0, /* LWJGL Keyboard key code; 0 means no hotkey. */
                    -1, /* Highlighted letter index (zero-based); -1 means no hotkey. */
                    new TabSpec.ContentFactory() { /* Builds this tab's content. */
                        // width/height: available UI size; close.run(): request closing the tab.
                        public UIPanelAPI create(float width, float height, Runnable close) {
                            CustomPanelAPI panel =
                                    Global.getSettings().createCustom(width, height, null);
                            TooltipMakerAPI text =
                                    panel.createUIElement(width - 40, height - 40, false);
                            text.setParaFont("graphics/fonts/insignia21LTaa.fnt");
                            text.addPara("TEST - ASHLIB CAMPAIGN BAR", 0);
                            panel.addUIElement(text).inTL(20, 20);
                            return panel;
                        }
                    });
        }

        CampaignBar.register(registration);
    }
}
