package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.FieldRef;
import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.Map;

/** Validates the RC8 Refit transition; Core owns confirmations, save/undo, closure and retry. */
final class CampaignRefitTransition {
    static final String PROFILE = CampaignCompatibility.REFIT_PROFILE;
    final UIPanelAPI core;
    final Object before;
    final String tabId;
    final FieldRef next, dialog;

    private CampaignRefitTransition(UIPanelAPI core, FieldRef[] state, String tabId)
            throws Exception {
        this.tabId = tabId;
        this.core = core;
        next = state[0];
        dialog = state[1];
        before = CampaignUiAccess.read(core, "getCurrentTab");
    }

    static FieldRef[] resolve(Class<?> type, String version) throws Exception {
        need(CampaignCompatibility.GAME_VERSION.equals(version), "UNSUPPORTED_VERSION");
        MethodRef show = CampaignUiAccess.showMethod(type);
        need(show != null && show.getDeclaringClass() == type, "UNKNOWN_SHOW_OWNER");
        Class<?>[] parameterTypes = show.getParameterTypes();
        MethodRef full =
                CampaignUiAccess.method(
                        type,
                        "showPanelAsDialog",
                        parameterTypes[0],
                        parameterTypes[1],
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        boolean.class);
        need(
                full != null
                        && full.getReturnType() == void.class
                        && full.getDeclaringClass() == type,
                "MISSING_NATIVE_GATED_OVERLOAD");
        need(
                CampaignUiAccess.method(type, "setCurrentTab", Object.class) != null
                        && CampaignUiAccess.method(type, "closeCurrent") != null,
                "NO_NATIVE_RESUME_CONTRACT");
        MethodRef warning = null;
        for (MethodRef method : CampaignHandleAccess.methods(type)) {
            if (method.getName().equals("showWeaponGroupsWarningIfNeeded")
                    && method.getReturnType() == boolean.class
                    && method.getParameterTypes().length == 1) {
                need(warning == null, "AMBIGUOUS_WARNING_CONTRACT");
                warning = method;
            }
        }
        need(warning != null, "NO_NATIVE_REFIT_GATE");
        FieldRef next = null, dialog = null;
        for (FieldRef field : CampaignUiAccess.fields(type)) {
            if (field.getDeclaringClass() == type) {
                if (field.getType() == Object.class) {
                    need(next == null, "AMBIGUOUS_DEFERRED_TARGET");
                    next = field;
                }
                if (field.getType().isEnum()
                        && field.getType().getDeclaringClass() == type
                        && field.getType().getEnumConstants().length == 2) {
                    need(dialog == null, "AMBIGUOUS_DIALOG_STATE");
                    dialog = field;
                }
            }
        }
        need(next != null && dialog != null, "NO_NATIVE_DEFERRED_STATE");
        return new FieldRef[] {next, dialog};
    }

    static CampaignRefitTransition prepare(
            UIPanelAPI core, ButtonAPI button, String version, String tabId) throws Exception {
        need(button != null && button.isEnabled(), "REGISTERED_ENABLED_BUTTON_REQUIRED");
        CampaignRefitTransition result =
                new CampaignRefitTransition(core, resolve(core.getClass(), version), tabId);
        Object bar = CampaignUiAccess.read(core, "getButtons"),
                forward = CampaignUiAccess.read(bar, "getButtons");
        need(
                forward instanceof Map && ((Map<?, ?>) forward).get(tabId) == button,
                "FORWARD_IDENTITY");
        MethodRef reverse = null;
        for (MethodRef method : CampaignHandleAccess.methods(bar.getClass())) {
            if (method.getName().equals("getTabId")
                    && !method.isBridge()
                    && method.getParameterTypes().length == 1
                    && ButtonAPI.class.isAssignableFrom(method.getParameterTypes()[0])) {
                need(reverse == null, "AMBIGUOUS_REVERSE_IDENTITY");
                reverse = method;
            }
        }
        need(reverse != null && tabId.equals(reverse.invoke(bar, button)), "REVERSE_IDENTITY");
        need(CampaignUiAccess.read(button, "getListener") != null, "NO_RESUME_CALLBACK");
        // The native gate returns false during an unsaved-changes callback.
        // Wait until any pending confirmation has completed before issuing another request.
        need(
                result.next.read(core) == null && result.dialog.read(core) == null,
                "NATIVE_CONFIRMATION_ALREADY_ACTIVE");
        return result;
    }

    boolean deferred() throws Exception {
        return CampaignUiAccess.read(core, "getCurrentTab") == before
                && tabId.equals(next.read(core));
    }

    String state() throws Exception {
        Object panel = CampaignUiAccess.read(before, "getRefitPanel");
        need(
                before == null
                        || CampaignUiAccess.method(before.getClass(), "getRefitPanel") == null
                        || panel != null,
                "REFIT_PANEL_NOT_READY");
        return "profile="
                + PROFILE
                + " actualRefit="
                + (panel != null)
                + " edited="
                + CampaignUiAccess.read(panel, "isEditedSinceSave");
    }

    private static void need(boolean ok, String why) {
        if (!ok) {
            throw new IllegalStateException(PROFILE + ": " + why);
        }
    }
}
