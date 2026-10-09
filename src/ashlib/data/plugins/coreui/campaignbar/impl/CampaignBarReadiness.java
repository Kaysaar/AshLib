package ashlib.data.plugins.coreui.campaignbar.impl;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.List;
import java.util.Map;

/** Checks construction readiness; the injector validates compatibility afterward. */
public final class CampaignBarReadiness {
    private CampaignBarReadiness() {}

    public static String inspect(UIPanelAPI core) throws Exception {
        return inspect(core, false);
    }

    public static String inspect(UIPanelAPI core, boolean fixedNavigation) throws Exception {
        if (core == null) {
            return "WAIT_CORE";
        }
        if (!sized(core)) {
            return "WAIT_CORE_GEOMETRY";
        }
        Object value = CampaignUiAccess.read(core, "getButtons");
        if (!(value instanceof UIPanelAPI)) {
            return "WAIT_BAR";
        }
        UIPanelAPI bar = (UIPanelAPI) value;
        if (!CampaignUiAccess.children(core).contains(bar)) {
            return "WAIT_BAR_ATTACHMENT";
        }
        if (!sized(bar)) {
            return "WAIT_BAR_GEOMETRY";
        }
        Object raw = CampaignUiAccess.read(bar, "getButtons");
        if (!(raw instanceof Map)) {
            return "WAIT_REGISTRY";
        }
        Map<?, ?> map = (Map<?, ?>) raw;
        if (map.size() > 7) {
            return "READY_FOR_PREFLIGHT";
        } // Preflight checks unexpected entries.
        if (map.size() < 7) {
            return "WAIT_NATIVE_BUTTONS";
        }
        List<?> children = CampaignUiAccess.children(bar);
        for (CoreUITabId id :
                new CoreUITabId[] {
                    CoreUITabId.CHARACTER,
                    CoreUITabId.FLEET,
                    CoreUITabId.REFIT,
                    CoreUITabId.CARGO,
                    CoreUITabId.MAP,
                    CoreUITabId.INTEL,
                    CoreUITabId.OUTPOSTS
                }) {
            Object button = map.get(id);
            if (!(button instanceof ButtonAPI)) {
                return "READY_FOR_PREFLIGHT";
            } // Preflight checks incompatible entries.
            if (!children.contains(button) || !sized((ButtonAPI) button)) {
                return "WAIT_BUTTON_LAYOUT";
            }
            if (!fixedNavigation && !((ButtonAPI) button).isEnabled()) {
                return "WAIT_NATIVE_TABS_ENABLED";
            }
        }
        if (children.size() < 8) {
            return "WAIT_FRAME_ATTACHMENT";
        }
        return "READY_FOR_PREFLIGHT";
    }

    private static boolean sized(UIComponentAPI component) {
        PositionAPI position = component.getPosition();
        return position != null
                && finite(position.getX())
                && finite(position.getY())
                && finite(position.getWidth())
                && finite(position.getHeight())
                && position.getWidth() > 0
                && position.getHeight() > 0;
    }

    private static boolean finite(float n) {
        return !Float.isNaN(n) && !Float.isInfinite(n);
    }
}
