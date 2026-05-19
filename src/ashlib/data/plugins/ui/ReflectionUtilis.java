package ashlib.data.plugins.ui;

import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

 class ReflectionUtilis {
    // Code taken and modified from Grand Colonies
    private static final Class<?> fieldClass;
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final MethodHandle setFieldHandle;
    private static final MethodHandle getFieldHandle;
    private static final MethodHandle getFieldNameHandle;
    private static final MethodHandle setFieldAccessibleHandle;
    private static final Class<?> methodClass;
    private static final MethodHandle getMethodNameHandle;
    private static final MethodHandle invokeMethodHandle;
    private static final MethodHandle setMethodAccessable;
    private static final MethodHandle getModifiersHandle;
    private static final MethodHandle  getParameterTypesHandle;
    private static final MethodHandle  getFieldTypeHandle;
    private static final Class<?>  fileclass;
    private static final Class<?>  fileWriterClass;
    private static final Class<?> fileReaderClass;
    private static final Class<?> bufferedReaderClass;
    private static final Class<?> readerClass;

    static {
        try {
            fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());
            setFieldHandle = lookup.findVirtual(fieldClass, "set", MethodType.methodType(Void.TYPE, Object.class, Object.class));
            getFieldHandle = lookup.findVirtual(fieldClass, "get", MethodType.methodType(Object.class, Object.class));
            getFieldNameHandle = lookup.findVirtual(fieldClass, "getName", MethodType.methodType(String.class));
            getFieldTypeHandle = lookup.findVirtual(fieldClass, "getType", MethodType.methodType(Class.class));
            setFieldAccessibleHandle = lookup.findVirtual(fieldClass, "setAccessible", MethodType.methodType(Void.TYPE, boolean.class));

            methodClass = Class.forName("java.lang.reflect.Method", false, Class.class.getClassLoader());
            getMethodNameHandle = lookup.findVirtual(methodClass, "getName", MethodType.methodType(String.class));
            invokeMethodHandle = lookup.findVirtual(methodClass, "invoke", MethodType.methodType(Object.class, Object.class, Object[].class));
            setMethodAccessable = lookup.findVirtual(methodClass, "setAccessible", MethodType.methodType(Void.TYPE, boolean.class));
            getModifiersHandle = lookup.findVirtual(methodClass, "getModifiers", MethodType.methodType(int.class));
            getParameterTypesHandle = lookup.findVirtual(methodClass, "getParameterTypes", MethodType.methodType(Class[].class));
            fileclass = Class.forName("java.io.File",false,Class.class.getClassLoader());
            fileWriterClass = Class.forName("java.io.FileWriter",false,Class.class.getClassLoader());
            fileReaderClass = Class.forName("java.io.FileReader", false, Class.class.getClassLoader());
            bufferedReaderClass = Class.forName("java.io.BufferedReader", false, Class.class.getClassLoader());
            readerClass = Class.forName("java.io.Reader", false, Class.class.getClassLoader());


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getFile(String pathAbsolute){
        try {
            MethodHandle mh = lookup.findConstructor(fileclass, MethodType.methodType(Void.TYPE, String.class));
            Object fileObj = mh.invoke(pathAbsolute);
            return fileObj;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }
    public static Object getFileWriter(String pathAbsolute,boolean append){
        try {
            MethodHandle mh = lookup.findConstructor(fileWriterClass, MethodType.methodType(Void.TYPE, String.class, boolean.class));
            Object fileObj = mh.invoke(pathAbsolute,append);
            return fileObj;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }
    public static Object getFileReader(String pathAbsolute) {
        try {
            MethodHandle ctor = lookup.findConstructor(fileReaderClass,
                    MethodType.methodType(Void.TYPE, String.class));
            return ctor.invoke(pathAbsolute);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getBufferedReader(Object reader) {
        try {
            MethodHandle ctor = lookup.findConstructor(bufferedReaderClass,
                    MethodType.methodType(Void.TYPE, readerClass));
            return ctor.invoke(reader);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // Generic close() for any Closeable/Reader/Writer
    public static void closeQuiet(Object closeable) {
        try {
            invokeMethodWithAutoProjection("close", closeable);
        } catch (Throwable ignored) {}
    }
    public static JSONArray readCsvAsJsonArrayReflect(String csvPath)throws Exception {
        JSONArray array = new JSONArray();

        Object fr = null, br = null;
        try {
            fr = ReflectionUtilis.getFileReader(csvPath);
            br = ReflectionUtilis.getBufferedReader(fr);

            List<String> headers = null;
            while (true) {
                Object lineObj = ReflectionUtilis.invokeMethodWithAutoProjection("readLine", br);
                if (lineObj == null) break; // EOF
                String line = (String) lineObj;
                if (line.isEmpty() || line.startsWith("#")) continue;

                List<String> cells = parseCsvLine(line);
                if (headers == null) {
                    headers = cells;
                    continue;
                }

                JSONObject row = new JSONObject();
                for (int i = 0; i < headers.size(); i++) {
                    String key = headers.get(i);
                    String val = (i < cells.size()) ? cells.get(i) : "";
                    row.put(key, val);
                }
                array.put(row);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed reading CSV: " + csvPath, e);
        } finally {
            ReflectionUtilis.closeQuiet(br);
            ReflectionUtilis.closeQuiet(fr);
        }

        return array;
    }
    private static List<String> parseCsvLine(String line) {
        ArrayList<String> out = new ArrayList<>();
        if (line == null) return out;
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        int i = 0, n = line.length();
        while (i < n) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && line.charAt(i + 1) == '"') {
                        cell.append('"');
                        i += 2;
                    } else {
                        inQuotes = false;
                        i++;
                    }
                } else {
                    cell.append(c);
                    i++;
                }
            } else {
                if (c == ',') {
                    out.add(cell.toString());
                    cell.setLength(0);
                    i++;
                } else if (c == '"') {
                    inQuotes = true;
                    i++;
                } else {
                    cell.append(c);
                    i++;
                }
            }
        }
        out.add(cell.toString());
        return out;
    }

    public static boolean createDirectory(String pathAbsolute) {
        try {
            // Create a java.io.File instance via MethodHandle
            MethodHandle fileCtor = lookup.findConstructor(fileclass,
                    MethodType.methodType(Void.TYPE, String.class));
            Object fileObj = fileCtor.invoke(pathAbsolute);

            // Get the mkdirs() method handle
            MethodHandle mkdirsMH = lookup.findVirtual(fileclass, "mkdirs",
                    MethodType.methodType(boolean.class));

            // Call mkdirs() to create the directory (and parents)
            return (boolean) mkdirsMH.invoke(fileObj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getPrivateVariable(String fieldName, Object instanceToGetFrom) {
        try {
            Class<?> instances = instanceToGetFrom.getClass();
            while (instances != null) {
                for (Object obj : instances.getDeclaredFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        return getFieldHandle.invoke(obj, instanceToGetFrom);
                    }
                }
                for (Object obj : instances.getFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        return getFieldHandle.invoke(obj, instanceToGetFrom);
                    }
                }
                instances = instances.getSuperclass();
            }
            return null;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getPrivateField(String fieldName, Object instanceToGetFrom) {
        try {
            Class<?> instances = instanceToGetFrom.getClass();
            while (instances != null) {
                for (Object obj : instances.getDeclaredFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        return obj;
                    }
                }
                for (Object obj : instances.getFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        return obj;
                    }
                }
                instances = instances.getSuperclass();
            }
            return null;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static Object getPrivateVariableFromSuperClass(String fieldName, Object instanceToGetFrom) {
        try {
            Class<?> instances = instanceToGetFrom.getClass();
            while (instances != null) {
                for (Object obj : instances.getDeclaredFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        return getFieldHandle.invoke(obj, instanceToGetFrom);
                    }
                }
                for (Object obj : instances.getFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        return getFieldHandle.invoke(obj, instanceToGetFrom);
                    }
                }
                instances = instances.getSuperclass();
            }
            return null;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPrivateVariableFromSuperclass(String fieldName, Object instanceToModify, Object newValue) {
        try {
            Class<?> instances = instanceToModify.getClass();
            while (instances != null) {
                for (Object obj : instances.getDeclaredFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        setFieldHandle.invoke(obj, instanceToModify, newValue);
                        return;
                    }
                }
                for (Object obj : instances.getFields()) {
                    setFieldAccessibleHandle.invoke(obj, true);
                    String name = (String) getFieldNameHandle.invoke(obj);
                    if (name.equals(fieldName)) {
                        setFieldHandle.invoke(obj, instanceToModify, newValue);
                        return;
                    }
                }
                instances = instances.getSuperclass();
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasMethodOfName(String name, Object instance) {
        try {
            for (Object method : instance.getClass().getMethods()) {
                if (getMethodNameHandle.invoke(method).equals(name)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    public static Object invokeMethod(String methodName, Object instance, Object... arguments) {
        try {
            Object method = instance.getClass().getMethod(methodName);
            return invokeMethodHandle.invoke(method, instance, arguments);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static Object invokeMethodDirectly(Object method,Object instance, Object... arguments) {
        try {

            return invokeMethodHandle.invoke(method,null, arguments);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static List<UIComponentAPI> getChildrenCopy(UIPanelAPI panel) {
        try {
            return (List<UIComponentAPI>) invokeMethod("getChildrenCopy", panel);
        } catch (Throwable e) {
            return null;
        }
    }

    public static Pair<Object, Class<?>[]> getMethodFromSuperclass(String methodName, Object instance, Object... arguments) {
        Class<?> currentClass = instance.getClass();

        while (currentClass != null) {
            Object[] methods = currentClass.getDeclaredMethods();

            for (Object method : methods) {
                try {
                    // Get method name
                    String currentName = (String) getMethodNameHandle.invoke(method);
                    if (!currentName.equals(methodName)) continue;

                    // Get parameter types
                    Class<?>[] parameterTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);

                    // Check if argument types match
                    if (areArgumentsCompatible(parameterTypes, arguments)) {
                        return new Pair<>(method, parameterTypes);
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            currentClass = currentClass.getSuperclass();
        }

        return null;
    }
    private static boolean areArgumentsCompatible(Class<?>[] parameterTypes, Object[] arguments) {
        if (parameterTypes.length != arguments.length) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = arguments[i];

            if (arg == null) {
                if (parameterTypes[i].isPrimitive()) return false;
            } else {
                if (!wrap(parameterTypes[i]).isAssignableFrom(arg.getClass())) return false;
            }
        }

        return true;
    }
    private static Class<?> wrap(Class<?> clazz) {
        if (!clazz.isPrimitive()) return clazz;

        if (clazz == int.class) return Integer.class;
        if (clazz == long.class) return Long.class;
        if (clazz == double.class) return Double.class;
        if (clazz == float.class) return Float.class;
        if (clazz == boolean.class) return Boolean.class;
        if (clazz == char.class) return Character.class;
        if (clazz == byte.class) return Byte.class;
        if (clazz == short.class) return Short.class;
        if (clazz == void.class) return Void.class;

        return clazz; // fallback
    }
    public static Object invokeStaticExact(Class<?> targetClass, String methodName,
                                           Class<?>[] paramTypes, Object... args) {
        try {
            Object method = targetClass.getDeclaredMethod(methodName, paramTypes);
            setMethodAccessable.invoke(method, true);
            return invokeMethodHandle.invoke(method, null, args);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeStaticMethodWithAutoProjection(Class<?> targetClass, String methodName, Object... arguments) {
        try {
            // Find the method by its name and parameter types
            Object[] methods = targetClass.getDeclaredMethods();

            Object matchingMethod = null;
            Class<?>[] parameterTypes = null;

            for (Object method : methods) {
                // Get the method name dynamically
                String currentName = (String) getMethodNameHandle.invoke(method);

                // Check if names match and method is static
                int modifiers = (int) getModifiersHandle.invoke(method);
                if (currentName.equals(methodName) && (modifiers & 0x0008) != 0) { // Static check
                    // Retrieve parameter types
                    parameterTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                    if(parameterTypes.length== arguments.length){
                        matchingMethod = method;
                        break;
                    }

                }
            }

            if (matchingMethod == null) {
                throw new NoSuchMethodException("Static method " + methodName + " not found in class " + targetClass.getName());
            }

            // Project arguments to the correct types
            Object[] projectedArgs = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                Object arg = (arguments.length > i) ? arguments[i] : null;

                if (arg == null) {
                    if (parameterTypes[i].isPrimitive()) {
                        throw new IllegalArgumentException("Null cannot be used for primitive type: " + parameterTypes[i].getName());
                    }
                    projectedArgs[i] = null;
                } else {
                    projectedArgs[i] = convertArgument(arg, parameterTypes[i]);
                }
            }

            // Ensure the method is accessible
            setMethodAccessable.invoke(matchingMethod, true);

            // Invoke the static method (pass null as the instance for static methods)
            return invokeMethodHandle.invoke(matchingMethod, null, projectedArgs);
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                Throwable cause = ((InvocationTargetException) e).getTargetException();
                System.err.println("Root cause of InvocationTargetException: " + cause.getClass().getName());
                cause.printStackTrace(); // Print root cause
            } else {
                e.printStackTrace();
            }
            throw new RuntimeException(e);
        }
    }

    public static Object invokeMethodWithAutoProjection(String methodName, Object instance, Object... arguments) {
        // Retrieve the method and its parameter types
        Pair<Object, Class<?>[]> methodPair = getMethodFromSuperclass(methodName, instance,arguments);

        // Check if the method was found
        if (methodPair == null) {
            try {
                throw new NoSuchMethodException("Method " + methodName + " not found in class hierarchy of " + instance.getClass().getName());
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        Object method = methodPair.one;
        Class<?>[] parameterTypes = methodPair.two;

        // Prepare arguments by projecting them to the correct types
        Object[] projectedArgs = new Object[parameterTypes.length];
        for (int index = 0; index < parameterTypes.length; index++) {
            Object arg = (arguments.length > index) ? arguments[index] : null;

            if (arg == null) {
                // If the expected type is a primitive type, throw an exception
                if (parameterTypes[index].isPrimitive()) {
                    throw new IllegalArgumentException("Argument at index " + index + " cannot be null for primitive type " + parameterTypes[index].getName());
                }
                projectedArgs[index] = null; // Keep nulls as null for reference types
            } else {
                // Try to convert the argument to the expected parameter type
                try {
                    projectedArgs[index] = convertArgument(arg, parameterTypes[index]);
                } catch (Exception e) {
                    continue;
                }
            }
        }

        // Ensure the method is accessible
        try {
            setMethodAccessable.invoke(method, true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        // Invoke the method with the projected arguments
        try {
            return invokeMethodHandle.invoke(method, instance, projectedArgs);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // Helper function to convert an argument to the expected type
    public static Object convertArgument(Object arg, Class<?> targetType) {
        if (targetType.isAssignableFrom(arg.getClass())) {
            return arg; // Use as-is if types match
        } else if (targetType.isPrimitive()) {
            // Handle primitive types by boxing
            if (targetType == int.class) {
                return ((Number) arg).intValue();
            } else if (targetType == long.class) {
                return ((Number) arg).longValue();
            } else if (targetType == double.class) {
                return ((Number) arg).doubleValue();
            } else if (targetType == float.class) {
                return ((Number) arg).floatValue();
            } else if (targetType == short.class) {
                return ((Number) arg).shortValue();
            } else if (targetType == byte.class) {
                return ((Number) arg).byteValue();
            } else if (targetType == boolean.class) {
                return arg;
            } else if (targetType == char.class) {
                return arg;
            } else {
                throw new IllegalArgumentException("Unsupported primitive type: " + targetType.getName());
            }
        } else {
            // For reference types, perform a cast if possible
            return targetType.cast(arg);
        }
    }
    public static Object invokeStaticMethod(Class<?> targetClass, String methodName, Object... arguments) {
        try {
            // Retrieve the parameter types of the arguments
            Class<?>[] parameterTypes = new Class[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                parameterTypes[i] = arguments[i].getClass();
            }

            // Find the method by its name and parameter types
            Object method = findStaticMethodByParameterTypes(targetClass, parameterTypes);
            if (method == null) {
                throw new NoSuchMethodException("Static method " + methodName + " not found in class " + targetClass.getName());
            }

            // Invoke the method (static methods do not need an instance)
            return invokeMethodHandle.invoke(method, null, arguments);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static Object findFieldByType(Object targetObject, Class<?> fieldType) {
        try {
            Class<?> currentClass = targetObject.getClass();

            while (currentClass != null) {
                // Retrieve all declared fields dynamically
                Object[] fields = currentClass.getDeclaredFields();

                for (Object field : fields) {
                    try {
                        // Retrieve field type dynamically
                        Class<?> fieldClass = (Class<?>) invokeMethodWithAutoProjection("getType",field);

                        // Check if the field type matches or is assignable
                        if (fieldClass.isAssignableFrom(fieldType)) {
                            setFieldAccessibleHandle.invoke(field, true);
                            String name = (String) getFieldNameHandle.invoke(field);
                            return  getFieldHandle.invoke(field, targetObject);
                        }
                    } catch (Throwable e) {
                        // Handle exceptions gracefully during field inspection
                        e.printStackTrace();
                    }
                }

                // Move to the superclass dynamically
                currentClass = (Class<?>) invokeMethodHandle.invoke(currentClass, "getSuperclass");
            }

            // Return null if no matching field is found
            return null;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static Object findStaticMethodByParameterTypes(Class<?> targetClass, Class<?>... parameterTypes) {
        try {
            Class<?> currentClass = targetClass;

            while (currentClass != null) {
                // Retrieve all declared methods dynamically
                Object[] methods = currentClass.getDeclaredMethods();

                for (Object method : methods) {
                    try {
                        // Retrieve method modifiers dynamically
                        int modifiers = (int) getModifiersHandle.invoke(method);

                        // Check if the method is static
                        if ((modifiers & 0x0008) != 0) { // 0x0008 is the `static` modifier bit
                            // Retrieve parameter types dynamically
                            Class<?>[] methodParamTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);

                            // Compare parameter types
                            if (areParameterTypesMatching(methodParamTypes, parameterTypes)) {
                                return method; // Return the matching method
                            }
                        }
                    } catch (Throwable e) {
                        // Handle exceptions gracefully during method inspection
                        e.printStackTrace();
                    }
                }

                // Move to the superclass dynamically
                currentClass = (Class<?>) invokeMethodHandle.invoke(currentClass, "getSuperclass");
            }

            // Return null if no matching method is found
            return null;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }




    // Helper function to compare parameter types
    private static boolean areParameterTypesMatching(Class<?>[] methodParamTypes, Class<?>[] targetParamTypes) {
        if (methodParamTypes.length != targetParamTypes.length) {
            return false;
        }

        for (int i = 0; i < methodParamTypes.length; i++) {
            if (!methodParamTypes[i].isAssignableFrom(targetParamTypes[i])) {
                return false;
            }
        }

        return true;
    }


    public static List<UIComponentAPI> getChildren(UIPanelAPI panelAPI) {
        return ReflectionUtilis.getChildrenCopy(panelAPI);
    }
}