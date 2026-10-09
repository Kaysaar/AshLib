package ashlib.data.plugins.coreui.campaignbar.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Accesses opaque JDK metadata through MethodHandles, avoiding restricted class references. */
public final class CampaignHandleAccess {
    private CampaignHandleAccess() {}

    private static Metadata metadata;
    // ClassValue caches metadata without retaining unloaded class loaders.
    private static final ClassValue<List<MethodRef>> METHOD_CACHE =
            new ClassValue<List<MethodRef>>() {
                protected List<MethodRef> computeValue(Class<?> type) {
                    try {
                        return Collections.unmodifiableList(scanMethods(type));
                    } catch (Exception error) {
                        throw new IllegalStateException(
                                "Method metadata: " + type.getName(), error);
                    }
                }
            };
    private static final ClassValue<List<FieldRef>> FIELD_CACHE =
            new ClassValue<List<FieldRef>>() {
                protected List<FieldRef> computeValue(Class<?> type) {
                    try {
                        return Collections.unmodifiableList(scanFields(type));
                    } catch (Exception error) {
                        throw new IllegalStateException("Field metadata: " + type.getName(), error);
                    }
                }
            };

    public static boolean isPublic(int flags) {
        return (flags & 0x0001) != 0;
    }

    public static boolean isStatic(int flags) {
        return (flags & 0x0008) != 0;
    }

    public static boolean isFinal(int flags) {
        return (flags & 0x0010) != 0;
    }

    public static boolean isAbstract(int flags) {
        return (flags & 0x0400) != 0;
    }

    private static synchronized Metadata metadata() throws Exception {
        if (metadata == null) {
            metadata = new Metadata();
        }
        return metadata;
    }

    private static Object call(MethodHandle handle, String operation, Object... args)
            throws Exception {
        try {
            return handle.invokeWithArguments(args);
        } catch (Throwable cause) {
            throw new Exception("Campaign capability: " + operation, cause);
        }
    }

    private static MethodHandle generic(MethodHandle handle) {
        return handle.asType(MethodType.genericMethodType(handle.type().parameterCount()));
    }

    private static final class Metadata {
        final MethodHandle methods,
                fields,
                methodName,
                methodOwner,
                methodResult,
                methodParams,
                methodFlags,
                methodBridge;
        final MethodHandle fieldName,
                fieldOwner,
                fieldType,
                fieldFlags,
                fieldAccessible,
                fieldValue,
                fieldSet;

        Metadata() throws Exception {
            // Resolve JDK metadata types through their bootstrap loader.
            ClassLoader bootstrap = Class.class.getClassLoader();
            Class<?> method = Class.forName("java.lang.reflect.Method", false, bootstrap);
            Class<?> field = Class.forName("java.lang.reflect.Field", false, bootstrap);
            Class<?> methodArray = Class.forName("[Ljava.lang.reflect.Method;", false, bootstrap);
            Class<?> fieldArray = Class.forName("[Ljava.lang.reflect.Field;", false, bootstrap);
            methods = virtual(Class.class, "getMethods", methodArray);
            fields = virtual(Class.class, "getDeclaredFields", fieldArray);
            methodName = virtual(method, "getName", String.class);
            methodOwner = virtual(method, "getDeclaringClass", Class.class);
            methodResult = virtual(method, "getReturnType", Class.class);
            methodParams = virtual(method, "getParameterTypes", Class[].class);
            methodFlags = virtual(method, "getModifiers", int.class);
            methodBridge = virtual(method, "isBridge", boolean.class);
            fieldName = virtual(field, "getName", String.class);
            fieldOwner = virtual(field, "getDeclaringClass", Class.class);
            fieldType = virtual(field, "getType", Class.class);
            fieldFlags = virtual(field, "getModifiers", int.class);
            fieldAccessible = generic(virtual(field, "setAccessible", void.class, boolean.class));
            fieldValue = generic(virtual(field, "get", Object.class, Object.class));
            fieldSet = generic(virtual(field, "set", void.class, Object.class, Object.class));
        }
    }

    private static MethodHandle virtual(
            Class<?> owner, String name, Class<?> result, Class<?>... args) throws Exception {
        // Caller-sensitive metadata access requires the adapter's own lookup.
        return MethodHandles.lookup().findVirtual(owner, name, MethodType.methodType(result, args));
    }

    public static List<MethodRef> methods(Class<?> type) throws Exception {
        return METHOD_CACHE.get(type);
    }

    private static List<MethodRef> scanMethods(Class<?> type) throws Exception {
        Metadata m = metadata();
        Object[] values = (Object[]) call(m.methods, type.getName() + ".getMethods", type);
        List<MethodRef> result = new ArrayList<MethodRef>(values.length);
        for (Object value : values) {
            result.add(new MethodRef(value, m));
        }
        return result;
    }

    public static List<FieldRef> declaredFields(Class<?> type) throws Exception {
        return FIELD_CACHE.get(type);
    }

    private static List<FieldRef> scanFields(Class<?> type) throws Exception {
        Metadata m = metadata();
        Object[] values = (Object[]) call(m.fields, type.getName() + ".getDeclaredFields", type);
        List<FieldRef> result = new ArrayList<FieldRef>(values.length);
        for (Object value : values) {
            result.add(new FieldRef(value, m));
        }
        return result;
    }

    public static final class MethodRef {
        private final String name;
        private final Class<?> owner, result;
        private final Class<?>[] parameters;
        private final int flags;
        private final boolean bridge;
        private final String operation;
        private MethodHandle handle;
        private MethodHandle exactHandle;

        MethodRef(Object descriptor, Metadata m) throws Exception {
            name = (String) call(m.methodName, "method.name", descriptor);
            owner = (Class<?>) call(m.methodOwner, "method.owner", descriptor);
            result = (Class<?>) call(m.methodResult, "method.returnType", descriptor);
            parameters = (Class<?>[]) call(m.methodParams, "method.parameters", descriptor);
            flags = (Integer) call(m.methodFlags, "method.modifiers", descriptor);
            bridge = (Boolean) call(m.methodBridge, "method.bridge", descriptor);
            operation = owner.getName() + "." + name;
        }

        public String getName() {
            return name;
        }

        public Class<?> getDeclaringClass() {
            return owner;
        }

        public Class<?> getReturnType() {
            return result;
        }

        public Class<?>[] getParameterTypes() {
            return parameters.clone();
        }

        boolean matchesParameters(Class<?>[] args) {
            return Arrays.equals(parameters, args);
        }

        Class<?> parameterType(int index) {
            return parameters[index];
        }

        public int getModifiers() {
            return flags;
        }

        public boolean isBridge() {
            return bridge;
        }

        public Object invoke(Object receiver, Object... args) throws Exception {
            if (isStatic(flags)) {
                throw new IllegalStateException("UI adapter refuses static invocation: " + name);
            }
            if (handle == null) {
                handle = virtual(owner, name, result, parameters);
            }
            // Keep variable-arity and uncommon signatures on the original invocation path.
            if (args.length == parameters.length
                    && args.length <= 4
                    && !handle.isVarargsCollector()) {
                try {
                    if (exactHandle == null) {
                        exactHandle = generic(handle);
                    }
                    switch (args.length) {
                        case 0:
                            return (Object) exactHandle.invokeExact(receiver);
                        case 1:
                            return (Object) exactHandle.invokeExact(receiver, args[0]);
                        case 2:
                            return (Object) exactHandle.invokeExact(receiver, args[0], args[1]);
                        case 3:
                            return (Object) exactHandle.invokeExact(receiver, args[0], args[1], args[2]);
                        default:
                            return (Object) exactHandle.invokeExact(
                                    receiver, args[0], args[1], args[2], args[3]);
                    }
                } catch (Throwable cause) {
                    throw new Exception("Campaign capability: " + operation, cause);
                }
            }
            Object[] values = new Object[args.length + 1];
            values[0] = receiver;
            System.arraycopy(args, 0, values, 1, args.length);
            return call(handle, operation, values);
        }
    }

    public static final class FieldRef {
        private final Object descriptor;
        private final String name;
        private final Class<?> owner, type;
        private final int flags;
        private final String readAccessOperation, readOperation, writeAccessOperation, writeOperation;

        FieldRef(Object descriptor, Metadata m) throws Exception {
            this.descriptor = descriptor;
            name = (String) call(m.fieldName, "field.name", descriptor);
            owner = (Class<?>) call(m.fieldOwner, "field.owner", descriptor);
            type = (Class<?>) call(m.fieldType, "field.type", descriptor);
            flags = (Integer) call(m.fieldFlags, "field.modifiers", descriptor);
            String qualifiedName = owner.getName() + "." + name;
            readAccessOperation = qualifiedName + ".readAccess";
            readOperation = qualifiedName + ".get";
            writeAccessOperation = qualifiedName + ".writeAccess";
            writeOperation = qualifiedName + ".set";
        }

        public String getName() {
            return name;
        }

        public Class<?> getType() {
            return type;
        }

        public Class<?> getDeclaringClass() {
            return owner;
        }

        public int getModifiers() {
            return flags;
        }

        public Object read(Object target) throws Exception {
            if (isStatic(flags)) {
                throw new IllegalStateException("UI adapter refuses static field access: " + name);
            }
            Metadata m = metadata();
            // Access applies only to this field descriptor.
            makeAccessible(m, readAccessOperation);
            try {
                return (Object) m.fieldValue.invokeExact(descriptor, target);
            } catch (Throwable cause) {
                throw new Exception("Campaign capability: " + readOperation, cause);
            }
        }

        /** Internal transaction write; excluded from the public read-only adapter. */
        void write(Object target, Object value) throws Exception {
            if (isStatic(flags) || isFinal(flags)) {
                throw new IllegalStateException(
                        "Campaign Bar refuses static/final mutation: " + name);
            }
            Metadata m = metadata();
            makeAccessible(m, writeAccessOperation);
            try {
                Object ignored = (Object) m.fieldSet.invokeExact(descriptor, target, value);
            } catch (Throwable cause) {
                throw new Exception("Campaign capability: " + writeOperation, cause);
            }
        }

        private void makeAccessible(Metadata m, String operation) throws Exception {
            try {
                Object ignored =
                        (Object) m.fieldAccessible.invokeExact(descriptor, (Object) Boolean.TRUE);
            } catch (Throwable cause) {
                throw new Exception("Campaign capability: " + operation, cause);
            }
        }
    }
}
