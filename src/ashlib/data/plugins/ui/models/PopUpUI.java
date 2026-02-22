package ashlib.data.plugins.ui.models;

import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.reflection.ReflectionBetterUtilis;
import ashlib.data.plugins.ui.plugins.UILinesRenderer;
import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.state.AppDriver;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

public class PopUpUI implements CustomUIPanelPlugin {

    public int limit = 10;
    public float totalFrames;
    public IntervalUtil betweenCodex = null;
    public boolean detectedCodex = false;
    public boolean attemptedExit = false;
    protected Fader fader = null; // Purple Nebula

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

        @SuppressWarnings("unchecked")
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
                Object[] methods = currentClass.getDeclaredMethods();

                for (Object method : methods) {
                    try {
                        MethodHandle getParameterTypesHandle =
                                ReflectionBetterUtilis.getParameterTypesHandle(method.getClass(), "getParameterTypes");

                        if (getMethodNameHandle.invoke(method).equals(methodName)) {
                            Class<?>[] parameterTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);
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

        public static Object invokeMethodWithAutoProjection(String methodName, Object instance, Object... arguments) {
            Pair<Object, Class<?>[]> methodPair = getMethodFromSuperclass(methodName, instance);

            if (methodPair == null) {
                try {
                    throw new NoSuchMethodException("Method " + methodName + " not found in class hierarchy of " + instance.getClass().getName());
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            Object method = methodPair.one;
            Class<?>[] parameterTypes = methodPair.two;

            Object[] projectedArgs = new Object[parameterTypes.length];
            for (int index = 0; index < parameterTypes.length; index++) {
                Object arg = (arguments.length > index) ? arguments[index] : null;

                if (arg == null) {
                    if (parameterTypes[index].isPrimitive()) {
                        throw new IllegalArgumentException("Argument at index " + index + " cannot be null for primitive type " + parameterTypes[index].getName());
                    }
                    projectedArgs[index] = null;
                } else {
                    projectedArgs[index] = convertArgument(arg, parameterTypes[index]);
                }
            }

            try {
                setMethodAccessable.invoke(method, true);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            try {
                return invokeMethodHandle.invoke(method, instance, projectedArgs);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public static Object convertArgument(Object arg, Class<?> targetType) {
            if (targetType.isAssignableFrom(arg.getClass())) {
                return arg;
            } else if (targetType.isPrimitive()) {
                if (targetType == int.class) return ((Number) arg).intValue();
                if (targetType == long.class) return ((Number) arg).longValue();
                if (targetType == double.class) return ((Number) arg).doubleValue();
                if (targetType == float.class) return ((Number) arg).floatValue();
                if (targetType == short.class) return ((Number) arg).shortValue();
                if (targetType == byte.class) return ((Number) arg).byteValue();
                if (targetType == boolean.class) return arg;
                if (targetType == char.class) return arg;
                throw new IllegalArgumentException("Unsupported primitive type: " + targetType.getName());
            } else {
                return targetType.cast(arg);
            }
        }
    }

    public static List<UIComponentAPI> getChildren(UIPanelAPI panelAPI) {
        return ReflectionUtilis.getChildrenCopy(panelAPI);
    }

    protected static class ProductionUtil {
        public static UIPanelAPI getCoreUI() {
            CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
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

        public static UIPanelAPI getCoreUIForDialog() {
            CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
            Object dialog = campaignUI.getCurrentInteractionDialog();

            if (AppDriver.getInstance().getCurrentState() instanceof CampaignState) {
                dialog = ReflectionUtilis.invokeMethod("getEncounterDialog", AppDriver.getInstance().getCurrentState());
            }

            if (dialog != null) {
                return (UIPanelAPI) ReflectionUtilis.invokeMethod("getParent", dialog);
            }

            return null;
        }

        public static UIPanelAPI getCurrentTab() {
            UIPanelAPI coreUltimate = getCoreUI();
            if (coreUltimate == null) return null;
            UIPanelAPI core = (UIPanelAPI) ReflectionUtilis.invokeMethod("getCurrentTab", coreUltimate);
            return core == null ? null : core;
        }
    }

    SpriteAPI blackBackground = Global.getSettings().getSprite("rendering", "GlitchSquare");
    SpriteAPI borders = Global.getSettings().getSprite("rendering", "GlitchSquare");
    SpriteAPI panelBackground = Global.getSettings().getSprite("ui", "panel00_center");
    SpriteAPI bot = Global.getSettings().getSprite("ui", "panel00_bot");
    SpriteAPI top = Global.getSettings().getSprite("ui", "panel00_top");
    SpriteAPI left = Global.getSettings().getSprite("ui", "panel00_left");
    SpriteAPI right = Global.getSettings().getSprite("ui", "panel00_right");
    SpriteAPI topLeft = Global.getSettings().getSprite("ui", "panel00_top_left");
    SpriteAPI topRight = Global.getSettings().getSprite("ui", "panel00_top_right");
    SpriteAPI bottomLeft = Global.getSettings().getSprite("ui", "panel00_bot_left");
    SpriteAPI bottomRight = Global.getSettings().getSprite("ui", "panel00_bot_right");

    public static float buttonConfirmWidth = 160;

    UIPanelAPI parentUIPanel;
    public float frames;
    public CustomPanelAPI panelToInfluence;

    public ArrayList<TooltipMakerAPI> mainTooltips = new ArrayList<>();
    public ArrayList<CustomPanelAPI> mainPanels = new ArrayList<>();

    public UILinesRenderer rendererBorder = new UILinesRenderer(0f);
    public List<UILinesRenderer> internalLinesRenderers = new ArrayList<>(); // Purple Nebula

    public ButtonAPI confirmButton, cancelButton;
    public String confirmButtonText = "Confirm";
    public String cancelButtonText = "Cancel";

    public boolean isDialog = true;
    public boolean confirmOnly = false;

    public boolean reachedMaxHeight = false;
    public boolean pressedConfirmCancel = false; // Purple Nebula

    // Target size (expanded)
    float goalSizeX, goalSizeY;

    // Raw inputs (caller space)
    float x, y;

    // UI-space converted Y (this is the FIX)
    float initYUi;

    // Animation offsets (UI-space)
    float goalYOffset;
    float expandOffset;

    boolean didOnceOne = false;
    boolean didOnceTwo = false;

    public void addInternalLinesRenderer(UILinesRenderer internalLinesRenderer) {
        if (internalLinesRenderers == null) internalLinesRenderers = new ArrayList<>();
        internalLinesRenderers.add(internalLinesRenderer);
    }

    public void addTooltip(TooltipMakerAPI tooltipMakerAPI) {
        if (mainTooltips == null) mainTooltips = new ArrayList<>();
        mainTooltips.add(tooltipMakerAPI);
    }

    public void addPanel(CustomPanelAPI customPanelAPI) {
        if (mainPanels == null) mainPanels = new ArrayList<>();
        mainPanels.add(customPanelAPI);
    }

    // Purple Nebula
    public void removeUI() {
        internalLinesRenderers.clear();

        for (TooltipMakerAPI mainTooltip : mainTooltips) {
            mainTooltip.setOpacity(0f);
        }
        mainTooltips.clear();

        for (CustomPanelAPI mainPanel : mainPanels) {
            mainPanel.setOpacity(0f);
        }
        mainPanels.clear();

        if (confirmButton != null) confirmButton.setOpacity(0);
        if (cancelButton != null) cancelButton.setOpacity(0);
    }

    @Override
    public void positionChanged(PositionAPI position) {
    }

    public void init(CustomPanelAPI panelAPI, float x, float y, boolean isDialog) {
        panelToInfluence = panelAPI;
        parentUIPanel = ProductionUtil.getCoreUI();

        if (ReflectionUtilis.hasMethodOfName("getFader", this.panelToInfluence)) {
            this.fader = (Fader) ReflectionUtilis.invokeMethod("getFader", this.panelToInfluence);
        }

        goalSizeX = panelAPI.getPosition().getWidth();
        goalSizeY = panelAPI.getPosition().getHeight();

        panelToInfluence.getPosition().setSize(16, 16);
        this.isDialog = isDialog;

        this.x = x;
        this.y = y;

        // ===== FIX: convert incoming y to UI-space once, and use it everywhere =====
        // The parent UI panel's coordinate system is top-left origin. Mouse/screen is often bottom-left.
        // You already used (parentHeight - y) when placing; keep that consistent.
        initYUi = parentUIPanel.getPosition().getHeight() - y;

        parentUIPanel.addComponent(panelToInfluence).inTL(x, initYUi * 2f);
        parentUIPanel.bringComponentToTop(panelToInfluence);

        rendererBorder.setPanel(panelToInfluence);

        if (fader != null) fader.setBrightness(0.1f);

        // Reset animation flags if this instance is reused
        didOnceOne = false;
        didOnceTwo = false;
        reachedMaxHeight = false;
        pressedConfirmCancel = false;
        frames = 0f;
    }

    public void initForDialog(CustomPanelAPI panelAPI, float x, float y, boolean isDialog) {
        panelToInfluence = panelAPI;
        parentUIPanel = ProductionUtil.getCoreUIForDialog();

        if (ReflectionUtilis.hasMethodOfName("getFader", this.panelToInfluence)) {
            this.fader = (Fader) ReflectionUtilis.invokeMethod("getFader", this.panelToInfluence);
        }

        goalSizeX = panelAPI.getPosition().getWidth();
        goalSizeY = panelAPI.getPosition().getHeight();

        panelToInfluence.getPosition().setSize(16, 16);
        this.isDialog = isDialog;

        this.x = x;
        this.y = y;

        // ===== FIX: same conversion =====
        initYUi = parentUIPanel.getPosition().getHeight() - y;

        parentUIPanel.addComponent(panelToInfluence).inTL(x, initYUi * 2f);
        parentUIPanel.bringComponentToTop(panelToInfluence);

        rendererBorder.setPanel(panelToInfluence);

        if (fader != null) fader.setBrightness(0.1f);

        didOnceOne = false;
        didOnceTwo = false;
        reachedMaxHeight = false;
        pressedConfirmCancel = false;
        frames = 0f;
    }

    /**
     * Note: here is where you create UI : Methods you need to change is advance , createUI, and inputEvents handler.
     * Remember to also use super.apply()
     */
    public void createUI(CustomPanelAPI panelAPI) {
        // Implement in subclasses
    }

    public float createUIMockup(CustomPanelAPI panelAPI) {
        return 0f;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panelToInfluence != null) {
            TiledTextureRenderer renderer = new TiledTextureRenderer(panelBackground.getTextureId());

            if (isDialog) {
                // Parent core UI is safer than screen height for UI scaling
                UIPanelAPI core = ProductionUtil.getCoreUI();
                if (core != null) {
                    blackBackground.setSize(core.getPosition().getWidth(), core.getPosition().getHeight());
                    blackBackground.setColor(Color.black);
                    if (fader != null) blackBackground.setAlphaMult(fader.getBrightness() / 2f);
                    blackBackground.renderAtCenter(core.getPosition().getCenterX(), core.getPosition().getCenterY());
                }

                renderer.renderTiledTexture(
                        panelToInfluence.getPosition().getX(),
                        panelToInfluence.getPosition().getY(),
                        panelToInfluence.getPosition().getWidth(),
                        panelToInfluence.getPosition().getHeight(),
                        panelBackground.getTextureWidth(),
                        panelBackground.getTextureHeight(),
                        (frames / limit) * 0.9F,
                        Color.BLACK
                );
            } else {
                renderer.renderTiledTexture(
                        panelToInfluence.getPosition().getX(),
                        panelToInfluence.getPosition().getY(),
                        panelToInfluence.getPosition().getWidth(),
                        panelToInfluence.getPosition().getHeight(),
                        panelBackground.getTextureWidth(),
                        panelBackground.getTextureHeight(),
                        (frames / limit),
                        panelBackground.getColor()
                );
            }

            if (isDialog) {
                renderBorders(panelToInfluence);
            } else {
                rendererBorder.render(alphaMult);
            }
        }
    }

    @Override
    public void render(float alphaMult) {
        for (UILinesRenderer linesRenderer : internalLinesRenderers) {
            linesRenderer.render(alphaMult);
        }
    }

    @Override
    public void advance(float amount) {
        if (betweenCodex != null) {
            betweenCodex.advance(amount);
            if (betweenCodex.intervalElapsed()) {
                betweenCodex = null;
            }
        }

        // ===== Animation parameters in UI-space =====
        float dialogPanelHeight = goalSizeY;          // expanded height
        float yDistanceToExpand = dialogPanelHeight / 2f;

        // expanded position (in UI coords): panel's top-left y is initYUi
        float yPosExpandedUi = initYUi;
        float yPosRetractedUi = yPosExpandedUi + yDistanceToExpand;

        // Purple Nebula: initialize offsets once (FIX: keep in same space as inTL)
        if (!didOnceOne) {
            // Baseline align like v1 so the "retracted" start is stable
            panelToInfluence.getPosition().setYAlignOffset(-yPosExpandedUi * 2f);

            goalYOffset = -yPosExpandedUi;
            expandOffset = -yPosRetractedUi; // start "too low" then move up toward goalYOffset

            // If you want v1 exact movement, uncomment this instead:
            // expandOffset = goalYOffset * 2f;

            didOnceOne = true;
        }

        // ===== Open / expand =====
        if (!pressedConfirmCancel) {
            if (this.frames <= (float) this.limit) {
                ++this.frames;

                // move from retracted -> expanded over limit frames
                expandOffset += Math.abs(yDistanceToExpand / limit);

                float progress = this.frames / (float) this.limit;

                if (this.frames < (float) this.limit && !this.reachedMaxHeight) {
                    if (fader != null) {
                        fader.setDurationIn((float) limit / 20f);
                        fader.fadeIn();
                    }
                    this.panelToInfluence.getPosition().setYAlignOffset(expandOffset);
                    this.panelToInfluence.getPosition().setSize(this.goalSizeX, this.goalSizeY * progress);
                    return;
                }

                if (this.frames >= (float) this.limit && !this.reachedMaxHeight) {
                    this.reachedMaxHeight = true;
                    this.panelToInfluence.getPosition().setYAlignOffset(goalYOffset);
                    this.panelToInfluence.getPosition().setSize(this.goalSizeX, this.goalSizeY);
                    this.createUI(this.panelToInfluence);
                    return;
                }
            }
        }

        // ===== Handle buttons =====
        if (confirmButton != null && confirmButton.isChecked()) {
            confirmButton.setChecked(false);
            applyConfirmScript();
            pressedConfirmCancel = true;
        }

        if (cancelButton != null && cancelButton.isChecked()) {
            cancelButton.setChecked(false);
            pressedConfirmCancel = true;
        }

        // ===== Close / retract =====
        if (pressedConfirmCancel) {
            if (!didOnceTwo) {
                removeUI();
                this.frames = (float) limit;
                didOnceTwo = true;
            }

            if (this.frames >= 0) {
                --this.frames;

                // move back expanded -> retracted over limit frames
                expandOffset -= Math.abs(yDistanceToExpand / limit);

                float progress = this.frames / (float) (limit);
                if (this.frames > 0) {
                    if (fader != null) {
                        fader.setDurationOut(0.05f);
                        fader.fadeOut();
                    }

                    this.panelToInfluence.getPosition().setYAlignOffset(expandOffset);
                    this.panelToInfluence.getPosition().setSize(this.goalSizeX, this.goalSizeY * progress);
                    return;
                }
            }

            if (fader == null || !fader.isFadingOut()) {
                pressedConfirmCancel = false;
                if (parentUIPanel != null) parentUIPanel.removeComponent(this.panelToInfluence);
                this.onExit();
            }
        }

        // Codex tooltip mode gate
        if (Global.CODEX_TOOLTIP_MODE) {
            detectedCodex = true;
        }
        if (!Global.CODEX_TOOLTIP_MODE && detectedCodex) {
            detectedCodex = false;
            betweenCodex = new IntervalUtil(0.1f, 0.1f);
        }
    }

    public void applyConfirmScript() {
        // Implement in subclasses
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (betweenCodex != null) return;

        for (InputEventAPI event : events) {
            if (frames >= limit - 1 && reachedMaxHeight) {
                if (event.isMouseDownEvent() && !isDialog) {
                    TrapezoidButtonDetector detector = new TrapezoidButtonDetector();
                    float xLeft = panelToInfluence.getPosition().getX();
                    float xRight = panelToInfluence.getPosition().getX() + panelToInfluence.getPosition().getWidth();
                    float yBot = panelToInfluence.getPosition().getY();
                    float yTop = panelToInfluence.getPosition().getY() + panelToInfluence.getPosition().getHeight();
                    boolean hovers = detector.determineIfHoversOverButton(
                            xLeft, yTop, xRight, yTop, xLeft, yBot, xRight, yBot,
                            Global.getSettings().getMouseX(), Global.getSettings().getMouseY()
                    );
                    if (!hovers) {
                        if (cancelButton != null) cancelButton.setChecked(true);
                        else pressedConfirmCancel = true;
                    }
                }

                if (!event.isConsumed()) {
                    if (event.getEventValue() == Keyboard.KEY_ESCAPE && !event.isMouseEvent() && event.isKeyDownEvent()) {
                        if (cancelButton != null) cancelButton.setChecked(true);
                        else pressedConfirmCancel = true;

                        event.consume();
                        break;
                    }
                }
            }
            event.consume();
        }
    }

    public void forceDismiss() {
        if (parentUIPanel != null) parentUIPanel.removeComponent(panelToInfluence);
        onExit();
    }

    public void onExit() {
        // Implement in subclasses
    }

    @Override
    public void buttonPressed(Object buttonId) {
    }

    public void renderBorders(CustomPanelAPI panelAPI) {
        float leftX = panelAPI.getPosition().getX() + 16;
        float currAlpha = (frames / limit) * 0.9F;
        if (currAlpha >= 1) currAlpha = 1;

        top.setSize(16, 16);
        bot.setSize(16, 16);
        topLeft.setSize(16, 16);
        topRight.setSize(16, 16);
        bottomLeft.setSize(16, 16);
        bottomRight.setSize(16, 16);
        left.setSize(16, 16);
        right.setSize(16, 16);

        top.setAlphaMult(currAlpha);
        bot.setAlphaMult(currAlpha);
        topLeft.setAlphaMult(currAlpha);
        topRight.setAlphaMult(currAlpha);
        bottomLeft.setAlphaMult(currAlpha);
        bottomRight.setAlphaMult(currAlpha);
        left.setAlphaMult(currAlpha);
        right.setAlphaMult(currAlpha);

        float botX = panelAPI.getPosition().getY() + 16;

        AshMisc.startStencilWithXPad(panelAPI, 8);
        for (float i = leftX; i <= panelAPI.getPosition().getX() + panelAPI.getPosition().getWidth(); i += top.getWidth()) {
            top.renderAtCenter(i, panelAPI.getPosition().getY() + panelAPI.getPosition().getHeight());
            bot.renderAtCenter(i, panelAPI.getPosition().getY());
        }
        AshMisc.endStencil();

        AshMisc.startStencilWithYPad(panelAPI, 8);
        for (float i = botX; i <= panelAPI.getPosition().getY() + panelAPI.getPosition().getHeight(); i += top.getWidth()) {
            left.renderAtCenter(panelAPI.getPosition().getX(), i);
            right.renderAtCenter(panelAPI.getPosition().getX() + panelAPI.getPosition().getWidth(), i);
        }
        AshMisc.endStencil();

        topLeft.renderAtCenter(leftX - 16, panelAPI.getPosition().getY() + panelAPI.getPosition().getHeight());
        topRight.renderAtCenter(panelAPI.getPosition().getX() + panelAPI.getPosition().getWidth(), panelAPI.getPosition().getY() + panelAPI.getPosition().getHeight());
        bottomLeft.renderAtCenter(leftX - 16, panelAPI.getPosition().getY());
        bottomRight.renderAtCenter(panelAPI.getPosition().getX() + panelAPI.getPosition().getWidth(), panelAPI.getPosition().getY());
    }

    public ButtonAPI generateConfirmButton(TooltipMakerAPI tooltip) {
        ButtonAPI button = tooltip.addButton(
                confirmButtonText,
                "confirm",
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                Alignment.MID,
                CutStyle.TL_BR,
                160,
                25,
                0f
        );
        button.setShortcut(Keyboard.KEY_G, true);
        confirmButton = button;
        return button;
    }

    public ButtonAPI generateCancelButton(TooltipMakerAPI tooltip) {
        ButtonAPI button = tooltip.addButton(
                cancelButtonText,
                "cancel",
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                Alignment.MID,
                CutStyle.TL_BR,
                buttonConfirmWidth,
                25,
                0f
        );
        button.setShortcut(Keyboard.KEY_ESCAPE, true);
        cancelButton = button;
        return button;
    }

    public void createConfirmSection(CustomPanelAPI mainPanel) {
        float totalWidth = buttonConfirmWidth + 10;
        TooltipMakerAPI tooltip = mainPanel.createUIElement(totalWidth, 25, false);
        tooltip.setButtonFontOrbitron20();
        generateConfirmButton(tooltip);
        confirmButton.getPosition().inTL(0, 0);
        float bottom = goalSizeY;
        mainPanel.addUIElement(tooltip).inTL(mainPanel.getPosition().getWidth() - (totalWidth) - 10, bottom - 40);
    }

    public void createConfirmAndCancelSection(CustomPanelAPI mainPanel) {
        float totalWidth = buttonConfirmWidth * 2 + 10;
        TooltipMakerAPI tooltip = mainPanel.createUIElement(totalWidth, 25, false);
        tooltip.setButtonFontOrbitron20();
        generateConfirmButton(tooltip);
        generateCancelButton(tooltip);
        confirmButton.getPosition().inTL(0, 0);
        cancelButton.getPosition().inTL(buttonConfirmWidth + 5, 0);
        float bottom = goalSizeY;
        mainPanel.addUIElement(tooltip).inTL(mainPanel.getPosition().getWidth() - (totalWidth) - 10, bottom - 40);
    }

    public CustomPanelAPI getPanelToInfluence() {
        return panelToInfluence;
    }

    public ButtonAPI getConfirmButton() {
        return confirmButton;
    }

    public ButtonAPI getCancelButton() {
        return cancelButton;
    }

    public void setConfirmText(String confirmButtonText) {
        this.confirmButtonText = confirmButtonText;
    }

    public void setCancelText(String cancelButtonText) {
        this.cancelButtonText = cancelButtonText;
    }

    public void setConfirmOnly(boolean confirmOnly) {
        this.confirmOnly = confirmOnly;
    }
}