package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

/** Validates RC8 viewport types obtained from public-API-created samples. */
public final class CampaignViewportContract {
    public static final String PROFILE = CampaignCompatibility.VIEWPORT_PROFILE;
    public final Class<?> scroll, events, button, renderer, listener, event;
    public final MethodHandle viewportConstructor, buttonConstructor;
    public final MethodRef cancelCapture;

    public CampaignViewportContract(Class<?> scroll, Class<?> button, String version)
            throws Exception {
        need(CampaignCompatibility.GAME_VERSION.equals(version), "UNSUPPORTED_VIEWPORT_VERSION");
        this.scroll = scroll;
        this.button = button;
        need(
                ScrollPanelAPI.class.isAssignableFrom(scroll) && concrete(scroll),
                "UNKNOWN_SCROLL_TYPE");
        MethodRef input = null;
        for (MethodRef method : CampaignHandleAccess.methods(scroll)) {
            if (method.getName().equals("processScrollInputOnly") && !method.isBridge()) {
                need(input == null, "AMBIGUOUS_SCROLL_INPUT");
                input = method;
            }
        }
        need(
                input != null
                        && input.getReturnType() == void.class
                        && !CampaignHandleAccess.isFinal(input.getModifiers()),
                "NO_SCROLL_INPUT_HOOK");
        Class<?>[] parameterTypes = input.getParameterTypes();
        need(
                parameterTypes.length == 2
                        && List.class.isAssignableFrom(parameterTypes[0])
                        && parameterTypes[1] == boolean.class,
                "UNKNOWN_SCROLL_INPUT_SIGNATURE");
        events = parameterTypes[0];
        required(scroll, "setContentSize", void.class, float.class, float.class);
        required(scroll, "setShowScrollbars", void.class, boolean.class);
        required(scroll, "setUseMWheel", void.class, boolean.class);
        required(scroll, "setMaxShadowHeight", void.class, float.class);
        required(scroll, "setUseSimpleShadows", void.class, boolean.class);
        MethodRef content = CampaignUiAccess.method(scroll, "getContentContainer");
        need(
                content != null && UIPanelAPI.class.isAssignableFrom(content.getReturnType()),
                "NO_CONTENT_CONTAINER");
        need(ButtonAPI.class.isAssignableFrom(button) && concrete(button), "UNKNOWN_BUTTON_TYPE");
        MethodRef render = CampaignUiAccess.method(button, "getRenderer"),
                action = CampaignUiAccess.method(button, "getListener");
        need(render != null && action != null, "NO_BUTTON_FACTORY_SHAPE");
        renderer = render.getReturnType();
        listener = action.getReturnType();
        MethodRef enter = null;
        for (MethodRef method : CampaignHandleAccess.methods(button)) {
            if (method.getName().equals("mouseEnter") && !method.isBridge()) {
                need(enter == null, "AMBIGUOUS_MOUSE_OBSERVER");
                enter = method;
            }
        }
        need(enter != null, "NO_MOUSE_OBSERVER");
        parameterTypes = enter.getParameterTypes();
        need(
                parameterTypes.length == 2
                        && InputEventAPI.class.isAssignableFrom(parameterTypes[0])
                        && parameterTypes[1] == Object.class,
                "UNKNOWN_MOUSE_OBSERVER");
        event = parameterTypes[0];
        required(button, "mouseEnter", void.class, event, Object.class);
        required(button, "mouseLeave", void.class, event, Object.class);
        required(button, "buttonPressed", void.class, event, Object.class);
        for (String name : new String[] {"buttonDown", "buttonUp", "buttonPressCancelled"}) {
            required(button, name, void.class, Object.class);
        }
        MethodRef logicGetter = CampaignUiAccess.method(button, "getLogic");
        need(logicGetter != null, "NO_BUTTON_LOGIC");
        Class<?> logic = logicGetter.getReturnType();
        required(logic, "processInput", void.class, events);
        required(logic, "focusLost", void.class);
        MethodRef cancel = null;
        // RC8 has one additional public void/no-arg method that resets the button and releases
        // owned capture.
        // focusLost alone does not release capture.
        for (MethodRef method : CampaignHandleAccess.methods(logic)) {
            if (method.getDeclaringClass() == logic
                    && method.getReturnType() == void.class
                    && method.getParameterTypes().length == 0
                    && !method.getName().equals("focusLost")
                    && !method.isBridge()
                    && !CampaignHandleAccess.isStatic(method.getModifiers())) {
                need(cancel == null, "AMBIGUOUS_CAPTURE_CANCEL");
                cancel = method;
            }
        }
        need(cancel != null, "NO_OWNED_CAPTURE_CANCEL");
        cancelCapture = cancel;
        ClassLoader parent = CampaignViewportContract.class.getClassLoader();
        for (Class<?> type : new Class<?>[] {scroll, events, button, renderer, listener, event}) {
            need(Class.forName(type.getName(), false, parent) == type, "VIEWPORT_LOADER_IDENTITY");
        }
        MethodHandles.publicLookup().findConstructor(scroll, MethodType.methodType(void.class));
        MethodHandles.publicLookup()
                .findConstructor(button, MethodType.methodType(void.class, renderer, listener));
        // Validate bridge definition and linkage before accepting the contract.
        viewportConstructor = CampaignViewportBridge.viewport(scroll, events);
        buttonConstructor = CampaignViewportBridge.button(button, renderer, listener, event);
    }

    public static void required(Class<?> type, String name, Class<?> result, Class<?>... args)
            throws Exception {
        MethodRef method = CampaignUiAccess.method(type, name, args);
        need(
                method != null
                        && method.getReturnType() == result
                        && !CampaignHandleAccess.isStatic(method.getModifiers())
                        && !CampaignHandleAccess.isFinal(method.getModifiers()),
                "VIEWPORT_METHOD " + name);
    }

    private static boolean concrete(Class<?> type) {
        int f = type.getModifiers();
        return CampaignHandleAccess.isPublic(f)
                && !CampaignHandleAccess.isFinal(f)
                && !CampaignHandleAccess.isAbstract(f);
    }

    static void need(boolean ok, String reason) {
        if (!ok) {
            throw new IllegalStateException(reason);
        }
    }
}
