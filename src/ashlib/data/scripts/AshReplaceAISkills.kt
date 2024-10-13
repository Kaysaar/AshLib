package ashlib.data.scripts

import ashlib.data.plugins.handlers.AICoreSkillPollHandler
import ashlib.data.plugins.reflection.ReflectionBetterUtilis
import ashlib.data.plugins.ui.plugins.DarkHighlightPlugin
import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CoreUIAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.coreui.CaptainPickerDialog
import com.fs.starfarer.rpg.Person
import java.awt.Color
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class AshReplaceAISkills :EveryFrameScript {
        private val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)

        private val getFieldHandle = MethodHandles.lookup()
            .findVirtual(fieldClass, "get", MethodType.methodType(Any::class.java, Any::class.java))
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


        private fun getFieldType(field: Any): Any { // Adjusted to return Any
            return getFieldTypeHandle.invoke(field) as Any
        }

        private fun hasMethodOfName(name: String, instance: Any): Boolean {

            val instancesOfMethods: Array<out Any> = instance.javaClass.methods
            return instancesOfMethods.any { getMethodNameHandle.invoke(it) == name }
        }


        private fun invokeMethod(methodName: String, instance: Any, vararg arguments: Any?): Any? {
            var method: Any? = null

            val clazz = instance.javaClass
            val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }
            val methodType = MethodType.methodType(Void.TYPE, args)

            method = clazz.getMethod(methodName, *methodType.parameterArray())

            return invokeMethodHandle.invoke(method, instance, arguments)
        }


        private fun getMethodFromSuperclass(methodName: String, instance: Any): Pair<Any?, Any?>? {
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



       private fun invokeMethodWithAutoProjection(methodName: String, instance: Any, vararg arguments: Any?): Any? {
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
       private fun convertArgument(arg: Any, targetType: Class<*>): Any? {
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



        private fun invoke(methodName: String, instance: Any, vararg arguments: Any?, declared: Boolean = false): Any? {
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


        //Extends the UI API by adding the required method to get the child objects of a panel, only when used within this class.
        private fun getChildren(panel:Any): List<UIComponentAPI> {
            return invokeMethod("getChildrenCopy", panel) as List<UIComponentAPI>
        }
        private fun UIPanelAPI.getChildrenCopy(): List<UIComponentAPI> {
            return invokeMethod("getChildrenCopy", this) as List<UIComponentAPI>
        }

        private fun isAssignableFrom(superClass: Class<*>, subClass: Any): Boolean {
            return isAssignableFromHandle.invoke(superClass, subClass) as Boolean
        }
    fun getCoreUI(): UIPanelAPI? {
        val campaignUI = Global.getSector().campaignUI
        val dialog = campaignUI.currentInteractionDialog
        val core = if (dialog == null) {
            invokeMethod("getCore", campaignUI) as CoreUIAPI
        } else {
            invokeMethod("getCoreUI", dialog) as CoreUIAPI
        }

        return if (core == null) null else core as UIPanelAPI
    }

        private fun findArrayOfSkills(currentTab: Any): Any? {
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
    //Inspired with Officer Extension
    var inserted: Boolean = false
    var currentlyAffectedPerson: PersonAPI? = null
    var innerPanel: UIPanelAPI? = null
    var plugin: DarkHighlightPlugin? = null
    var confirmButton: ButtonAPI? = null
    var shouldInsert: Boolean = false
    var mapSkillRaw: HashMap<String?, Float?>? = null
    var originalMap: HashMap<String, Float>? = null
    var labelComponent: LabelAPI? = null
    var currentSkills: Int = 0
     var w: Float = 70f
    var h: Float = 70f

    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    var skillmap: LinkedHashMap<String, ButtonAPI>? = LinkedHashMap()

    override fun advance(amount: Float) {
        if (Global.getSector().campaignUI.currentCoreTab != null) {
            val dialog = findSkillDialog()
            if (getCoreUI() == null) {
                return
            }
            val dialog2: Any? = findCaptainDialog(getCoreUI())
            if (dialog == null && dialog2 != null) {
                inserted = false
                val officerListData = invokeMethod("getListOfficers", dialog2)
                val offcerRawData = invokeMethod("getItems", officerListData!!) as ArrayList<Any>?
                if (currentlyAffectedPerson != null) {
                    for (offcerRawDatum in offcerRawData!!) {
                        val person = invokeMethod("getPerson", offcerRawDatum) as PersonAPI?
                        if (person!!.id == currentlyAffectedPerson!!.id) {
                            if (mapSkillRaw != null) {
                                if (shouldInsert) {
                                    person.stats.isSkipRefresh = true
                                    for (skillLevelAPI in person.stats.skillsCopy) {
                                        person.stats.setSkillLevel(
                                            skillLevelAPI.skill.id,
                                            originalMap!![skillLevelAPI.skill.id]!!
                                        )
                                    }
                                    for ((key, value) in mapSkillRaw!!) {
                                        person.stats.setSkillLevel(key, value!!)
                                    }

                                    person.stats.isSkipRefresh = false
                                }
                            }


                            invokeMethod("recreate", offcerRawDatum)
                            clearData()
                            break
                        }
                    }
                }

                clearData()
                return
            }
            if (dialog == null) return
            if (findArrayOfSkills(dialog) != null) {
                val person = findArrayOfSkills(dialog) as PersonAPI?
                if (person!!.aiCoreId != null && AICoreSkillPollHandler.getInstance()
                        .getSetOfSkills(person.aiCoreId) != null
                ) {
                    if (currentlyAffectedPerson == null) {
                        currentlyAffectedPerson = person
                    }
                    if (!inserted) {
                        for (skillLevelAPI in person.stats.skillsCopy) {
                            if (mapSkillRaw == null) {
                                mapSkillRaw = HashMap()
                                originalMap = HashMap()
                            }
                            if (getSkills().contains(skillLevelAPI.skill.id)) {
                                mapSkillRaw!![skillLevelAPI.skill.id] = skillLevelAPI.level
                            }
                            originalMap!![skillLevelAPI.skill.id] = skillLevelAPI.level
                        }
                        innerPanel = invokeMethod("getInnerPanel", dialog) as UIPanelAPI?
                        if (innerPanel == null) return
                        for (componentAPI in getChildren(innerPanel!!)) {
                            if (componentAPI is LabelAPI) {
                                if (!(componentAPI as LabelAPI).text.contains("Select")) {
                                    val label = invokeMethodWithAutoProjection(
                                        "createSkillPointsLabel",
                                        dialog,
                                        currentlyAffectedPerson!!.stats.level
                                    ) as LabelAPI?
                                    innerPanel!!.removeComponent(componentAPI)
                                    labelComponent = label
                                    label!!.setColor(Misc.getGrayColor())
                                    label.setHighlightColor(Color.ORANGE)
                                    innerPanel!!.addComponent(labelComponent as UIComponentAPI?)
                                        .inTL(10f, innerPanel!!.position.height - 24)
                                    updateLabel(currentlyAffectedPerson)
                                }
                                continue
                            }
                            if (componentAPI is ButtonAPI) {
                                if ((componentAPI as ButtonAPI).text == null) {
                                    w = componentAPI.getPosition().width
                                    h = componentAPI.getPosition().height

                                    innerPanel!!.removeComponent(componentAPI)
                                } else {
                                    if ((componentAPI as ButtonAPI).text.contains("Confirm")) {
                                        confirmButton = componentAPI
                                    }
                                }
                            } else {
                                innerPanel!!.removeComponent(componentAPI)
                            }
                        }
                        for (skill in getSkills()) {
                            if (mapSkillRaw!![skill] == null) {
                                mapSkillRaw!![skill] = 0f
                                originalMap!![skill] = 0f
                            }
                        }
                        for (skill in getSkills()) {
                            populateSkillMap(skill, person)
                        }
                        var x = 10f
                        var y = 40f
                        var i = 0
                        for (componentAPI in skillmap!!.entries) {
                            x = placeButton(componentAPI, w, h, person, x, y)
                            i++
                            if (i >= 7) {
                                i = 0
                                y += 82f
                                x = 10f
                            }
                        }
                        plugin = DarkHighlightPlugin(skillmap, 0.6f, mapSkillRaw)
                        val panel = Global.getSettings().createCustom(w, h, plugin)
                        plugin!!.panelTied = panel
                        innerPanel!!.addComponent(panel).inTL(x, y)
                        inserted = true
                        if (mapSkillRaw != null) {
                            updateLabel(currentlyAffectedPerson)
                        }
                    }
                    handleEnabledButtons()
                    handleButtons()
                } else {
                    clearData()
                }
            } else {
                clearData()
            }
        }
    }

    private fun updateLabel(person: PersonAPI?) {
        currentSkills = getCurrentSkillCount()
        val currentSkill = "" + currentSkills + " / " + person!!.stats.level
        labelComponent!!.text = "Skills: $currentSkill"
        labelComponent!!.setHighlight(currentSkill)
    }

    fun getSkills(): LinkedHashSet<String> {
        return (AICoreSkillPollHandler.getInstance().getSetOfSkills(currentlyAffectedPerson!!.aiCoreId))
    }

    private fun placeButton(
        componentAPI: Map.Entry<String, ButtonAPI>,
        w: Float,
        h: Float,
        person: PersonAPI?,
        x: Float,
        y: Float
    ): Float {
        var x = x
        componentAPI.value.position.setSize(w, h)
        innerPanel!!.addComponent(componentAPI.value).inTL(x, y)
        innerPanel!!.bringComponentToTop(componentAPI.value)

        x += w + 10
        return x
    }

    private fun populateSkillMap(skill: String, person: PersonAPI?) {
        val panelAPI = Global.getSettings().createCustom(100f, 100f, null)
        val tooltip = panelAPI.createUIElement(500f, 70f, false)
        val dummy = Global.getFactory().createPerson()
        var level = mapSkillRaw!![skill]!!
        var demoting = true

        if (level == 0f) {
            level = 1f
            demoting = false
        }
        dummy.stats.setSkillLevel(skill, level)
        if (dummy.stats.hasSkill(skill)) {
            val component = tooltip.addSkillPanel(dummy, 0f)
            val exactSkillButtonsOfCharacter = ArrayList<UIComponentAPI>()
            for (deepRoot in (((component as UIPanelAPI).getChildrenCopy()[0] as UIPanelAPI).getChildrenCopy()[0] as UIPanelAPI).getChildrenCopy()) {
                for (componentAPI in (deepRoot as UIPanelAPI).getChildrenCopy()) {
                    if (demoting) {
                        ((componentAPI as UIPanelAPI).getChildrenCopy()[0] as ButtonAPI).setButtonPressedSound("ui_char_decrease_skill")
                    } else {
                        val spec = Global.getSettings().getSkillSpec(skill)

                        ((componentAPI as UIPanelAPI).getChildrenCopy()[0] as ButtonAPI).setButtonPressedSound(
                            AICoreSkillPollHandler.getInstance()
                                .getSoundBasedOnAptitudeAndTier(spec.governingAptitudeId, spec.tier)
                        )
                    }
                    (componentAPI.getChildrenCopy()[0] as ButtonAPI).setButtonDisabledPressedSound("ui_char_can_not_increase_skill_or_aptitude")
                    skillmap!![skill] = componentAPI.getChildrenCopy()[0] as ButtonAPI
                }
            }
        }
    }

    fun clearData() {
        if (skillmap != null) {
            skillmap!!.clear()
        }

        if (mapSkillRaw != null) {
            mapSkillRaw!!.clear()
        }
        if (originalMap != null) {
            originalMap!!.clear()
        }


        currentlyAffectedPerson = null
        innerPanel = null
        plugin = null
        shouldInsert = false
        confirmButton = null
        labelComponent = null
        mapSkillRaw = null
        originalMap = null
        currentSkills = 0
    }

    fun findSkillDialog(): Any? {
        val panelAPI = getCoreUI() ?: return null

        for (componentAPI in getChildren(panelAPI)) {
            if (hasMethodOfName("getNumSkills", componentAPI!!)) {
                return componentAPI
            }
        }
        return null
    }

    fun handleButtons() {
        if (confirmButton != null) {
            shouldInsert = confirmButton!!.isChecked
        }
        for (entry in skillmap!!.entries) {
            if (entry.value.isChecked) {
                if (mapSkillRaw == null) {
                    mapSkillRaw = HashMap()
                }
                entry.value.isChecked = false

                if (mapSkillRaw!![entry.key] == 0f) {
                    mapSkillRaw!![entry.key] = 20f
                } else {
                    mapSkillRaw!![entry.key] = 0f
                }
                val x = entry.value.position.x - innerPanel!!.position.x
                var y = entry.value.position.y - innerPanel!!.position.y - innerPanel!!.position.height
                innerPanel!!.removeComponent(entry.value)
                populateSkillMap(entry.key, currentlyAffectedPerson)
                placeButton(entry, w, h, currentlyAffectedPerson, x, y)
                y = entry.value.position.y - innerPanel!!.position.y - innerPanel!!.position.height
                innerPanel!!.removeComponent(entry.value)
                placeButton(entry, w, h, currentlyAffectedPerson, x, y)
                innerPanel!!.bringComponentToTop(plugin!!.panelTied)
                currentSkills = getCurrentSkillCount()
                updateLabel(currentlyAffectedPerson)
                break
            }
        }
    }

    fun handleEnabledButtons() {
        val enabled = getCurrentSkillCount() < currentlyAffectedPerson!!.stats.level
        for ((key, value) in skillmap!!) {
            if (mapSkillRaw!![key] == 0f) {
                if (value.isEnabled != enabled) {
                    value.isEnabled = enabled
                }
            }
        }
    }

    fun findCaptainDialog(coreUI: UIPanelAPI?): CaptainPickerDialog? {
        for (componentAPI in coreUI?.let { getChildren(it) }!!) {
            if (componentAPI is CaptainPickerDialog) {
                return componentAPI
            }
        }
        return null
    }

    fun getCurrentSkillCount(): Int {
        var skills = 0
        for ((_, value) in mapSkillRaw!!) {
            if (value!! > 0) {
                skills++
            }
        }
        return skills
    }

}