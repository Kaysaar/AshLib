package ashlib.data.plugins.coreui.campaignbar.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/** Generates native host and listener subclasses from discovered runtime types. */
public final class CampaignRuntimeBridge {
    private CampaignRuntimeBridge() {}

    /** Creates an action listener for the callback type exposed by native addButton. */
    public static Object actionListener(Class<?> contract, Runnable action) throws Exception {
        if (!contract.isInterface() || !CampaignHandleAccess.isPublic(contract.getModifiers())) {
            throw new IllegalArgumentException("Non-public action contract");
        }
        int count = 0;
        for (CampaignHandleAccess.MethodRef method : CampaignHandleAccess.methods(contract)) {
            if (!CampaignHandleAccess.isAbstract(method.getModifiers())) {
                continue;
            }
            count++;
            if (!method.getName().equals("actionPerformed")
                    || method.getReturnType() != void.class
                    || !Arrays.equals(
                            method.getParameterTypes(),
                            new Class<?>[] {Object.class, Object.class})) {
                throw new IllegalArgumentException("Unknown action contract");
            }
        }
        if (count != 1) {
            throw new IllegalArgumentException("Ambiguous action contract");
        }
        ClassLoader parent = CampaignRuntimeBridge.class.getClassLoader();
        if (Class.forName(contract.getName(), false, parent) != contract) {
            throw new IllegalStateException("Action type identity mismatch");
        }
        Bytes out = new Bytes();
        out.writeInt(0xCAFEBABE);
        out.writeShort(0);
        out.writeShort(49);
        out.writeShort(24);
        utf(out, "ashlib/data/plugins/coreui/campaignbar/impl/generated/ExperimentalBarAction");
        cls(out, 1);
        utf(out, "java/lang/Object");
        cls(out, 3);
        utf(out, contract.getName().replace('.', '/'));
        cls(out, 5);
        utf(out, "action");
        utf(out, "Ljava/lang/Runnable;");
        pair(out, 12, 7, 8);
        pair(out, 9, 2, 9);
        utf(out, "<init>");
        utf(out, "()V");
        pair(out, 12, 11, 12);
        pair(out, 10, 4, 13);
        utf(out, "(Ljava/lang/Runnable;)V");
        utf(out, "Code");
        utf(out, "actionPerformed");
        utf(out, "(Ljava/lang/Object;Ljava/lang/Object;)V");
        utf(out, "java/lang/Runnable");
        cls(out, 19);
        utf(out, "run");
        pair(out, 12, 21, 12);
        pair(out, 11, 20, 22);
        out.writeShort(0x0031);
        out.writeShort(2);
        out.writeShort(4);
        out.writeShort(1);
        out.writeShort(6);
        out.writeShort(1);
        out.writeShort(0x0012);
        out.writeShort(7);
        out.writeShort(8);
        out.writeShort(0);
        out.writeShort(2);
        code(
                out,
                11,
                15,
                2,
                2,
                new byte[] {42, (byte) 183, 0, 14, 42, 43, (byte) 181, 0, 10, (byte) 177});
        code(
                out,
                17,
                18,
                1,
                3,
                new byte[] {42, (byte) 180, 0, 10, (byte) 185, 0, 23, 1, 0, (byte) 177});
        out.writeShort(0);
        Class<?> generated = new BridgeLoader(parent).load(out.toByteArray());
        try {
            return MethodHandles.publicLookup()
                    .findConstructor(generated, MethodType.methodType(void.class, Runnable.class))
                    .invokeWithArguments(action);
        } catch (Throwable error) {
            throw new Exception("Native action bridge", error);
        }
    }

    public static MethodHandle prepare(Class<?> base, Class<?> contract) throws Exception {
        if (!CampaignHandleAccess.isPublic(base.getModifiers())
                || CampaignHandleAccess.isFinal(base.getModifiers())
                || CampaignHandleAccess.isAbstract(base.getModifiers())
                || !CampaignUiAccess.validContract(contract)) {
            throw new IllegalArgumentException("Unsupported host types");
        }
        MethodHandles.publicLookup()
                .findConstructor(base, MethodType.methodType(void.class, float.class, float.class));
        ClassLoader parent = CampaignRuntimeBridge.class.getClassLoader();
        if (Class.forName(base.getName(), false, parent) != base
                || Class.forName(contract.getName(), false, parent) != contract) {
            throw new IllegalStateException("Host types not visible to mod classloader");
        }
        byte[] bytes = emit(base.getName().replace('.', '/'), contract.getName().replace('.', '/'));
        Class<?> generated = new BridgeLoader(parent).load(bytes);
        if (!base.isAssignableFrom(generated) || !contract.isAssignableFrom(generated)) {
            throw new IllegalStateException("Invalid bridge types");
        }
        return MethodHandles.publicLookup()
                .findConstructor(
                        generated,
                        MethodType.methodType(
                                void.class, float.class, float.class, Runnable.class));
    }

    private static final class BridgeLoader extends ClassLoader {
        BridgeLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> load(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }

    private static byte[] emit(String base, String contract) throws Exception {
        Bytes out = new Bytes();
        out.writeInt(0xCAFEBABE);
        out.writeShort(0);
        out.writeShort(49);
        out.writeShort(24);
        utf(out, "ashlib/data/plugins/coreui/campaignbar/impl/generated/ExperimentalCoreHost");
        cls(out, 1);
        utf(out, base);
        cls(out, 3);
        utf(out, contract);
        cls(out, 5);
        utf(out, "closed");
        utf(out, "Ljava/lang/Runnable;");
        pair(out, 12, 7, 8);
        pair(out, 9, 2, 9);
        utf(out, "<init>");
        utf(out, "(FF)V");
        pair(out, 12, 11, 12);
        pair(out, 10, 4, 13);
        utf(out, "(FFLjava/lang/Runnable;)V");
        utf(out, "Code");
        utf(out, "tabClosed");
        utf(out, "()V");
        utf(out, "java/lang/Runnable");
        cls(out, 19);
        utf(out, "run");
        pair(out, 12, 21, 18);
        pair(out, 11, 20, 22);
        out.writeShort(0x0031);
        out.writeShort(2);
        out.writeShort(4);
        out.writeShort(1);
        out.writeShort(6);
        out.writeShort(1);
        out.writeShort(0x0012);
        out.writeShort(7);
        out.writeShort(8);
        out.writeShort(0);
        out.writeShort(2);
        code(
                out,
                11,
                15,
                3,
                4,
                new byte[] {42, 35, 36, (byte) 183, 0, 14, 42, 45, (byte) 181, 0, 10, (byte) 177});
        code(
                out,
                17,
                18,
                1,
                1,
                new byte[] {42, (byte) 180, 0, 10, (byte) 185, 0, 23, 1, 0, (byte) 177});
        out.writeShort(0);
        return out.toByteArray();
    }

    private static void utf(Bytes out, String text) throws Exception {
        out.writeByte(1);
        out.writeUTF(text);
    }

    private static void cls(Bytes out, int name) throws Exception {
        out.writeByte(7);
        out.writeShort(name);
    }

    private static void pair(Bytes out, int tag, int a, int b) throws Exception {
        out.writeByte(tag);
        out.writeShort(a);
        out.writeShort(b);
    }

    private static void code(
            Bytes out, int name, int descriptor, int stack, int locals, byte[] body)
            throws Exception {
        out.writeShort(1);
        out.writeShort(name);
        out.writeShort(descriptor);
        out.writeShort(1);
        out.writeShort(16);
        out.writeInt(12 + body.length);
        out.writeShort(stack);
        out.writeShort(locals);
        out.writeInt(body.length);
        out.write(body);
        out.writeShort(0);
        out.writeShort(0);
    }

    /** Writes classfile bytes in memory without restricted java.io types. */
    private static final class Bytes {
        private byte[] data = new byte[1024];
        private int size;

        void writeByte(int value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = (byte) value;
        }

        void writeShort(int value) {
            writeByte(value >>> 8);
            writeByte(value);
        }

        void writeInt(int value) {
            writeShort(value >>> 16);
            writeShort(value);
        }

        void write(byte[] values) {
            for (byte value : values) {
                writeByte(value);
            }
        }

        void writeUTF(String value) {
            int length = 0;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                length += c > 0 && c < 128 ? 1 : c < 2048 ? 2 : 3;
            }
            if (length > 65535) {
                throw new IllegalArgumentException("Classfile UTF entry too long");
            }
            writeShort(length);
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c > 0 && c < 128) {
                    writeByte(c);
                } else if (c < 2048) {
                    writeByte(0xc0 | (c >> 6));
                    writeByte(0x80 | (c & 63));
                } else {
                    writeByte(0xe0 | (c >> 12));
                    writeByte(0x80 | ((c >> 6) & 63));
                    writeByte(0x80 | (c & 63));
                }
            }
        }

        byte[] toByteArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
