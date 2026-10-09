package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.FieldRef;
import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Checks native bindings without remapping them; unknown structures disable the optional shortcut. */
final class CampaignHotkeyConflicts {
    static void requireFree(UIPanelAPI core, ButtonAPI target, Class<?> binding, int requestedKey)
            throws Exception {
        Class<?> id = CampaignUiAccess.method(target.getClass(), "getShortcut").getReturnType();
        Map<?, ?> keys = registry(id);
        for (Map.Entry<?, ?> entry : keys.entrySet()) {
            if (campaign(entry.getKey()) && pairUsesKey(entry.getValue(), binding, requestedKey)) {
                throw new IllegalStateException(
                        "HOTKEY_CONFLICT campaignBinding=" + ((Enum<?>) entry.getKey()).name());
            }
        }
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        Deque<Object> pending = new ArrayDeque<Object>();
        pending.add(core);
        while (!pending.isEmpty()) {
            Object component = pending.removeFirst();
            if (!seen.add(component)) {
                continue;
            }
            if (seen.size() > 2000) {
                throw new IllegalStateException("HOTKEY_CONFLICT_SCAN_LIMIT");
            }
            if (component instanceof ButtonAPI
                    && component != target
                    && ((ButtonAPI) component).isEnabled()) {
                Object logic = CampaignUiAccess.read(component, "getLogic");
                if (logic != null) {
                    for (FieldRef field : CampaignUiAccess.fields(logic.getClass())) {
                        Object value = field.read(logic);
                        if (usesKey(value, binding, requestedKey)
                                || keys.containsKey(value)
                                        && pairUsesKey(keys.get(value), binding, requestedKey)) {
                            conflict((ButtonAPI) component);
                        }
                        if (value instanceof Collection) {
                            for (Object item : (Collection<?>) value) {
                                if (usesKey(item, binding, requestedKey)
                                        || keys.containsKey(item)
                                                && pairUsesKey(
                                                        keys.get(item), binding, requestedKey)) {
                                    conflict((ButtonAPI) component);
                                }
                            }
                        }
                    }
                }
            }
            for (Object child : CampaignUiAccess.children(component)) {
                if (child != null) {
                    pending.addLast(child);
                }
            }
        }
    }

    private static void conflict(ButtonAPI button) {
        throw new IllegalStateException("HOTKEY_O_CONFLICT button=" + button.getText());
    }

    static Map<?, ?> registry(Class<?> id) throws Exception {
        String name = id.getName();
        Class<?> owner =
                Class.forName(name.substring(0, name.lastIndexOf('$')), false, id.getClassLoader());
        MethodRef getter = null;
        for (MethodRef method : CampaignHandleAccess.methods(owner)) {
            if (CampaignHandleAccess.isStatic(method.getModifiers())
                    && method.getReturnType() == Map.class
                    && method.getParameterTypes().length == 0) {
                if (getter != null) {
                    throw new IllegalStateException("HOTKEY_AMBIGUOUS_REGISTRY");
                }
                getter = method;
            }
        }
        if (getter == null) {
            throw new IllegalStateException("HOTKEY_NO_REGISTRY");
        }
        try {
            Map<?, ?> result =
                    (Map<?, ?>)
                            MethodHandles.publicLookup()
                                    .findStatic(
                                            owner,
                                            getter.getName(),
                                            MethodType.methodType(Map.class))
                                    .invokeWithArguments();
            if (result == null || result.isEmpty()) {
                throw new IllegalStateException("HOTKEY_REGISTRY_NOT_READY");
            }
            return result;
        } catch (Throwable error) {
            throw new Exception("HOTKEY_REGISTRY_READ", error);
        }
    }

    static boolean campaign(Object id) throws Exception {
        MethodRef groups = null;
        for (MethodRef method : CampaignHandleAccess.methods(id.getClass())) {
            if (method.getReturnType() == EnumSet.class && method.getParameterTypes().length == 0) {
                if (groups != null) {
                    throw new IllegalStateException("HOTKEY_AMBIGUOUS_CONTEXT");
                }
                groups = method;
            }
        }
        if (groups == null) {
            throw new IllegalStateException("HOTKEY_UNKNOWN_CONTEXT");
        }
        for (Object context : (EnumSet<?>) groups.invoke(id)) {
            for (FieldRef field : CampaignUiAccess.fields(context.getClass())) {
                if (field.getDeclaringClass() == context.getClass()
                        && field.getType() == String.class
                        && "Campaign".equals(field.read(context))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean pairUsesKey(Object pair, Class<?> binding, int requestedKey)
            throws Exception {
        if (pair == null) {
            return false;
        }
        int slots = 0;
        boolean uses = false;
        for (FieldRef field : CampaignUiAccess.fields(pair.getClass())) {
            if (field.getName().equals("one") || field.getName().equals("two")) {
                slots++;
                uses |= usesKey(field.read(pair), binding, requestedKey);
            }
        }
        if (slots != 2) {
            throw new IllegalStateException("HOTKEY_UNKNOWN_BINDING_PAIR");
        }
        return uses;
    }

    static boolean usesKey(Object value, Class<?> binding, int requestedKey) throws Exception {
        if (value == null || !binding.isInstance(value)) {
            return false;
        }
        int ints = 0, flags = 0, key = 0;
        boolean modified = false;
        for (FieldRef field : CampaignUiAccess.fields(binding)) {
            if (field.getDeclaringClass() == binding) {
                if (field.getType() == int.class) {
                    ints++;
                    key = ((Number) field.read(value)).intValue();
                }
                if (field.getType() == boolean.class) {
                    flags++;
                    modified |= (Boolean) field.read(value);
                }
            }
        }
        if (ints != 1 || flags != 3) {
            throw new IllegalStateException("HOTKEY_UNKNOWN_BINDING_SHAPE");
        }
        return key == requestedKey && !modified;
    }
}
