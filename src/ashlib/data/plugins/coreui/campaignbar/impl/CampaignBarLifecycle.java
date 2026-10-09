package ashlib.data.plugins.coreui.campaignbar.impl;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.lang.ref.WeakReference;
import java.util.Map;

/** Per-bar bounded retry state. No strong reference to a Core, bar, session or callback. */
final class CampaignBarLifecycle {
    final WeakReference<UIPanelAPI> bar;
    String readiness, reason;
    private long signature = Long.MIN_VALUE, next;
    private int attempts;
    private boolean incompatible;
    int index;

    CampaignBarLifecycle(UIPanelAPI bar, int index) {
        this.bar = new WeakReference<UIPanelAPI>(bar);
        this.index = index;
    }

    void observe(long value) {
        if (value != signature) {
            signature = value;
            attempts = 0;
            next = 0;
            incompatible = false;
            reason = null;
        }
    }

    boolean mayAttempt(long now) {
        return !incompatible && attempts < 3 && now >= next;
    }

    void attempting() {
        attempts++;
    }

    int attempts() {
        return attempts;
    }

    void failed(long now, String detail) {
        reason = detail == null ? "INSTALLATION_RETURNED_NULL" : detail;
        incompatible =
                reason.contains("UNSUPPORTED_")
                        || reason.contains("UNKNOWN_")
                        || reason.contains("FOREIGN_")
                        || reason.contains("AMBIGUOUS_")
                        || reason.contains("DUPLICATE_")
                        || reason.contains("RESTORE_REFUSED")
                        || reason.contains("ROLLBACK_FAILED")
                        || reason.contains("CANNOT_AUTHORIZE");
        next = now + (attempts <= 1 ? 250000000L : 1000000000L);
    }

    String disposition() {
        return incompatible
                ? "INCOMPATIBLE_UNTIL_STRUCTURE_CHANGE"
                : attempts >= 3 ? "WAIT_STRUCTURE_CHANGE" : "TRANSIENT_RETRY";
    }

    static String identity(Object value) {
        return value == null
                ? "none"
                : value.getClass().getName()
                        + "@"
                        + Integer.toHexString(System.identityHashCode(value));
    }

    /** Samples bar geometry and readiness without traversing tab or radar contents. */
    static long signature(UIPanelAPI core, UIPanelAPI bar) throws Exception {
        long h = 17;
        if (core != null) {
            h = position(h, core);
        }
        if (bar == null) {
            return h;
        }
        h = position(h, bar);
        for (Object child : CampaignUiAccess.children(bar)) {
            h = 31 * h + System.identityHashCode(child);
            if (child instanceof UIComponentAPI && !(child instanceof ButtonAPI)) {
                h = position(h, (UIComponentAPI) child);
                // Texture readiness may change after attachment, allowing another attempt.
                // Use cached getters without inspecting component fields.
                if (CampaignUiAccess.method(child.getClass(), "getSprite") != null) {
                    Object sprite = CampaignUiAccess.read(child, "getSprite");
                    h = 31 * h + System.identityHashCode(sprite);
                    if (sprite != null
                            && CampaignUiAccess.method(sprite.getClass(), "getTexture") != null) {
                        h =
                                31 * h
                                        + System.identityHashCode(
                                                CampaignUiAccess.read(sprite, "getTexture"));
                    }
                }
            }
        }
        Object value = CampaignUiAccess.read(bar, "getButtons");
        if (!(value instanceof Map)) {
            return 31 * h + System.identityHashCode(value);
        }
        Map<?, ?> map = (Map<?, ?>) value;
        h = 31 * h + map.size();
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
            h = 31 * h + System.identityHashCode(button);
            if (button instanceof UIComponentAPI) {
                h = position(h, (UIComponentAPI) button);
            }
            if (button instanceof ButtonAPI) {
                h = 31 * h + (((ButtonAPI) button).isEnabled() ? 1 : 0);
            }
        }
        return h;
    }

    private static long position(long h, UIComponentAPI c) {
        PositionAPI p = c.getPosition();
        if (p == null) {
            return 31 * h;
        }
        for (float n : new float[] {p.getX(), p.getY(), p.getWidth(), p.getHeight()}) {
            h = 31 * h + Float.floatToIntBits(n);
        }
        return h;
    }
}
