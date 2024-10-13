package ashlib.data.plugins.reflection

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.loading.SkillSpec
import com.fs.starfarer.rpg.Person
import ashlib.data.plugins.reflection.ReflectionBetterUtilis
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class ReflectionUtilis {
    //Code taken and modified from Grand Colonies
    companion object {
        private val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
        private val setFieldHandle = MethodHandles.lookup()
            .findVirtual(fieldClass, "set", MethodType.methodType(Void.TYPE, Any::class.java, Any::class.java))
        private val getFieldHandle = MethodHandles.lookup()
            .findVirtual(fieldClass, "get", MethodType.methodType(Any::class.java, Any::class.java))
        private val getFieldNameHandle =
            MethodHandles.lookup().findVirtual(fieldClass, "getName", MethodType.methodType(String::class.java))
        private val setFieldAccessibleHandle = MethodHandles.lookup().findVirtual(
            fieldClass,
            "setAccessible",
            MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType)
        )
        private val methodClass = Class.forName("java.lang.reflect.Method", false, Class::class.java.classLoader)
        private val getMethodNameHandle =
            MethodHandles.lookup().findVirtual(methodClass, "getName", MethodType.methodType(String::class.java))
        private val invokeMethodHandle = MethodHandles.lookup().findVirtual(
            methodClass,
            "invoke",
            MethodType.methodType(Any::class.java, Any::class.java, Array<Any>::class.java)
        )
        private val isAssignableFromHandle = MethodHandles.lookup().findVirtual(
            Class::class.java,
            "isAssignableFrom",
            MethodType.methodType(Boolean::class.javaPrimitiveType, Class::class.java)
        )
        private val getFieldTypeHandle = MethodHandles.lookup()
            .findVirtual(fieldClass, "getType", MethodType.methodType(Class::class.java)) // Changed return type to Any

        /**
         * Extracts the type of a given field using MethodHandles
         * @param field The field object to get the type from
         * @return The class type of the field
         */
        @JvmStatic
        fun getFieldType(field: Any): Any { // Adjusted to return Any
            return getFieldTypeHandle.invoke(field) as Any
        }

        @JvmStatic
        fun setPrivateVariable(fieldName: String, instanceToModify: Any, newValue: Any?) {

            val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
            val setMethod = MethodHandles.lookup()
                .findVirtual(fieldClass, "set", MethodType.methodType(Void.TYPE, Any::class.java, Any::class.java))
            val getNameMethod =
                MethodHandles.lookup().findVirtual(fieldClass, "getName", MethodType.methodType(String::class.java))
            val setAcessMethod = MethodHandles.lookup().findVirtual(
                fieldClass,
                "setAccessible",
                MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType)
            )

            val instancesOfFields: Array<out Any> = instanceToModify.javaClass.getDeclaredFields()
            for (obj in instancesOfFields) {
                setAcessMethod.invoke(obj, true)
                val name = getNameMethod.invoke(obj)
                if (name.toString() == fieldName) {
                    setMethod.invoke(obj, instanceToModify, newValue)
                }
            }
        }

        @JvmStatic
        fun getPrivateVariable(fieldName: String, instanceToGetFrom: Any): Any? {
            val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
            val getMethod = MethodHandles.lookup()
                .findVirtual(fieldClass, "get", MethodType.methodType(Any::class.java, Any::class.java))
            val getNameMethod =
                MethodHandles.lookup().findVirtual(fieldClass, "getName", MethodType.methodType(String::class.java))
            val setAcessMethod = MethodHandles.lookup().findVirtual(
                fieldClass,
                "setAccessible",
                MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType)
            )

            val instancesOfFields: Array<out Any> = instanceToGetFrom.javaClass.declaredFields
            for (obj in instancesOfFields) {
                setAcessMethod.invoke(obj, true)
                val name = getNameMethod.invoke(obj)
                if (name.toString() == fieldName) {
                    return getMethod.invoke(obj, instanceToGetFrom)
                }
            }
            val instancesOfFields2: Array<out Any> = instanceToGetFrom.javaClass.fields
            for (obj in instancesOfFields2) {
                setAcessMethod.invoke(obj, true)
                val name = getNameMethod.invoke(obj)
                if (name.toString() == fieldName) {
                    return getMethod.invoke(obj, instanceToGetFrom)
                }
            }
            return null
        }

        @JvmStatic
        fun getPrivateVariableFromSuperClass(fieldName: String, instanceToGetFrom: Any): Any? {
            val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
            val getMethod = MethodHandles.lookup()
                .findVirtual(fieldClass, "get", MethodType.methodType(Any::class.java, Any::class.java))
            val getNameMethod =
                MethodHandles.lookup().findVirtual(fieldClass, "getName", MethodType.methodType(String::class.java))
            val setAcessMethod = MethodHandles.lookup().findVirtual(
                fieldClass,
                "setAccessible",
                MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType)
            )

            val instancesOfFields: Array<out Any> = instanceToGetFrom.javaClass.superclass.declaredFields
            for (obj in instancesOfFields) {
                setAcessMethod.invoke(obj, true)
                val name = getNameMethod.invoke(obj)
                if (name.toString() == fieldName) {
                    return getMethod.invoke(obj, instanceToGetFrom)
                }
            }
            return null
        }

        @JvmStatic
        fun set(fieldName: String, instanceToModify: Any, newValue: Any?) {
            var field: Any? = null
            try {
                field = instanceToModify.javaClass.getField(fieldName)
            } catch (e: Throwable) {
                try {
                    field = instanceToModify.javaClass.getDeclaredField(fieldName)
                } catch (e: Throwable) {
                }
            }

            setFieldAccessibleHandle.invoke(field, true)
            setFieldHandle.invoke(field, instanceToModify, newValue)
        }

        @JvmStatic
        fun get(fieldName: String, instanceToGetFrom: Any): Any? {
            var field: Any? = null
            try {
                field = instanceToGetFrom.javaClass.getField(fieldName)
            } catch (e: Throwable) {
                try {
                    field = instanceToGetFrom.javaClass.getDeclaredField(fieldName)
                } catch (e: Throwable) {
                }
            }

            setFieldAccessibleHandle.invoke(field, true)
            return getFieldHandle.invoke(field, instanceToGetFrom)
        }

        @JvmStatic
        fun hasMethodOfName(name: String, instance: Any, contains: Boolean = false): Boolean {
            val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredMethods()

            if (!contains) {
                return instancesOfMethods.any { getMethodNameHandle.invoke(it) == name }
            } else {
                return instancesOfMethods.any { (getMethodNameHandle.invoke(it) as String).contains(name) }
            }
        }

        @JvmStatic
        fun hasVariableOfName(name: String, instance: Any): Boolean {

            val instancesOfFields: Array<out Any> = instance.javaClass.getDeclaredFields()
            return instancesOfFields.any { getFieldNameHandle.invoke(it) == name }
        }

        @JvmStatic
        fun instantiate(clazz: Class<*>, vararg arguments: Any?): Any? {
            val args = arguments.map { it!!::class.javaPrimitiveType ?: it!!::class.java }
            val methodType = MethodType.methodType(Void.TYPE, args)

            val constructorHandle = MethodHandles.lookup().findConstructor(clazz, methodType)
            val instance = constructorHandle.invokeWithArguments(arguments.toList())

            return instance
        }

        @JvmStatic
        fun hasMethodOfName(name: String, instance: Any): Boolean {

            val instancesOfMethods: Array<out Any> = instance.javaClass.methods
            return instancesOfMethods.any { getMethodNameHandle.invoke(it) == name }
        }

        @JvmStatic
        fun invokeMethod(methodName: String, instance: Any, vararg arguments: Any?): Any? {
            var method: Any? = null

            val clazz = instance.javaClass
            val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }
            val methodType = MethodType.methodType(Void.TYPE, args)

            method = clazz.getMethod(methodName, *methodType.parameterArray())

            return invokeMethodHandle.invoke(method, instance, arguments)
        }

        @JvmStatic
        fun getMethodFromSuperclass(methodName: String, instance: Any): Pair<Any?, Any?>? {
            var currentClass: Class<*>? = instance.javaClass
            while (currentClass != null) {
                val methods = currentClass.declaredMethods
                for (method in methods) {
                    // Get a MethodHandle for the getParameterTypes method
                    val getParameterTypesHandle =
                        ReflectionBetterUtilis.getParameterTypesHandle(method::class.java, "getParameterTypes")

                    // Use your getMethodNameHandle to retrieve the method name
                    if (getMethodNameHandle.invoke(method as Any) == methodName) {
                        // Invoke the MethodHandle to get the parameter types
                        val parameterTypes = getParameterTypesHandle.invoke(method as Any)
                        return Pair(method, parameterTypes)
                    }
                }
                currentClass = currentClass.superclass
            }
            return null
        }


        @JvmStatic
        fun invokeMethodWithAutoProjection(methodName: String, instance: Any, vararg arguments: Any?): Any? {
            // Retrieve the method and its parameter types
            val methodPair = getMethodFromSuperclass(methodName, instance) as Pair<Any?, Array<Class<*>>>

            // Check if the method was found
            val (method, parameterTypes) = methodPair
                ?: throw NoSuchMethodException("Method $methodName not found in class hierarchy of ${instance.javaClass.name}")

            // Prepare arguments by projecting them to the correct types
            val projectedArgs = Array(parameterTypes.size) { index ->
                val arg = arguments.getOrNull(index) // Safely get the argument

                if (arg == null) {
                    // If the expected type is a primitive type, throw an exception
                    if (parameterTypes[index].isPrimitive) {
                        throw IllegalArgumentException("Argument at index $index cannot be null for primitive type ${parameterTypes[index].name}")
                    }
                    null // Keep nulls as null for reference types
                } else {
                    // Try to convert the argument to the expected parameter type
                    try {
                        convertArgument(arg, parameterTypes[index])
                    } catch (e: Exception) {
                        throw IllegalArgumentException(
                            "Cannot convert argument at index $index to ${parameterTypes[index].name}",
                            e
                        )
                    }
                }
            }

            // Invoke the method with the projected arguments
            return invokeMethodHandle.invoke(method, instance, projectedArgs)
        }


        // Helper function to convert an argument to the expected type
        fun convertArgument(arg: Any, targetType: Class<*>): Any? {
            return when {
                targetType.isAssignableFrom(arg::class.java) -> arg // Use as-is if types match
                targetType.isPrimitive -> {
                    // Handle primitive types by boxing
                    when (targetType) {
                        Int::class.java -> (arg as Number).toInt()
                        Long::class.java -> (arg as Number).toLong()
                        Double::class.java -> (arg as Number).toDouble()
                        Float::class.java -> (arg as Number).toFloat()
                        Short::class.java -> (arg as Number).toShort()
                        Byte::class.java -> (arg as Number).toByte()
                        Boolean::class.java -> (arg as Boolean) // No conversion needed
                        Char::class.java -> (arg as Char) // No conversion needed
                        else -> throw IllegalArgumentException("Unsupported primitive type: ${targetType.name}")
                    }
                }

                else -> {
                    // For reference types, perform a cast if possible
                    targetType.cast(arg)
                }
            }
        }


        @JvmStatic
        fun invoke(methodName: String, instance: Any, vararg arguments: Any?, declared: Boolean = false): Any? {
            var method: Any? = null

            val clazz = instance.javaClass
            val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }
            val methodType = MethodType.methodType(Void.TYPE, args)

            if (!declared) {
                method = clazz.getMethod(methodName, *methodType.parameterArray())
            } else {
                method = clazz.getDeclaredMethod(methodName, *methodType.parameterArray())
            }

            return invokeMethodHandle.invoke(method, instance, arguments)
        }

        @JvmStatic
        //Extends the UI API by adding the required method to get the child objects of a panel, only when used within this class.
        fun UIPanelAPI.getChildrenCopy(): List<UIComponentAPI> {
            return invokeMethod("getChildrenCopy", this) as List<UIComponentAPI>
        }

        @JvmStatic
        fun isAssignableFrom(superClass: Class<*>, subClass: Any): Boolean {
            return isAssignableFromHandle.invoke(superClass, subClass) as Boolean
        }

        @JvmStatic
        fun findFieldMeetingCriteria(instance: Any, vararg arguments: Any?) {
            val declaredFields = instance.javaClass.declaredFields;

        }

        @JvmStatic
        fun findArrayOfSkills(currentTab: Any): Any? {
            var claSic = currentTab.javaClass
            while (true) {

                val declaredFields = claSic.declaredFields
                for (field in declaredFields) {
                    val isArray = isAssignableFrom(Person::class.java, getFieldType(field))
                    if (isArray) {
                        val fieldTrue = field as Any
                        setFieldAccessibleHandle.invoke(fieldTrue,true) as Any
                        return getFieldHandle.invoke(fieldTrue, currentTab);
                    }
                }
                if (claSic.superclass == null) break
                claSic = claSic.superclass
            }

            return null;
        }
        @JvmStatic
        fun findSkillSpec(currentTab: Any): Any? {
            var claSic = currentTab.javaClass
            while (true) {

                val declaredFields = claSic.declaredFields
                for (field in declaredFields) {
                    val isArray = isAssignableFrom(SkillSpec::class.java, getFieldType(field))
                    setFieldAccessibleHandle.invoke(field, true)
                    if (isArray) {
                        return getFieldHandle.invoke(field, currentTab);
                    }
                }
                if (claSic.superclass == null) break
                claSic = claSic.superclass
            }

            return null;
        }
        @JvmStatic
        fun setSkillSpec(currentTab: Any, spec: SkillSpec): Any? {
            var claSic = currentTab.javaClass
            while (true) {
                val declaredFields = claSic.declaredFields
                for (field in declaredFields) {
                    setFieldAccessibleHandle.invoke(field, true)

                    // Check if the field type is assignable from SkillSpec
                    if (isAssignableFrom(SkillSpec::class.java, getFieldType(field))) {
                        // Set the SkillSpec value in the found field
                        return setFieldHandle.invoke(field, currentTab, spec)
                    }
                }
                if (claSic.superclass == null) break
                claSic = claSic.superclass
            }
            return null
        }

        @JvmStatic
        fun findArrayOfRawSkills(currentTab: Any): List<*>? {
            var claSic = currentTab.javaClass
            while (true) {

                val declaredFields = claSic.declaredFields
                for (field in declaredFields) {
                    val isArray = isAssignableFrom(List::class.java, getFieldType(field))
                    setFieldAccessibleHandle.invoke(field, true)
                    if (isArray) {
                        for (item in getFieldHandle.invoke(field, currentTab) as List<*>){
                            if (item is SkillSpec){
                                return getFieldHandle.invoke(field, currentTab) as List<*>
                            }
                        }
                    }
                }
                if (claSic.superclass == null) break
                claSic = claSic.superclass
            }

            return null;
        }
        @JvmStatic
        fun setArrayOfSkills(currentTab: Any, newSkillsArray: List<SkillSpec>): Boolean {
            var claSic = currentTab.javaClass
            while (true) {
                val declaredFields = claSic.declaredFields
                for (field in declaredFields) {
                    try {
                        val isArray = isAssignableFrom(List::class.java, getFieldType(field))
                        setFieldAccessibleHandle.invoke(field, true)

                        if (isArray) {
                            val existingList = getFieldHandle.invoke(field, currentTab) as List<*>

                            // Check if the current list contains SkillSpec items
                            if (existingList.isNotEmpty() && existingList.first() is SkillSpec) {
                                // Set the new array of SkillSpec
                                setFieldHandle.invoke(field, currentTab, newSkillsArray)
                                return true // Return true if the field was successfully set
                            }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace() // Handle any exception that might occur
                    }
                }

                if (claSic.superclass == null) break
                claSic = claSic.superclass
            }
            return false // Return false if no array was set
        }
        @JvmStatic
        fun findSkillSpecField(currentTab: Any, prevInstance: Any? = null, visited: MutableSet<Any> = mutableSetOf()): Any? {
            // Avoid revisiting the same instance
            if (visited.contains(currentTab)) {
                return null
            }

            // Mark the current instance as visited
            visited.add(currentTab)

            val declaredFields = currentTab.javaClass.declaredFields
            for (field in declaredFields) {
                // Make the field accessible
                setFieldAccessibleHandle.invoke(field, true)

                // Get the field type
                val fieldType = getFieldType(field) as Class<*>

                // Check if the field's type is SkillSpec or assignable to SkillSpec
                if (isAssignableFrom(SkillSpec::class.java, fieldType)) {
                    return getFieldHandle.invoke(field, currentTab) // Return the SkillSpec field
                }

                // If the field itself contains fields (i.e., it's a nested class), dive deeper
                if (fieldType.declaredFields.isNotEmpty()) {
                    val nestedInstance = getFieldHandle.invoke(field, currentTab) // Get the field instance
                    if (nestedInstance != null) {
                        // Recursively call the method on the nested instance
                        val result = findSkillSpecField(nestedInstance, currentTab, visited)
                        if (result is SkillSpec) {
                            return result // Found the SkillSpec, return it
                        }
                    }
                }
            }
            return null // SkillSpec not found in this branch
        }



        @JvmStatic
        fun findUIPanelWithFleetAndLabel(currentTab: Any): UIPanelAPI? {
            val declaredFields = currentTab.javaClass.declaredFields

            for (field in declaredFields) {
                // Use the custom isAssignableFrom virtual method to check if UIPanelAPI is assignable
                val isUIPanel = isAssignableFrom(UIPanelAPI::class.java, getFieldType(field))
                if (!isUIPanel) {
                    continue
                }

                // Make the field accessible using MethodHandles
                setFieldAccessibleHandle.invoke(field, true)

                var hasFleetField = false
                var hasLabelField = false

                // Use MethodHandles to get the declared fields of UIPanelAPI class
                val innerFields = field.javaClass.declaredFields
                for (innerField in innerFields) {
                    // Check if the inner field is of type CampaignFleetAPI
                    val isFleetField = isAssignableFrom(CampaignFleetAPI::class.java, getFieldType(innerField))
                    if (isFleetField) {
                        hasFleetField = true
                    }

                    // Check if the inner field is of type LabelAPI
                    val isLabelField = isAssignableFrom(LabelAPI::class.java, getFieldType(innerField))
                    if (isLabelField) {
                        hasLabelField = true
                    }

                    // If both conditions are met, return the UIPanelAPI
                    if (hasFleetField && hasLabelField) {
                        try {
                            // Use MethodHandles to get the field value
                            return getFieldHandle.invoke(field, currentTab) as UIPanelAPI
                        } catch (e: Throwable) {
                            throw RuntimeException("Failed to access UIPanelAPI", e)
                        }
                    }
                }
                for (innerField in field.javaClass.fields) {
                    // Check if the inner field is of type CampaignFleetAPI
                    val isFleetField = isAssignableFrom(CampaignFleetAPI::class.java, getFieldType(innerField))
                    if (isFleetField) {
                        hasFleetField = true
                    }

                    // Check if the inner field is of type LabelAPI
                    val isLabelField = isAssignableFrom(LabelAPI::class.java, getFieldType(innerField))
                    if (isLabelField) {
                        hasLabelField = true
                    }

                    // If both conditions are met, return the UIPanelAPI
                    if (hasFleetField && hasLabelField) {
                        try {
                            // Use MethodHandles to get the field value
                            return getFieldHandle.invoke(field, currentTab) as UIPanelAPI
                        } catch (e: Throwable) {
                            throw RuntimeException("Failed to access UIPanelAPI", e)
                        }
                    }
                }
            }
            for (field in currentTab.javaClass.fields) {
                // Use the custom isAssignableFrom virtual method to check if UIPanelAPI is assignable
                val isUIPanel = isAssignableFrom(UIPanelAPI::class.java, getFieldType(field))
                if (!isUIPanel) {
                    continue
                }

                // Make the field accessible using MethodHandles
                setFieldAccessibleHandle.invoke(field, true)

                var hasFleetField = false
                var hasLabelField = false

                // Use MethodHandles to get the declared fields of UIPanelAPI class
                val innerFields = field.javaClass.declaredFields
                for (innerField in innerFields) {
                    // Check if the inner field is of type CampaignFleetAPI
                    val isFleetField = isAssignableFrom(CampaignFleetAPI::class.java, getFieldType(innerField))
                    if (isFleetField) {
                        hasFleetField = true
                    }

                    // Check if the inner field is of type LabelAPI
                    val isLabelField = isAssignableFrom(LabelAPI::class.java, getFieldType(innerField))
                    if (isLabelField) {
                        hasLabelField = true
                    }

                    // If both conditions are met, return the UIPanelAPI
                    if (hasFleetField && hasLabelField) {
                        try {
                            // Use MethodHandles to get the field value
                            return getFieldHandle.invoke(field, currentTab) as UIPanelAPI
                        } catch (e: Throwable) {
                            throw RuntimeException("Failed to access UIPanelAPI", e)
                        }
                    }
                }
            }
            return null
        }
    }


}
