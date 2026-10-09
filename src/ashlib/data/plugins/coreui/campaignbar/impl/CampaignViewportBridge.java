package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Generates subclasses only for newly created navigation components. */
public final class CampaignViewportBridge {
    public interface Hooks {
        void before(List<?> events);

        void after(List<?> events);

        void mouse(int slot, boolean entered);

        void press(int slot, int state);

        boolean activation(int slot, com.fs.starfarer.api.input.InputEventAPI event);

        void failure(Throwable error);
    }

    private static final String HOOKS = Hooks.class.getName().replace('.', '/');
    private static final String HD = "L" + HOOKS + ";";

    private CampaignViewportBridge() {}

    /** Rejects key modifiers while preserving native shortcut dispatch, focus and rendering. */
    static Object shortcut(Class<?> base, Class<?> id, MethodRef matcher, int key)
            throws Exception {
        if (!CampaignHandleAccess.isPublic(base.getModifiers())
                || CampaignHandleAccess.isFinal(base.getModifiers())
                || CampaignHandleAccess.isFinal(matcher.getModifiers())) {
            throw new IllegalArgumentException("HOTKEY_BINDING_NOT_EXTENSIBLE");
        }
        Class<?>[] p = matcher.getParameterTypes();
        String ctor = "(" + desc(id) + "IZZZ)V", match = "(" + desc(p[0]) + desc(p[1]) + ")Z";
        MethodHandles.publicLookup()
                .findConstructor(
                        base,
                        MethodType.methodType(
                                void.class,
                                id,
                                int.class,
                                boolean.class,
                                boolean.class,
                                boolean.class));
        Emitter e = new Emitter("UnmodifiedCampaignShortcut", base);
        Bytes c =
                new Bytes().op(42, 1, 27, 3, 3, 3).ref(183, e.method(base, "<init>", ctor)).op(177);
        e.code("<init>", "(I)V", 6, 2, c, null);
        c =
                new Bytes()
                        .op(43)
                        .ref(
                                184,
                                e.method(
                                        CampaignBarHotkey.class,
                                        "accept",
                                        "(Lcom/fs/starfarer/api/input/InputEventAPI;)Z"));
        c.op(154)
                .u2(5)
                .op(3, 172, 42, 43, 44)
                .ref(183, e.method(base, matcher.getName(), match))
                .op(172);
        e.code(matcher.getName(), match, 3, 3, c, null);
        try {
            return MethodHandles.publicLookup()
                    .findConstructor(e.define(), MethodType.methodType(void.class, int.class))
                    .invokeWithArguments(key);
        } catch (Throwable error) {
            throw new Exception("HOTKEY_STRICT_BINDING", error);
        }
    }

    public static MethodHandle viewport(Class<?> base, Class<?> events) throws Exception {
        Emitter e = new Emitter("ExperimentalViewport", base);
        int hook = e.field("hooks", HD), superCtor = e.method(base, "<init>", "()V");
        Bytes c = new Bytes();
        c.op(42).ref(183, superCtor).op(42, 43).ref(181, hook).op(177);
        e.code("<init>", "(" + HD + ")V", 2, 2, c, null);
        String input = "(" + desc(events) + ")V";
        c = new Bytes();
        c.op(42).ref(180, hook).op(43).iface(e.hook("before", "(Ljava/util/List;)V"), 2);
        c.op(42, 43).ref(183, e.method(base, "processInputImpl", input));
        c.op(42).ref(180, hook).op(43).iface(e.hook("after", "(Ljava/util/List;)V"), 2);
        int end = c.size;
        c.op(177);
        int handler = c.size;
        c.op(77, 42)
                .ref(180, hook)
                .op(44)
                .iface(e.hook("failure", "(Ljava/lang/Throwable;)V"), 2)
                .op(177);
        e.code(
                "processInputImpl",
                input,
                2,
                3,
                c,
                new int[] {0, end, handler, e.pool.cls("java/lang/Throwable")});
        e.code(
                "processScrollInputOnly",
                "(" + desc(events) + "Z)V",
                0,
                3,
                new Bytes().op(177),
                null);
        // Disable native offset changes so navigation remains discrete and index-driven.
        for (String name :
                new String[] {
                    "scrollToY",
                    "scrollToBottom",
                    "resetOffset",
                    "clampOffset",
                    "restoreVerticalScrollState",
                    "ensureVisible",
                    "setOffset"
                }) {
            MethodRef found = null;
            for (MethodRef m : CampaignHandleAccess.methods(base)) {
                if (m.getName().equals(name) && !m.isBridge()) {
                    if (found != null) {
                        throw new IllegalStateException("AMBIGUOUS_SCROLL_MUTATOR " + name);
                    }
                    found = m;
                }
            }
            if (found == null
                    || found.getReturnType() != void.class
                    || CampaignHandleAccess.isFinal(found.getModifiers())) {
                throw new IllegalStateException("MISSING_SCROLL_MUTATOR " + name);
            }
            StringBuilder d = new StringBuilder("(");
            for (Class<?> p : found.getParameterTypes()) {
                d.append(desc(p));
            }
            d.append(")V");
            e.code(
                    name,
                    d.toString(),
                    0,
                    1 + found.getParameterTypes().length,
                    new Bytes().op(177),
                    null);
        }
        return constructor(e.define(), Hooks.class);
    }

    public static MethodHandle button(
            Class<?> base, Class<?> renderer, Class<?> listener, Class<?> event) throws Exception {
        Emitter e = new Emitter("ObservedNativeButton", base);
        int hook = e.field("hooks", HD), slot = e.field("slot", "I");
        String nativeCtor = "(" + desc(renderer) + desc(listener) + ")V";
        Bytes c = new Bytes().op(42, 43, 44).ref(183, e.method(base, "<init>", nativeCtor));
        c.op(42, 45).ref(181, hook).op(42, 21, 4).ref(181, slot).op(177);
        e.code("<init>", "(" + desc(renderer) + desc(listener) + HD + "I)V", 3, 5, c, null);
        String mouse = "(" + desc(event) + "Ljava/lang/Object;)V";
        for (String name : new String[] {"mouseEnter", "mouseLeave"}) {
            c = new Bytes().op(42, 43, 44).ref(183, e.method(base, name, mouse));
            c.op(42)
                    .ref(180, hook)
                    .op(42)
                    .ref(180, slot)
                    .op(name.equals("mouseEnter") ? 4 : 3)
                    .iface(e.hook("mouse", "(IZ)V"), 3)
                    .op(177);
            e.code(name, mouse, 3, 3, c, null);
        }
        String[] states = {"buttonUp", "buttonDown", "buttonPressCancelled"};
        for (int i = 0; i < states.length; i++) {
            c = new Bytes().op(42, 43).ref(183, e.method(base, states[i], "(Ljava/lang/Object;)V"));
            c.op(42)
                    .ref(180, hook)
                    .op(42)
                    .ref(180, slot)
                    .op(3 + i)
                    .iface(e.hook("press", "(II)V"), 3)
                    .op(177);
            e.code(states[i], "(Ljava/lang/Object;)V", 3, 2, c, null);
        }
        c =
                new Bytes()
                        .op(42)
                        .ref(180, hook)
                        .op(42)
                        .ref(180, slot)
                        .op(43)
                        .iface(
                                e.hook(
                                        "activation",
                                        "(ILcom/fs/starfarer/api/input/InputEventAPI;)Z"),
                                3);
        // ifeq skips the six-byte native callback invocation and lands on return.
        c.op(153).u2(9);
        c.op(42, 43, 44).ref(183, e.method(base, "buttonPressed", mouse)).op(177);
        e.code("buttonPressed", mouse, 3, 3, c, null);
        return constructor(e.define(), renderer, listener, Hooks.class, int.class);
    }

    private static MethodHandle constructor(Class<?> type, Class<?>... args) throws Exception {
        return MethodHandles.publicLookup()
                .findConstructor(type, MethodType.methodType(void.class, args));
    }

    private static String desc(Class<?> c) {
        if (!c.isPrimitive()) {
            return "L" + c.getName().replace('.', '/') + ";";
        }
        if (c == void.class) {
            return "V";
        }
        if (c == int.class) {
            return "I";
        }
        if (c == float.class) {
            return "F";
        }
        if (c == boolean.class) {
            return "Z";
        }
        throw new IllegalArgumentException("Unsupported bridge parameter " + c);
    }

    private static final class Loader extends ClassLoader {
        Loader() {
            super(CampaignViewportBridge.class.getClassLoader());
        }

        Class<?> define(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }

    private static final class Emitter {
        final Pool pool = new Pool();
        final String name;
        final int own, parent, code;
        final List<int[]> fields = new ArrayList<int[]>();
        final List<byte[]> methods = new ArrayList<byte[]>();

        Emitter(String name, Class<?> base) {
            this.name = "ashlib/data/plugins/coreui/campaignbar/impl/generated/" + name;
            own = pool.cls(this.name);
            parent = pool.cls(base.getName().replace('.', '/'));
            code = pool.utf("Code");
        }

        int field(String name, String d) {
            int n = pool.utf(name), v = pool.utf(d);
            fields.add(new int[] {n, v});
            return pool.ref(9, own, pool.pair(n, v));
        }

        int method(Class<?> base, String name, String d) {
            return pool.ref(
                    10,
                    pool.cls(base.getName().replace('.', '/')),
                    pool.pair(pool.utf(name), pool.utf(d)));
        }

        int hook(String name, String d) {
            return pool.ref(11, pool.cls(HOOKS), pool.pair(pool.utf(name), pool.utf(d)));
        }

        void code(String name, String d, int stack, int locals, Bytes body, int[] handler) {
            Bytes m = new Bytes();
            m.u2(1).u2(pool.utf(name)).u2(pool.utf(d)).u2(1).u2(code);
            m.u4(12 + body.size + (handler == null ? 0 : 8))
                    .u2(stack)
                    .u2(locals)
                    .u4(body.size)
                    .raw(body.array());
            m.u2(handler == null ? 0 : 1);
            if (handler != null) {
                for (int n : handler) {
                    m.u2(n);
                }
            }
            m.u2(0);
            methods.add(m.array());
        }

        Class<?> define() {
            Bytes out = new Bytes().u4(0xCAFEBABE).u2(0).u2(49).u2(pool.items.size() + 1);
            for (byte[] entry : pool.items) {
                out.raw(entry);
            }
            out.u2(0x0031).u2(own).u2(parent).u2(0).u2(fields.size());
            for (int[] f : fields) {
                out.u2(0x0012).u2(f[0]).u2(f[1]).u2(0);
            }
            out.u2(methods.size());
            for (byte[] m : methods) {
                out.raw(m);
            }
            out.u2(0);
            return new Loader().define(out.array());
        }
    }

    private static final class Pool {
        final List<byte[]> items = new ArrayList<byte[]>();
        final Map<String, Integer> indices = new HashMap<String, Integer>();

        int add(String key, Bytes value) {
            Integer old = indices.get(key);
            if (old != null) {
                return old;
            }
            items.add(value.array());
            indices.put(key, items.size());
            return items.size();
        }

        int utf(String s) {
            return add("u" + s, new Bytes().op(1).utf(s));
        }

        int cls(String s) {
            return add("c" + s, new Bytes().op(7).u2(utf(s)));
        }

        int pair(int a, int b) {
            return ref(12, a, b);
        }

        int ref(int tag, int a, int b) {
            return add(tag + ":" + a + ":" + b, new Bytes().op(tag).u2(a).u2(b));
        }
    }

    private static final class Bytes {
        byte[] data = new byte[256];
        int size;

        Bytes op(int... values) {
            for (int v : values) {
                if (size == data.length) {
                    data = Arrays.copyOf(data, size * 2);
                }
                data[size++] = (byte) v;
            }
            return this;
        }

        Bytes u2(int n) {
            return op(n >>> 8, n);
        }

        Bytes u4(int n) {
            return op(n >>> 24, n >>> 16, n >>> 8, n);
        }

        Bytes ref(int op, int index) {
            return op(op).u2(index);
        }

        Bytes iface(int index, int count) {
            return ref(185, index).op(count, 0);
        }

        Bytes raw(byte[] bytes) {
            for (byte b : bytes) {
                op(b);
            }
            return this;
        }

        Bytes utf(String s) {
            int n = 0;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                n += c > 0 && c < 128 ? 1 : c < 2048 ? 2 : 3;
            }
            u2(n);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c > 0 && c < 128) {
                    op(c);
                } else if (c < 2048) {
                    op(0xc0 | (c >> 6), 0x80 | (c & 63));
                } else {
                    op(0xe0 | (c >> 12), 0x80 | ((c >> 6) & 63), 0x80 | (c & 63));
                }
            }
            return this;
        }

        byte[] array() {
            return Arrays.copyOf(data, size);
        }
    }
}
