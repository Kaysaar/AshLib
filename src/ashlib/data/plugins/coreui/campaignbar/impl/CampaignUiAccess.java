package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.FieldRef;
import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Optional adapter; no compile-time references to game implementation classes. */
public final class CampaignUiAccess {
    private static final Class<?>[] NO_PARAMETERS = new Class<?>[0];
    private static final Object[] NO_ARGUMENTS = new Object[0];

    private static final ClassValue<MethodRef> SHOW_METHODS =
            new ClassValue<MethodRef>() {
                protected MethodRef computeValue(Class<?> type) {
                    try {
                        return findShowMethod(type);
                    } catch (RuntimeException error) {
                        throw error;
                    } catch (Exception error) {
                        throw new IllegalStateException("Host method metadata: " + type.getName(), error);
                    }
                }
            };

    private static final ClassValue<Class<?>> CLOSING_CONTRACTS =
            new ClassValue<Class<?>>() {
                protected Class<?> computeValue(Class<?> type) {
                    try {
                        return findClosingContract(type);
                    } catch (RuntimeException error) {
                        throw error;
                    } catch (Exception error) {
                        throw new IllegalStateException("Close contract metadata: " + type.getName(), error);
                    }
                }
            };

    // Index class metadata, never UI instances; ClassValue follows the class loader's lifetime.
    private static final ClassValue<Map<String, List<MethodRef>>> METHODS_BY_NAME =
            new ClassValue<Map<String, List<MethodRef>>>() {
                protected Map<String, List<MethodRef>> computeValue(Class<?> type) {
                    try {
                        Map<String, List<MethodRef>> methods = new HashMap<String, List<MethodRef>>();
                        for (MethodRef method : CampaignHandleAccess.methods(type)) {
                            if (method.isBridge()) {
                                continue;
                            }
                            List<MethodRef> overloads = methods.get(method.getName());
                            if (overloads == null) {
                                overloads = new ArrayList<MethodRef>();
                                methods.put(method.getName(), overloads);
                            }
                            overloads.add(method);
                        }

                        for (Map.Entry<String, List<MethodRef>> entry : methods.entrySet()) {
                            entry.setValue(Collections.unmodifiableList(entry.getValue()));
                        }
                        return Collections.unmodifiableMap(methods);
                    } catch (Exception error) {
                        throw new IllegalStateException("Method index: " + type.getName(), error);
                    }
                }
            };

    private static final ClassValue<List<FieldRef>> INSTANCE_FIELDS =
            new ClassValue<List<FieldRef>>() {
                protected List<FieldRef> computeValue(Class<?> type) {
                    try {
                        List<FieldRef> fields = new ArrayList<FieldRef>();
                        for (Class<?> owner = type;
                                owner != null && owner != Object.class;
                                owner = owner.getSuperclass()) {
                            for (FieldRef field : CampaignHandleAccess.declaredFields(owner)) {
                                if (!CampaignHandleAccess.isStatic(field.getModifiers())) {
                                    fields.add(field);
                                }
                            }
                        }
                        return Collections.unmodifiableList(fields);
                    } catch (Exception error) {
                        throw new IllegalStateException("Field index: " + type.getName(), error);
                    }
                }
            };

    private CampaignUiAccess() {}

    /** Invokes a required method; rejects a missing target or signature. */
    static Object call(Object target, String name, Class<?>[] args, Object... values)
            throws Exception {
        if (target == null) {
            throw new IllegalStateException("NULL_CAPABILITY " + name);
        }
        CampaignHandleAccess.MethodRef m = CampaignUiAccess.method(target.getClass(), name, args);
        if (m == null) {
            throw new IllegalStateException("MISSING_CAPABILITY " + name);
        }
        return m.invoke(target, values);
    }

    public static MethodRef method(Class<?> type, String name, Class<?>... args) throws Exception {
        List<MethodRef> overloads = METHODS_BY_NAME.get(type).get(name);
        if (overloads == null) {
            return null;
        }
        for (MethodRef m : overloads) {
            if (m.matchesParameters(args)) {
                return m;
            }
        }
        return null;
    }

    public static Object read(Object target, String name) throws Exception {
        if (target == null) {
            return null;
        }
        MethodRef m = method(target.getClass(), name, NO_PARAMETERS);
        return m == null ? null : m.invoke(target, NO_ARGUMENTS);
    }

    public static List<FieldRef> fields(Class<?> type) throws Exception {
        // Callers retain their own mutable snapshot.
        return type == null
                ? new ArrayList<FieldRef>()
                : new ArrayList<FieldRef>(INSTANCE_FIELDS.get(type));
    }

    public static Object value(FieldRef f, Object owner) throws Exception {
        return f.read(owner);
    }

    public static MethodRef showMethod(Class<?> type) throws Exception {
        return SHOW_METHODS.get(type);
    }

    private static MethodRef findShowMethod(Class<?> type) throws Exception {
        MethodRef found = null;
        for (MethodRef m : CampaignHandleAccess.methods(type)) {
            Class<?>[] parameterTypes = m.getParameterTypes();
            if (m.isBridge()
                    || CampaignHandleAccess.isStatic(m.getModifiers())
                    || !m.getName().equals("showPanelAsDialog")
                    || m.getReturnType() != void.class
                    || parameterTypes.length != 4) {
                continue;
            }
            if (!ButtonAPI.class.isAssignableFrom(parameterTypes[0])
                    || !UIPanelAPI.class.isAssignableFrom(parameterTypes[1])
                    || parameterTypes[2] != boolean.class
                    || parameterTypes[3] != boolean.class) {
                continue;
            }
            if (found != null) {
                throw new IllegalStateException("Ambiguous host signature");
            }
            found = m;
        }
        return found;
    }

    /** Finds Core UI through fields and getters without opening a tab. */
    public static UIPanelAPI findCore(Object owner) throws Exception {
        return findCore(owner, true);
    }

    /** Finds Core UI for inspection without requiring the screen-host contract. */
    public static UIPanelAPI findProbeCore(Object owner) throws Exception {
        return findCore(owner, false);
    }

    private static boolean coreShape(Class<?> type, boolean host) throws Exception {
        return UIPanelAPI.class.isAssignableFrom(type)
                && (host
                        ? showMethod(type) != null
                        : method(type, "getButtons", NO_PARAMETERS) != null
                                && method(type, "getCurrentTab", NO_PARAMETERS) != null);
    }

    private static UIPanelAPI findCore(Object owner, boolean host) throws Exception {
        if (owner == null) {
            return null;
        }
        Object candidate = coreShape(owner.getClass(), host) ? owner : null;
        boolean ambiguous = false;
        Object getter = read(owner, "getCoreUI");
        if (getter != null && coreShape(getter.getClass(), host)) {
            ambiguous = candidate != null && candidate != getter;
            if (candidate == null) {
                candidate = getter;
            }
        }
        for (FieldRef field : INSTANCE_FIELDS.get(owner.getClass())) {
            if (!coreShape(field.getType(), host)) {
                continue;
            }
            Object v = value(field, owner);
            if (v != null) {
                ambiguous |= candidate != null && candidate != v;
                if (candidate == null) {
                    candidate = v;
                }
            }
        }
        if (ambiguous) {
            throw new IllegalStateException("Multiple core candidates");
        }
        return (UIPanelAPI) candidate;
    }

    public static List<?> children(Object panel) throws Exception {
        return childList(panel, true);
    }

    /** Read-only, immediate inspection; never retain this borrowed list across UI operations. */
    static List<?> inspectChildren(Object panel) throws Exception {
        return childList(panel, false);
    }

    private static List<?> childList(Object panel, boolean snapshot) throws Exception {
        if (!(panel instanceof UIPanelAPI)) {
            return Collections.emptyList();
        }
        MethodRef m = method(panel.getClass(), "getChildrenCopy", NO_PARAMETERS);
        if (m == null) {
            m = method(panel.getClass(), "getChildrenNonCopy", NO_PARAMETERS);
            snapshot = true;
        }
        if (m == null || !List.class.isAssignableFrom(m.getReturnType())) {
            throw new IllegalStateException("Missing children accessor");
        }
        List<?> children = (List<?>) m.invoke(panel, NO_ARGUMENTS);
        if (children == null) {
            throw new IllegalStateException("Null children list");
        }
        return snapshot ? new ArrayList<Object>(children) : children;
    }

    public static boolean validContract(Class<?> iface) throws Exception {
        if (!iface.isInterface() || !CampaignHandleAccess.isPublic(iface.getModifiers())) {
            return false;
        }
        int count = 0;
        for (MethodRef m : CampaignHandleAccess.methods(iface)) {
            if (!CampaignHandleAccess.isAbstract(m.getModifiers())
                    || CampaignHandleAccess.isStatic(m.getModifiers())) {
                continue;
            }
            count++;
            if (!m.getName().equals("tabClosed")
                    || m.getReturnType() != void.class
                    || m.getParameterTypes().length != 0) {
                return false;
            }
        }
        return count == 1;
    }

    public static Class<?> closingContract(Class<?> tabType) throws Exception {
        return tabType == null ? null : CLOSING_CONTRACTS.get(tabType);
    }

    private static Class<?> findClosingContract(Class<?> tabType) throws Exception {
        Set<Class<?>> interfaces =
                Collections.newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());
        collectInterfaces(tabType, interfaces);
        Class<?> found = null;
        for (Class<?> iface : interfaces) {
            if (validContract(iface)) {
                if (found != null && found != iface) {
                    throw new IllegalStateException("Ambiguous close contract");
                }
                found = iface;
            }
        }
        return found;
    }

    private static void collectInterfaces(Class<?> type, Set<Class<?>> out) {
        if (type == null) {
            return;
        }
        for (Class<?> iface : type.getInterfaces()) {
            if (out.add(iface)) {
                collectInterfaces(iface, out);
            }
        }
        collectInterfaces(type.getSuperclass(), out);
    }
}
