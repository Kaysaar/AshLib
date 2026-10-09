package ashlib.data.plugins.coreui.campaignbar.impl;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Reparents registered buttons reversibly without changing their registry mappings. */
final class CampaignBarReparent {
    final UIPanelAPI bar;
    final List<ButtonAPI> buttons;
    final List<?> children;
    private final List<Object[]> identities = new ArrayList<Object[]>();
    private UIPanelAPI track;
    private final Map<ButtonAPI, Object> decorated = new IdentityHashMap<ButtonAPI, Object>();

    void expectRenderer(ButtonAPI button, Object renderer) {
        if (button == null) {
            decorated.clear();
        } else {
            decorated.put(button, renderer);
        }
    }

    CampaignBarReparent(UIPanelAPI bar, List<ButtonAPI> order) throws Exception {
        this.bar = bar;
        buttons = new ArrayList<ButtonAPI>(order);
        children = CampaignUiAccess.children(bar);
        need(
                !buttons.isEmpty() && new HashSet<ButtonAPI>(buttons).size() == buttons.size(),
                "INVALID_BUTTON_IDENTITIES");
        // Reparenting requires native add/remove methods, not mod overrides.
        Class<?> panelOwner =
                CampaignUiAccess.method(bar.getClass(), "addComponent", UIComponentAPI.class)
                        .getDeclaringClass();
        need(
                CampaignUiAccess.method(bar.getClass(), "removeComponent", UIComponentAPI.class)
                                .getDeclaringClass()
                        == panelOwner,
                "MIXED_REPARENT_IMPLEMENTATION");
        for (ButtonAPI b : buttons) {
            identities.add(
                    new Object[] {
                        CampaignUiAccess.read(b, "getListener"),
                        CampaignUiAccess.read(b, "getShortcut"),
                        CampaignUiAccess.read(b, "getRenderer"),
                        b.getCustomData()
                    });
        }
        verifyOriginal();
    }

    static void need(boolean value, String why) {
        if (!value) {
            throw new IllegalStateException("NAV_" + why);
        }
    }

    static boolean near(float a, float b) {
        return Float.isFinite(a) && Float.isFinite(b) && Math.abs(a - b) < 0.05f;
    }

    void verifyIdentity() throws Exception {
        for (int i = 0; i < buttons.size(); i++) {
            ButtonAPI b = buttons.get(i);
            Object[] id = identities.get(i);
            need(
                    CampaignUiAccess.read(b, "getListener") == id[0]
                            && CampaignUiAccess.read(b, "getShortcut") == id[1]
                            && CampaignUiAccess.read(b, "getRenderer")
                                    == (decorated.containsKey(b) ? decorated.get(b) : id[2])
                            && b.getCustomData() == id[3],
                    "BUTTON_IDENTITY_CHANGED_" + i);
        }
    }

    void verifyOriginal() throws Exception {
        need(children.equals(CampaignUiAccess.children(bar)), "ORIGINAL_CHILD_ORDER");
        verifyAt(bar, 13);
        verifyIdentity();
    }

    void verifyAt(UIPanelAPI parent, float first) throws Exception {
        List<?> list = CampaignUiAccess.children(parent);
        for (int i = 0; i < buttons.size(); i++) {
            ButtonAPI b = buttons.get(i);
            PositionAPI position = b.getPosition();
            need(
                    list.contains(b)
                            && CampaignUiAccess.read(b, "getParent") == parent
                            && CampaignUiAccess.read(position, "getParent") == parent.getPosition(),
                    "PARENT_" + i);
            need(
                    CampaignUiAccess.read(position, "getBase")
                                    == (i == 0 ? null : buttons.get(i - 1).getPosition())
                            && !position.isSuspendRecompute(),
                    "ANCHOR_" + i);
            need(
                    near(position.getWidth(), 119)
                            && near(position.getHeight(), 21)
                            && near(position.getX(), parent.getPosition().getX() + first + i * 125)
                            && near(position.getY(), parent.getPosition().getY() + 4),
                    "GEOMETRY_" + i);
            need(
                    near(
                                    ((Number) CampaignUiAccess.read(position, "getXAlignOffset"))
                                            .floatValue(),
                                    i == 0 ? first : 6)
                            && near(
                                    ((Number) CampaignUiAccess.read(position, "getYAlignOffset"))
                                            .floatValue(),
                                    i == 0 ? 4 : 0),
                    "OFFSET_" + i);
        }
    }

    void moveTo(UIPanelAPI destination, Runnable checkpoint) throws Exception {
        need(
                track == null && CampaignUiAccess.children(destination).isEmpty(),
                "TRACK_ALREADY_USED");
        track = destination;
        // Detach sibling anchors before changing any parent.
        detachAll();
        checkpoint.run();
        for (ButtonAPI b : buttons) {
            bar.removeComponent(b);
            destination.addComponent(b).inBL(0, 4);
            checkpoint.run();
        }
        chain(0);
        verifyAt(destination, 0);
        verifyIdentity();
    }

    private void detachAll() {
        for (ButtonAPI b : buttons) {
            b.getPosition().inBL(0, 4);
        }
    }

    private void chain(float first) {
        for (int i = 0; i < buttons.size(); i++) {
            PositionAPI position = buttons.get(i).getPosition();
            if (i == 0) {
                position.inBL(first, 4);
            } else {
                position.rightOfMid(buttons.get(i - 1), 6);
            }
        }
    }

    void restore() throws Exception {
        // Allow partial movement, but reject buttons reparented by another owner.
        for (ButtonAPI b : buttons) {
            Object parent = CampaignUiAccess.read(b, "getParent");
            need(
                    parent == bar || parent == track || parent == null,
                    "FOREIGN_PARENT_DURING_RESTORE");
        }
        detachAll();
        for (ButtonAPI b : buttons) {
            Object parent = CampaignUiAccess.read(b, "getParent");
            if (parent != null) {
                ((UIPanelAPI) parent).removeComponent(b);
            }
        }
        // Restore original child order; appended tabs can differ from visual anchor order.
        for (Object child : children) {
            if (buttons.contains(child)) {
                bar.addComponent((UIComponentAPI) child).inBL(0, 4);
            }
        }
        chain(13);
        track = null;
        verifyOriginal();
    }
}
