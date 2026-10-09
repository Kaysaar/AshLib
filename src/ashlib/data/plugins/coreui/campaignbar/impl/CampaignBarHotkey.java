package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.api.TabSpec;
import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.FieldRef;
import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Adds native hotkey rendering and binding without replacing the tab or listener. */
public final class CampaignBarHotkey {
    private final ButtonAPI button;
    private final Object logic, originalRenderer, originalBinding, renderer, binding;
    private final FieldRef slot;
    private final MethodRef setRenderer;
    private boolean started, applied;

    public static boolean accept(InputEventAPI event) {
        return event != null
                && !event.isConsumed()
                && event.isKeyDownEvent()
                && !event.isRepeat()
                && event.isUnmodified();
    }

    static CampaignBarHotkey prepare(
            UIPanelAPI core, UIPanelAPI bar, ButtonAPI button, TabSpec spec) throws Exception {
        require(
                spec.title.equals(button.getText())
                        && CampaignUiAccess.read(button, "getRenderer") != null,
                "UNKNOWN_BUTTON_PRESENTATION");
        ButtonAPI scratch;
        try {
            UIPanelAPI detached =
                    (UIPanelAPI)
                            MethodHandles.publicLookup()
                                    .findConstructor(
                                            bar.getClass(), MethodType.methodType(void.class))
                                    .invokeWithArguments();
            MethodRef add = null;
            for (MethodRef method : CampaignHandleAccess.methods(bar.getClass())) {
                if (method.getName().equals("addButton") && !method.isBridge()) {
                    require(add == null, "AMBIGUOUS_FACTORY");
                    add = method;
                }
            }
            require(add != null && add.getParameterTypes().length == 4, "NO_FACTORY");
            Object action =
                    CampaignRuntimeBridge.actionListener(
                            add.getParameterTypes()[3],
                            new Runnable() {
                                public void run() {}
                            });
            add.invoke(detached, spec.title, "ashlib.campaignbar.hotkey.scratch", null, action);
            scratch =
                    (ButtonAPI)
                            ((Map<?, ?>) CampaignUiAccess.read(detached, "getButtons"))
                                    .get("ashlib.campaignbar.hotkey.scratch");
            detached.removeComponent(scratch);
        } catch (Throwable error) {
            throw new Exception("HOTKEY_NATIVE_PRESENTATION", error);
        }
        CampaignBarHotkey result = new CampaignBarHotkey(button, scratch, spec.key);
        require(spec.title.equals(scratch.getText()), "NATIVE_LABEL_CHANGED");
        CampaignHotkeyConflicts.requireFree(core, button, result.slot.getType(), spec.key);
        return result;
    }

    /** Uses an unattached button to obtain the native mnemonic color and underline. */
    CampaignBarHotkey(ButtonAPI button, ButtonAPI scratch, int key) throws Exception {
        this.button = button;
        logic = CampaignUiAccess.read(button, "getLogic");
        Object scratchLogic = CampaignUiAccess.read(scratch, "getLogic");
        require(logic != null && logic.getClass() == scratchLogic.getClass(), "LOGIC_PROFILE");
        originalRenderer = CampaignUiAccess.read(button, "getRenderer");
        renderer = CampaignUiAccess.read(scratch, "getRenderer");
        require(
                originalRenderer == null
                        ? renderer == null
                        : renderer != originalRenderer
                                && renderer.getClass() == originalRenderer.getClass(),
                "RENDERER_PROFILE");
        Class<?> rendererType =
                CampaignUiAccess.method(button.getClass(), "getRenderer").getReturnType();
        setRenderer = CampaignUiAccess.method(button.getClass(), "setRenderer", rendererType);
        require(setRenderer != null, "NO_RENDERER_RESTORE");
        List<FieldRef> fields = new ArrayList<FieldRef>();
        List<Object> values = new ArrayList<Object>();
        for (FieldRef field : CampaignUiAccess.fields(scratchLogic.getClass())) {
            if (!field.getType().isPrimitive()
                    && !CampaignHandleAccess.isStatic(field.getModifiers())) {
                fields.add(field);
                values.add(field.read(scratchLogic));
            }
        }
        scratch.setShortcut(key, false);
        FieldRef changed = null;
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).read(scratchLogic) != values.get(i)) {
                require(changed == null && values.get(i) == null, "UNKNOWN_BINDING_MUTATION");
                changed = fields.get(i);
            }
        }
        require(
                changed != null && !CampaignHandleAccess.isFinal(changed.getModifiers()),
                "NO_REVERSIBLE_BINDING_SLOT");
        slot = changed;
        originalBinding = slot.read(logic);
        require(
                originalBinding == null && CampaignUiAccess.read(button, "getShortcut") == null,
                "BUTTON_ALREADY_BOUND");
        Class<?> id = CampaignUiAccess.method(button.getClass(), "getShortcut").getReturnType();
        require(id.isEnum(), "NO_NATIVE_KEY_ENUM");
        MethodRef matcher = null;
        for (MethodRef method : CampaignHandleAccess.methods(slot.getType())) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getReturnType() == boolean.class
                    && parameterTypes.length == 2
                    && InputEventAPI.class.isAssignableFrom(parameterTypes[0])
                    && parameterTypes[1].isEnum()) {
                require(matcher == null, "AMBIGUOUS_KEY_MATCHER");
                matcher = method;
            }
        }
        require(matcher != null, "NO_NATIVE_KEY_MATCHER");
        binding = CampaignViewportBridge.shortcut(slot.getType(), id, matcher, key);
    }

    void apply() throws Exception {
        apply(
                new Runnable() {
                    public void run() {}
                });
    }

    void apply(Runnable checkpoint) throws Exception {
        require(
                !started
                        && slot.read(logic) == originalBinding
                        && CampaignUiAccess.read(button, "getRenderer") == originalRenderer,
                "PRE_APPLY_CHANGED");
        started = true;
        try {
            setRenderer.invoke(button, renderer);
            checkpoint.run();
            slot.write(logic, binding);
            checkpoint.run();
            applied = true;
            verify();
        } catch (Exception error) {
            restore();
            throw error;
        }
    }

    void verify() throws Exception {
        require(
                applied
                        && slot.read(logic) == binding
                        && CampaignUiAccess.read(button, "getRenderer") == renderer,
                "OWNERSHIP_CHANGED");
    }

    ButtonAPI button() {
        return button;
    }

    Object renderer() {
        return renderer;
    }

    Class<?> bindingType() {
        return slot.getType();
    }

    void restore() throws Exception {
        if (!started) {
            return;
        }
        Object current = slot.read(logic), visual = CampaignUiAccess.read(button, "getRenderer");
        require(
                (current == binding || current == originalBinding)
                        && (visual == renderer || visual == originalRenderer),
                "FOREIGN_CHANGE_REFUSE_RESTORE");
        slot.write(logic, originalBinding);
        setRenderer.invoke(button, originalRenderer);
        started = false;
        applied = false;
    }

    private static void require(boolean condition, String reason) {
        if (!condition) {
            throw new IllegalStateException("HOTKEY_" + reason);
        }
    }
}
