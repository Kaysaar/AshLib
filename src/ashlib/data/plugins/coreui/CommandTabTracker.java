package ashlib.data.plugins.coreui;

import ashlib.data.plugins.reflection.ReflectionBetterUtilis;
import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.state.AppDriver;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;


public class CommandTabTracker implements EveryFrameScript {
    public static float WIDTH,HEIGHT;

    private static class ReflectionUtilis {
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

        static {
            try {
                fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());
                setFieldHandle = lookup.findVirtual(fieldClass, "set", MethodType.methodType(Void.TYPE, Object.class, Object.class));
                getFieldHandle = lookup.findVirtual(fieldClass, "get", MethodType.methodType(Object.class, Object.class));
                getFieldNameHandle = lookup.findVirtual(fieldClass, "getName", MethodType.methodType(String.class));
                setFieldAccessibleHandle = lookup.findVirtual(fieldClass, "setAccessible", MethodType.methodType(Void.TYPE, boolean.class));

                methodClass = Class.forName("java.lang.reflect.Method", false, Class.class.getClassLoader());
                getMethodNameHandle = lookup.findVirtual(methodClass, "getName", MethodType.methodType(String.class));
                invokeMethodHandle = lookup.findVirtual(methodClass, "invoke", MethodType.methodType(Object.class, Object.class, Object[].class));
                setMethodAccessable = lookup.findVirtual(methodClass, "setAccessible", MethodType.methodType(Void.TYPE, boolean.class));
            } catch (Exception e) {
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

        public static List<UIComponentAPI> getChildrenCopy(UIPanelAPI panel) {
            try {
                return (List<UIComponentAPI>) invokeMethod("getChildrenCopy", panel);
            } catch (Throwable e) {
                return null;
            }
        }

        public static Pair<Object, Class<?>[]> getMethodFromSuperclass(String methodName, Object instance) {
            Class<?> currentClass = instance.getClass();

            while (currentClass != null) {
                // Retrieve all declared methods in the current class
                Object[] methods = currentClass.getDeclaredMethods();

                for (Object method : methods) {
                    try {
                        // Retrieve the MethodHandle for the getParameterTypes method
                        MethodHandle getParameterTypesHandle = ReflectionBetterUtilis.getParameterTypesHandle(method.getClass(), "getParameterTypes");
                        // Use the MethodHandle to retrieve the method's name

                        // Check if the method name matches
                        if (getMethodNameHandle.invoke(method).equals(methodName)) {
                            // Invoke the MethodHandle to get the parameter types
                            Class<?>[] parameterTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
                            return new Pair<>(method, parameterTypes);
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();  // Handle any reflection errors
                    }
                }
                // Move to the superclass if no match is found
                currentClass = currentClass.getSuperclass();
            }

            // Return null if the method was not found in the class hierarchy
            return null;
        }

        public static Object invokeMethodWithAutoProjection(String methodName, Object instance, Object... arguments) {
            // Retrieve the method and its parameter types
            Pair<Object, Class<?>[]> methodPair = getMethodFromSuperclass(methodName, instance);

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
                        throw new IllegalArgumentException("Cannot convert argument at index " + index + " to " + parameterTypes[index].getName(), e);
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
    }



    private static class ProductionUtil {
        public static UIPanelAPI getCoreUI() {
            CampaignUIAPI campaignUI;
            campaignUI = Global.getSector().getCampaignUI();
            Object dialog = campaignUI.getCurrentInteractionDialog();
            if (AppDriver.getInstance().getCurrentState() instanceof CampaignState) {
                dialog = ReflectionUtilis.invokeMethod("getEncounterDialog", AppDriver.getInstance().getCurrentState());
            }

            CoreUIAPI core;
            if (dialog == null) {
                core = (CoreUIAPI) ReflectionUtilis.invokeMethod("getCore", campaignUI);
            } else {
                core = (CoreUIAPI) ReflectionUtilis.invokeMethod("getCoreUI", dialog);
            }
            if (core == null && dialog != null) {
                return (UIPanelAPI) ReflectionUtilis.invokeMethod("getParent", dialog);

            }
            return core == null ? null : (UIPanelAPI) core;
        }


        public static UIPanelAPI getCurrentTab() {
            UIPanelAPI coreUltimate = getCoreUI();
            UIPanelAPI core = (UIPanelAPI) ReflectionUtilis.invokeMethod("getCurrentTab", coreUltimate);
            return core == null ? null : (UIPanelAPI) core;
        }
    }


    boolean inserted = false;
    boolean insertedOnce = false;
    boolean removed = false;
    transient HashMap<ButtonAPI, Object> panelMap = null;
    boolean turnedMusicOnce = false;
    private boolean needsToResetStates = false;
    public static boolean sendSignalToOpenCore = false;
    transient ButtonAPI currentTab = null;
    transient public LinkedHashMap<String, CommandUIPlugin> additionalPlugins = new LinkedHashMap<>();
    String currMusicId;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if ((Global.getSector().getCampaignUI().getCurrentCoreTab() == null || Global.getSector().getCampaignUI().getCurrentCoreTab() != CoreUITabId.OUTPOSTS)) {
            inserted = false;
            if(panelMap!=null) {
                panelMap.clear();
            }
            panelMap = null;
            currentTab = null;
            if(additionalPlugins!=null) {

                for (CommandUIPlugin value : additionalPlugins.values()) {
                    value.clearUI(turnedMusicOnce);
                }

                additionalPlugins.clear();
            }
            additionalPlugins = null;
            currMusicId =null;

            removed = false;
            insertedOnce = false;
            turnedMusicOnce = false;
            return;
        }
        if (Global.getSector().getCampaignUI().getCurrentCoreTab() != null) {
            sendSignalToOpenCore = false;
        }
        UIPanelAPI mainParent = ProductionUtil.getCurrentTab();
        if (mainParent == null) return;
        ButtonAPI button = tryToGetButtonProd("income");

        if (button == null) {
            return;
        }
        List<CommandTabListener> listeners = Global.getSector().getListenerManager().getListeners(CommandTabListener.class).stream().sorted(new Comparator<CommandTabListener>() {
            @Override
            public int compare(CommandTabListener o1, CommandTabListener o2) {
                return Integer.compare(o1.getOrder(), o2.getOrder());
            }
        }).toList();
        LinkedHashMap<String, String> vanillaButtonsOriginal = new LinkedHashMap<>();
        vanillaButtonsOriginal.put("colonies", "colonies");
        vanillaButtonsOriginal.put("orders", "orders");
        vanillaButtonsOriginal.put("income", "income");
        vanillaButtonsOriginal.put("doctrine & blueprints", "doctrine & blueprints");
        vanillaButtonsOriginal.put("custom production", "custom production");
        float xS = 0;
        for (String string : vanillaButtonsOriginal.keySet()) {
            ButtonAPI bu = tryToGetButtonProd(string);
            if(bu!=null){
                tryToGetButtonProd(string).getPosition().inTL(xS,0);
                xS+=tryToGetButtonProd(string).getPosition().getWidth()+1;
            }
            else{
                break;
            }

        }
        for (CommandTabListener listener : listeners) {
            if (tryToGetButtonProd(listener.getNameForTab().toLowerCase()) == null) {
                if (listener.getButtonToReplace() != null) {
                    if(listener.getButtonToBePlacedNear()==null){
                        ButtonAPI x = tryToGetButtonProd(listener.getButtonToReplace().toLowerCase());
                        insertButton(x.getPosition().getX(),x.getPosition().getY(),x.getPosition().getHeight(), mainParent, listener.getNameForTab(), listener.getTooltipCreatorForButton(), listener.getWidthOfButton(), listener.getKeyBind(), !listener.shouldButtonBeEnabled());
                        mainParent.removeComponent(x);
                        vanillaButtonsOriginal.put(listener.getButtonToReplace().toLowerCase(), listener.getNameForTab().toLowerCase());
                    }
                    else{
                        mainParent.removeComponent(tryToGetButtonProd(vanillaButtonsOriginal.get(listener.getButtonToReplace().toLowerCase())));
                        vanillaButtonsOriginal.put(listener.getButtonToReplace().toLowerCase(), listener.getNameForTab().toLowerCase());
                    }

                }
                if(listener.getButtonToBePlacedNear()!=null){
                    insertButton(tryToGetButtonProd(vanillaButtonsOriginal.get(listener.getButtonToBePlacedNear().toLowerCase())), mainParent, listener.getNameForTab(), listener.getTooltipCreatorForButton(), tryToGetButtonProd(vanillaButtonsOriginal.get("colonies")), listener.getWidthOfButton(), listener.getKeyBind(), !listener.shouldButtonBeEnabled());

                }

            }
        }

        float y = button.getPosition().getY();
        float x = button.getPosition().getX();
        if (y < 0) {
            y *= -1;
        }
        if (!removed) {
            if(currMusicId==null){
                currMusicId = Global.getSoundPlayer().getCurrentMusicId();
            }
            removed = true;
            panelMap = new HashMap<>();
            panelMap.putAll(getPanelMap(mainParent));
            for (CommandTabListener listener : listeners) {
                listener.performRecalculations((UIComponentAPI) panelMap.get(button));
            }
            removePanels((ArrayList<UIComponentAPI>) ReflectionUtilis.getChildrenCopy(mainParent), mainParent, null);
            for (CommandTabListener listener : listeners) {

                CommandUIPlugin plugin = listener.createPlugin();
                plugin.init(CommandTabMemoryManager.getInstance().getTabStates().get(listener.getNameForTab().toLowerCase()), mainParent);
                panelMap.put(tryToGetButtonProd(listener.getNameForTab().toLowerCase()), plugin.getMainPanel());
                if(additionalPlugins==null)additionalPlugins = new LinkedHashMap<>();
                additionalPlugins.put(listener.getNameForTab().toLowerCase(), plugin);
            }
        }

        if (currentTab == null && CommandTabMemoryManager.getInstance().getLastCheckedTab() == null) {
            for (ButtonAPI buttonAPI : panelMap.keySet()) {
                if (buttonAPI.isHighlighted()) {
                    currentTab = buttonAPI;
                    CommandTabMemoryManager.getInstance().setLastCheckedTab(currentTab.getText().toLowerCase());
                    for (CommandTabListener listener : listeners) {
                        listener.performRefresh(currentTab);
                    }
                    break;
                }
            }
        }
        if (currentTab == null && CommandTabMemoryManager.getInstance().getLastCheckedTab() != null) {
            for (ButtonAPI buttonAPI : panelMap.keySet()) {
                if (buttonAPI.getText().toLowerCase().contains(CommandTabMemoryManager.getInstance().getLastCheckedTab())) {
                    currentTab = buttonAPI;
                    for (CommandTabListener listener : listeners) {
                        listener.performRefresh(currentTab);
                    }
                }
            }
        }
        if (currentTab != null) {
            Object obj = panelMap.get(currentTab);
            if (obj instanceof CustomPanelAPI panelAPI) {
                if (panelAPI.getPlugin() instanceof CommandUIPlugin plugin) {
                    if (plugin.doesPlayCustomSoundWhenEnteredEntireTab()) {
                        if (!turnedMusicOnce) {
                            turnedMusicOnce = true;
                            plugin.playSound(null);
                        }
                    } else if (plugin.doesPlayCustomSound() && plugin.getCurrentlyChosen() != null) {
                        if (!turnedMusicOnce) {
                            turnedMusicOnce = true;
                            plugin.playSound(plugin.getCurrentlyChosen());
                        }
                    }
                } else {
                    if(turnedMusicOnce){
                        Global.getSoundPlayer().restartCurrentMusic();
                    }
                    turnedMusicOnce = false;
                }
            } else {
                if(turnedMusicOnce){
                    Global.getSoundPlayer().restartCurrentMusic();
                }
                turnedMusicOnce = false;
            }
        }
        if (!hasComponentPresent((UIComponentAPI) panelMap.get(currentTab))&&currentTab!=null) {
            removePanels((ArrayList<UIComponentAPI>) ReflectionUtilis.getChildrenCopy(mainParent), mainParent, null);
            if (currentTab.getText().toLowerCase().contains("doctrine & blueprints")) {
                UIComponentAPI comp = (UIComponentAPI) panelMap.get(currentTab);
                ReflectionUtilis.invokeMethodWithAutoProjection("createIfNeeded", comp);
                ReflectionUtilis.invokeMethodWithAutoProjection("restoreTableUIState", comp);
                Fader fader = (Fader) ReflectionUtilis.invokeMethodWithAutoProjection("getFader", comp);
                fader.forceIn();
            }
            mainParent.addComponent((UIComponentAPI) panelMap.get(currentTab));
            CommandTabMemoryManager.getInstance().setLastCheckedTab(currentTab.getText().toLowerCase());

        }
        handleButtonsHighlight();
        handleButtons();



    }
    private HashMap<ButtonAPI, Object> getPanelMap(UIComponentAPI mainParent) {
        HashMap<ButtonAPI, Object> map = (HashMap<ButtonAPI, Object>)ReflectionUtilis.invokeMethod("getButtonToTab", mainParent);
        return map;
    }
    private void handleButtonsHighlight() {
        for (ButtonAPI buttonAPI : panelMap.keySet()) {
            if (!buttonAPI.equals(currentTab)) {
                buttonAPI.unhighlight();
            } else {
                buttonAPI.highlight();
            }
        }

    }
    public static boolean lockedState = false;
    public static boolean initalizedState = false;

    public static void lockMainPanel(){
        lockedState = true;
        initalizedState = true;
    }
    public static void unlockMainPanel(){
        lockedState = false;
        initalizedState = true;
    }


    private void handleButtons() {
        ButtonAPI bt = tryToGetButtonProd("doctrine & blueprints");
        if(initalizedState){
            initalizedState = false;
            if(lockedState){
                panelMap.keySet().stream().filter(x -> !x.getText().contains("orders")).forEach(x -> x.setEnabled(false));
                ReflectionUtilis.invokeMethodWithAutoProjection("disableEnabledTabs",ProductionUtil.getCoreUI());
                ButtonAPI button = (ButtonAPI) ReflectionUtilis.invokeMethodWithAutoProjection("getPowerButton",ProductionUtil.getCoreUI());
                button.setEnabled(false);
            }
            else{
                panelMap.keySet().stream().filter(x -> !x.getText().contains("orders")).forEach(x -> x.setEnabled(true));
                ReflectionUtilis.invokeMethodWithAutoProjection("enableAllTabs",ProductionUtil.getCoreUI());
                ButtonAPI button = (ButtonAPI) ReflectionUtilis.invokeMethodWithAutoProjection("getPowerButton",ProductionUtil.getCoreUI());
                button.setEnabled(true);
            }
        }

        if (bt!=null&&!bt.isEnabled()) {
            if (!needsToResetStates) {
                needsToResetStates = true;
                for (ButtonAPI buttonAPI : panelMap.keySet()) {
                    buttonAPI.setEnabled(false);
                }
            }

        } else if (needsToResetStates) {
            needsToResetStates = false;
            panelMap.keySet().stream().filter(x -> !x.getText().contains("orders")).forEach(x -> x.setEnabled(true));
        }

        for (ButtonAPI buttonAPI : panelMap.keySet()) {
            if (buttonAPI.isChecked()) {
                buttonAPI.setChecked(false);
                if (!currentTab.equals(buttonAPI)) {
                    Global.getSector().getListenerManager().getListeners(CommandTabListener.class).stream().sorted(new Comparator<CommandTabListener>() {
                        @Override
                        public int compare(CommandTabListener o1, CommandTabListener o2) {
                            return Integer.compare(o1.getOrder(), o2.getOrder());
                        }
                    }).toList().forEach(x->x.performRefresh(buttonAPI));
                    ProductionUtil.getCurrentTab().removeComponent((UIComponentAPI) panelMap.get(currentTab));
                    Object obj = panelMap.get(currentTab);

                    if (obj instanceof CustomPanelAPI panelAPI) {
                        if (panelAPI.getPlugin() instanceof CommandUIPlugin plugin) {
                            if (plugin.doesPlayCustomSoundWhenEnteredEntireTab()||plugin.doesPlayCustomSound()) {
                            }
                        }
                    }
                    obj = panelMap.get(buttonAPI);
                    if (obj instanceof CustomPanelAPI panelAPI) {
                        if (panelAPI.getPlugin() instanceof CommandUIPlugin plugin) {
                            if (plugin.doesPlayCustomSoundWhenEnteredEntireTab()) {

                                plugin.playSound(null);
                            } else if (plugin.doesPlayCustomSound() && plugin.getCurrentlyChosen() != null) {

                                plugin.playSound(plugin.getCurrentlyChosen());

                            }
                        }
                    }

                    currentTab = buttonAPI;
                    CommandTabMemoryManager.getInstance().setLastCheckedTab(buttonAPI.getText().toLowerCase());
                }
            }
        }
    }

    private boolean hasComponentPresent(UIComponentAPI component) {
        if(component==null)return false;
        for (UIComponentAPI buttonAPI : ReflectionUtilis.getChildrenCopy(ProductionUtil.getCurrentTab())) {
            if (component.equals(buttonAPI)) {
                return true;
            }
        }
        return false;
    }

    private static void removePanels(ArrayList<UIComponentAPI> componentAPIS, UIPanelAPI mainParent, UIComponentAPI panelToIgnore) {
        for (UIComponentAPI componentAPI : componentAPIS) {
            if (componentAPI instanceof ButtonAPI) continue;

            if (componentAPI.equals(panelToIgnore)) continue;
            mainParent.removeComponent(componentAPI);
        }
    }

    public static ButtonAPI tryToGetButtonProd(String name) {
        ButtonAPI button = null;
        try {
            for (UIComponentAPI componentAPI : ReflectionUtilis.getChildrenCopy((UIPanelAPI) ProductionUtil.getCurrentTab())) {
                if (componentAPI instanceof ButtonAPI) {
                    if (((ButtonAPI) componentAPI).getText().toLowerCase().contains(name)) {
                        button = (ButtonAPI) componentAPI;
                        break;
                    }
                }
            }
            return button;
        } catch (Exception e) {

        }
        return button;

    }

    private void insertButton(ButtonAPI buttonOfReference, UIPanelAPI mainParent, String name, TooltipMakerAPI.TooltipCreator creator, ButtonAPI referenceButton, float size, int keyBind, boolean dissabled) {
        ButtonAPI newButton = createPanelButton(name, size, buttonOfReference.getPosition().getHeight(), keyBind, dissabled, creator).two;

        mainParent.addComponent(newButton).inTL(buttonOfReference.getPosition().getX() + buttonOfReference.getPosition().getWidth() - referenceButton.getPosition().getX() + 1, 0);
        mainParent.bringComponentToTop(newButton);
    }
    private void insertButton(float x, float y,float height, UIPanelAPI mainParent, String name, TooltipMakerAPI.TooltipCreator creator, float size, int keyBind, boolean dissabled) {
        ButtonAPI newButton = createPanelButton(name, size, height, keyBind, dissabled, creator).two;

        mainParent.addComponent(newButton).inTL(x-mainParent.getPosition().getX(), 0);
        mainParent.bringComponentToTop(newButton);
    }
    private Pair<CustomPanelAPI, ButtonAPI> createPanelButton(String buttonName, float width, float height, int bindingValue, boolean dissabled, TooltipMakerAPI.TooltipCreator onHoverTooltip) {
        CustomPanelAPI panel = Global.getSettings().createCustom(width, height, null);
        TooltipMakerAPI tooltipMakerAPI = panel.createUIElement(width, height, false);
        ButtonAPI button = tooltipMakerAPI.addButton(buttonName, null, Global.getSettings().getColor("buttonBg"), Global.getSettings().getColor("buttonBgDark"), Alignment.MID, CutStyle.TOP, width, height, 0f);
        button.setShortcut(bindingValue, false);
        button.setEnabled(!dissabled);
        if (onHoverTooltip != null) {
            tooltipMakerAPI.addTooltipToPrevious(onHoverTooltip, TooltipMakerAPI.TooltipLocation.BELOW);

        }
        panel.addUIElement(tooltipMakerAPI).inTL(0, 0);
        return new Pair(panel, button);
    }
}
