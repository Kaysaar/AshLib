package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.api.TabSpec;
import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.FieldRef;
import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import org.lwjgl.input.Mouse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Adds navigation layout while preserving native button identities and Core registries. */
final class CampaignNavigableBar implements CampaignViewportBridge.Hooks {
    static final int WIDTH = 914;
    static final int BAR_WIDTH = 909;
    static final int ARROW_WIDTH = 44, ARROW_HEIGHT = 17;
    static final int VIEW_X = 13;
    // Logical offsets inside TRIPAD's native decoration, which follows Core tab layering.
    static final int LEFT_X = 116, RIGHT_X = 164, ARROW_Y = 3;

    interface Factory {
        Assets create(UIPanelAPI bar, Class<?> button, Runnable left, Runnable right)
                throws Exception;
    }

    interface Checkpoint {
        void reached(String phase);
    }

    static final Checkpoint NONE =
            new Checkpoint() {
                public void reached(String phase) {}
            };

    static final class Assets {
        final CampaignViewportContract contract;
        final UIPanelAPI track;
        final ButtonAPI left;
        final ButtonAPI right;
        final CampaignArrowIcons icons;

        Assets(
                CampaignViewportContract c,
                UIPanelAPI t,
                ButtonAPI l,
                ButtonAPI r,
                CampaignArrowIcons i) {
            contract = c;
            track = t;
            left = l;
            right = r;
            icons = i;
        }
    }

    static final Factory NATIVE =
            new Factory() {
                public Assets create(UIPanelAPI bar, Class<?> button, Runnable left, Runnable right)
                        throws Exception {
                    try {
                        // Create arrows on an unattached bar to preserve live registries and
                        // last-button state.
                        UIPanelAPI scratch =
                                (UIPanelAPI)
                                        MethodHandles.publicLookup()
                                                .findConstructor(
                                                        bar.getClass(),
                                                        MethodType.methodType(void.class))
                                                .invokeWithArguments();
                        FieldRef width = null;
                        MethodRef add = null;
                        for (FieldRef field : CampaignUiAccess.fields(scratch.getClass())) {
                            if (field.getName().equals("BUTTON_WIDTH")
                                    && field.getType() == float.class) {
                                width = field;
                            }
                        }
                        for (MethodRef method : CampaignHandleAccess.methods(scratch.getClass())) {
                            if (method.getName().equals("addButton") && !method.isBridge()) {
                                need(add == null, "AMBIGUOUS_ARROW_FACTORY");
                                add = method;
                            }
                        }
                        need(
                                width != null && add != null && add.getParameterTypes().length == 4,
                                "NO_ARROW_FACTORY");

                        width.write(scratch, (float) ARROW_WIDTH);
                        Object l = CampaignRuntimeBridge.actionListener(
                                add.getParameterTypes()[3], left);
                        Object r = CampaignRuntimeBridge.actionListener(
                                add.getParameterTypes()[3], right);
                        add.invoke(scratch, "", "ashlib.poc.arrow.left", null, l);
                        add.invoke(scratch, "", "ashlib.poc.arrow.right", null, r);

                        Map<?, ?> registry =
                                (Map<?, ?>) CampaignUiAccess.read(scratch, "getButtons");
                        ButtonAPI lb = (ButtonAPI) registry.get("ashlib.poc.arrow.left");
                        ButtonAPI rb = (ButtonAPI) registry.get("ashlib.poc.arrow.right");
                        lb.getPosition().setSize(ARROW_WIDTH, ARROW_HEIGHT);
                        rb.getPosition().setSize(ARROW_WIDTH, ARROW_HEIGHT);
                        lb.getPosition().inBL(0, ARROW_Y);
                        rb.getPosition().inBL(0, ARROW_Y);
                        scratch.removeComponent(lb);
                        scratch.removeComponent(rb);
                        need(
                                lb.getClass() == button && rb.getClass() == button,
                                "ARROW_BUTTON_TYPE");
                        need(
                                CampaignUiAccess.read(lb, "getShortcut") == null
                                        && CampaignUiAccess.read(rb, "getShortcut") == null,
                                "ARROW_HAS_SHORTCUT");

                        CustomPanelAPI sample = Global.getSettings().createCustom(800, 40, null);
                        TooltipMakerAPI tip = sample.createUIElement(CampaignViewportModel.VIEW_WIDTH, 35, true);
                        sample.addUIElement(tip);
                        ScrollPanelAPI scroller = tip.getExternalScroller();
                        need(scroller != null, "NO_NATIVE_SCROLLER");
                        CampaignViewportContract contract =
                                new CampaignViewportContract(
                                        scroller.getClass(),
                                        button,
                                        Global.getSettings().getVersionString());
                        sample.removeComponent(scroller);
                        UIPanelAPI track = Global.getSettings().createCustom(994, 35, null);
                        return new Assets(
                                contract, track, lb, rb, CampaignArrowIcons.create(lb, rb));
                    } catch (Throwable error) {
                        throw new Exception("Navigation assets unavailable", error);
                    }
                }
            };

    final CampaignViewportModel model;
    private final UIPanelAPI core;
    private final UIPanelAPI bar;
    private final UIPanelAPI arrowOwner;
    private final List<?> arrowOwnerChildren;
    private final List<?> coreChildren;
    private final Map<Object, Object> forward;
    private final Map<Object, Object> reverse;
    private final Map<Object, Object> forwardCopy;
    private final Map<Object, Object> reverseCopy;
    private final FieldRef accumulator;
    private final FieldRef last;
    private final Object lastValue;
    private final Object accumulatorValue;
    private final CampaignBarReparent movement;
    private final UIComponentAPI baselineFrame;
    private final CampaignFrameSurface frame;
    private final CampaignBarInjector.Sink sink;
    private final Checkpoint checkpoint;
    private final float width;
    private final float height;
    private final float baselineOpacity;
    private final Assets assets;
    private final ScrollPanelAPI viewport;
    private final MethodHandle eventCtor;
    private final MethodHandle mouseX;
    private final MethodHandle mouseY;
    private final MethodRef arrowActive;
    private final MethodRef arrowSetActive;
    private final List<InputEventAPI> insideEvents = new ArrayList<InputEventAPI>();
    private final List<InputEventAPI> arrowEvents = new ArrayList<InputEventAPI>();

    private int pending = -1;
    private int lastX = -100000;
    private int lastY = -100000;
    private boolean started;
    private boolean active;
    private boolean restored;
    private boolean failed;
    private boolean blocked;
    private boolean committed;

    // The live viewport retains its session even when there are no extension buttons.
    private CampaignBarInjector.Session session;
    private long lastBlockedLog;
    private final Map<ButtonAPI, CampaignBarHotkey> hotkeys =
            new IdentityHashMap<ButtonAPI, CampaignBarHotkey>();
    private final Set<ButtonAPI> hotkeyAttempted =
            Collections.newSetFromMap(new IdentityHashMap<ButtonAPI, Boolean>());

    void retainSession(CampaignBarInjector.Session owner) {
        need(!started && session == null && owner != null, "SESSION_OWNER_ALREADY_ATTACHED");
        session = owner;
    }

    void prepareHotkey(TabSpec spec, ButtonAPI button) {
        if (!active || !hotkeyAttempted.add(button)) {
            return;
        }
        if (spec.key == 0) {
            return;
        }
        try {
            installHotkey(CampaignBarHotkey.prepare(core, bar, button, spec));
        } catch (Throwable error) {
            sink.failure("HOTKEY_UNAVAILABLE_CLICK_PRESERVED", error);
        }
    }

    void installHotkey(CampaignBarHotkey prepared) throws Exception {
        need(
                active && !hotkeys.containsKey(prepared.button()),
                "HOTKEY_ALREADY_INSTALLED_OR_INACTIVE");
        prepared.apply();
        hotkeys.put(prepared.button(), prepared);
        movement.expectRenderer(prepared.button(), prepared.renderer());
        sink.event("HOTKEY_INSTALLED nativeRenderer=true nativeDispatch=true unmodifiedOnly=true");
    }

    void reveal(int slot) {
        if (active && !model.visible(slot)) {
            pending = model.nearestIndex(slot);
            advance(false);
        }
    }

    void resumeIndex(int index) {
        if (active) {
            pending = Math.max(0, Math.min(model.maxIndex(), index));
            advance(false);
        }
    }

    CampaignNavigableBar(
            UIPanelAPI core,
            UIPanelAPI bar,
            List<ButtonAPI> buttons,
            Map<Object, Object> forward,
            Map<Object, Object> reverse,
            FieldRef accumulator,
            FieldRef last,
            UIComponentAPI baselineFrame,
            CampaignFrameSurface frame,
            CampaignBarInjector.Sink sink,
            Factory factory,
            Checkpoint checkpoint)
            throws Exception {
        this.model = new CampaignViewportModel(buttons.size());
        this.core = core;
        this.bar = bar;
        arrowOwner = findTripadDecoration(core, bar);
        arrowOwnerChildren = CampaignUiAccess.children(arrowOwner);
        this.forward = forward;
        this.reverse = reverse;
        this.accumulator = accumulator;
        this.last = last;
        this.baselineFrame = baselineFrame;
        this.frame = frame;
        this.sink = sink;
        this.checkpoint = checkpoint;
        coreChildren = CampaignUiAccess.children(core);
        forwardCopy = new HashMap<Object, Object>(forward);
        reverseCopy = new HashMap<Object, Object>(reverse);
        lastValue = last.read(bar);
        accumulatorValue = accumulator.read(bar);
        width = bar.getPosition().getWidth();
        height = bar.getPosition().getHeight();
        baselineOpacity = baselineFrame.getOpacity();

        need(
                forward.size() == buttons.size()
                        && reverse.size() == buttons.size()
                        && near(width, 13 + buttons.size() * 125)
                        && near(baselineOpacity, 1),
                "NOT_REGISTERED_BAR_BASELINE");
        need(
                frame.plan.width == 914
                        && near(baselineFrame.getPosition().getWidth(), 18 + buttons.size() * 125),
                "NOT_119_6_SLICED_FRAME");
        movement = new CampaignBarReparent(bar, buttons);
        need(
                movement.children.size() == buttons.size() + 2
                        && movement.children.get(0) == baselineFrame,
                "FOREIGN_BAR_DECORATION");

        assets = factory.create(
                bar,
                buttons.get(0).getClass(),
                new Runnable() {
                    public void run() {
                        step(-1);
                    }
                },
                new Runnable() {
                    public void run() {
                        step(1);
                    }
                });

        try {
            arrowActive = CampaignUiAccess.method(assets.left.getClass(), "isActive");
            arrowSetActive =
                    CampaignUiAccess.method(assets.left.getClass(), "setActive", boolean.class);
            need(
                    arrowActive.getReturnType() == boolean.class
                            && arrowSetActive.getReturnType() == void.class
                            && Boolean.TRUE.equals(arrowActive.invoke(assets.left))
                            && Boolean.TRUE.equals(arrowActive.invoke(assets.right)),
                    "ARROW_INPUT_GATE_UNAVAILABLE");
            need(
                    !assets.left.isPerformActionWhenDisabled()
                            && !assets.right.isPerformActionWhenDisabled()
                            && !Boolean.TRUE.equals(
                                    CampaignUiAccess.read(assets.left, "isRightClicksOkWhenDisabled"))
                            && !Boolean.TRUE.equals(
                                    CampaignUiAccess.read(assets.right, "isRightClicksOkWhenDisabled")),
                    "ARROW_DISABLED_ACTION_UNSAFE");
            need(
                    CampaignUiAccess.method(bar.getClass(), "addComponent", UIComponentAPI.class)
                                    .getDeclaringClass()
                            == CampaignUiAccess.method(
                                            assets.track.getClass(),
                                            "addComponent",
                                            UIComponentAPI.class)
                                    .getDeclaringClass(),
                    "FOREIGN_REPARENT_IMPLEMENTATION");

            try {
                viewport =
                        (ScrollPanelAPI)
                                assets.contract.viewportConstructor.invokeWithArguments(this);
                viewport.getPosition().setSize(CampaignViewportModel.VIEW_WIDTH, 35);
                call(
                        viewport,
                        "setContentSize",
                        new Class<?>[] {float.class, float.class},
                        (float) model.trackWidth(),
                        35f);
                call(viewport, "setShowScrollbars", new Class<?>[] {boolean.class}, false);
                call(viewport, "setUseMWheel", new Class<?>[] {boolean.class}, false);
                call(viewport, "setMaxShadowHeight", new Class<?>[] {float.class}, 0f);
                call(viewport, "setUseSimpleShadows", new Class<?>[] {boolean.class}, true);
                assets.track.getPosition().setSize(model.trackWidth(), 35);
                viewport.addComponent(assets.track).inBL(0, 0);

                eventCtor =
                        MethodHandles.publicLookup()
                                .findConstructor(
                                        assets.contract.event,
                                        MethodType.methodType(
                                                void.class,
                                                InputEventClass.class,
                                                InputEventType.class,
                                                int.class,
                                                int.class,
                                                int.class,
                                                char.class));
                mouseX =
                        MethodHandles.publicLookup()
                                .findStatic(
                                        assets.contract.event,
                                        "getMouseX",
                                        MethodType.methodType(int.class));
                mouseY =
                        MethodHandles.publicLookup()
                                .findStatic(
                                        assets.contract.event,
                                        "getMouseY",
                                        MethodType.methodType(int.class));
            } catch (Throwable error) {
                throw new Exception("Navigation viewport capability", error);
            }

            need(
                    CampaignUiAccess.read(assets.left, "getShortcut") == null
                            && CampaignUiAccess.read(assets.right, "getShortcut") == null,
                    "ARROW_SHORTCUT");
            need(
                    near(assets.left.getPosition().getWidth(), ARROW_WIDTH)
                            && near(assets.right.getPosition().getWidth(), ARROW_WIDTH),
                    "ARROW_SIZE");

            verifyRegistry();
            movement.verifyOriginal();
            sink.event(
                    "NAV_PREFLIGHT_OK restoreProfile=BASELINE_PARENTS_ANCHORS_REGISTRIES width=914 viewport=890 arrows=44x17 location=TRIPAD noNewShortcuts=true");
        } catch (Throwable error) {
            assets.icons.dispose();
            frame.dispose();
            throw new Exception("Navigation preparation", error);
        }
    }

    void apply() throws Exception {
        started = true;
        try {
            baselineFrame.setOpacity(0);
            bar.addComponent(frame.panel).inBL(0, 0);
            checkpoint.reached("FRAME");
            bar.addComponent(viewport).inBL(VIEW_X, 0);
            checkpoint.reached("VIEWPORT");

            movement.moveTo(
                    assets.track,
                    new Runnable() {
                        public void run() {
                            checkpoint.reached("MOVE");
                        }
                    });

            arrowOwner.addComponent(assets.left).inBL(LEFT_X, ARROW_Y);
            checkpoint.reached("LEFT");
            arrowOwner.addComponent(assets.right).inBL(RIGHT_X, ARROW_Y);
            checkpoint.reached("RIGHT");
            assets.icons.panel.getPosition().setSize(RIGHT_X - LEFT_X + ARROW_WIDTH, ARROW_HEIGHT);
            arrowOwner.addComponent(assets.icons.panel).inBL(LEFT_X, ARROW_Y);
            checkpoint.reached("ICONS");

            accumulator.write(bar, (float) BAR_WIDTH);
            bar.getPosition().setSize(BAR_WIDTH, height);
            checkpoint.reached("SIZE");

            active = true;
            updateArrows();
            verify();
            need(
                    coreChildren.equals(CampaignUiAccess.children(core)),
                    "CORE_LAYER_CHANGED_DURING_INSTALL");
            checkpoint.reached("VERIFIED");

            committed = true;
            sink.event(
                    "NAV_INSTALLED "
                            + geometry()
                            + " realButtons=true registryWrites=0 arrowIcons=WHITE_TRIANGLES"
                            + " limitFeedback=NATIVE_DISABLED_BUTTON modalInput=BLOCKED");
        } catch (Throwable error) {
            active = false;
            try {
                restore();
            } catch (Throwable rollback) {
                error.addSuppressed(rollback);
            }
            throw new Exception("Navigation apply failed", error);
        }
    }

    boolean isActive() {
        return active;
    }

    boolean isRestored() {
        return restored;
    }

    void blocked(boolean value) {
        blocked = value;
    }

    void step(int direction) {
        if (!active || blocked || failed) {
            return;
        }

        int base = pending < 0 ? model.index() : pending;
        int target = base + direction;
        if (target < 0 || target > model.maxIndex()) {
            return;
        }

        pending = target;
        sink.event("NAV_ARROW_CLICK direction=" + direction + " target=" + target);
    }

    void advance(boolean mouseDown) {
        if (!active) {
            return;
        }
        try {
            if (frame.failure() != null) {
                throw new IllegalStateException("NAV_FRAME_RENDER", frame.failure());
            }
            if (assets.icons.failure() != null) {
                throw new IllegalStateException("NAV_ARROW_ICON_RENDER", assets.icons.failure());
            }
            if (failed) {
                throw new IllegalStateException("NAV_INPUT_FAILURE");
            }

            verify();
            updateArrows();
            if (pending < 0 || mouseDown || blocked) {
                return;
            }

            cancelButtons(); // Release owned input capture without changing selection.
            model.setIndex(pending);
            pending = -1;
            viewport.setXOffset(model.offset());

            if (Mouse.isCreated()) {
                lastX = (Integer) mouseX.invokeWithArguments();
                lastY = (Integer) mouseY.invokeWithArguments();
            }
            InputEventAPI move =
                    (InputEventAPI)
                            eventCtor.invokeWithArguments(
                                    InputEventClass.MOUSE_EVENT,
                                    InputEventType.MOUSE_MOVE,
                                    0,
                                    0,
                                    0,
                                    '\0');
            call(move, "setX", new Class<?>[] {int.class}, lastX);
            call(move, "setY", new Class<?>[] {int.class}, lastY);
            viewport.processInput(Collections.singletonList(move));

            updateArrows();
            verify();
            sink.event("NAV_INDEX_APPLIED " + geometry());
        } catch (Throwable error) {
            sink.failure("NAV_RUNTIME_FAILED", error);
            try {
                restore();
            } catch (Throwable rollback) {
                sink.failure("NAV_RESTORE_FAILED_RESTART_REQUIRED", rollback);
            }
        }
    }

    private void updateArrows() throws Exception {
        boolean allowed = active && !blocked && !failed;
        for (ButtonAPI b : movement.buttons) {
            allowed &= b.isEnabled();
        }
        updateArrow(assets.left, allowed && model.index() > 0, allowed);
        updateArrow(assets.right, allowed && model.index() < model.maxIndex(), allowed);
    }

    private void updateArrow(ButtonAPI arrow, boolean enabled, boolean contextAllowsInput)
            throws Exception {
        // Limits keep native disabled-click feedback; modal contexts block all arrow input.
        boolean interactive = Boolean.TRUE.equals(arrowActive.invoke(arrow));
        if (arrow.isEnabled() != enabled || interactive != contextAllowsInput) {
            cancel(arrow);
        }
        // RC8 rearms mouseEnter on every setEnabled(false), so write only on transitions.
        if (arrow.isEnabled() != enabled) {
            arrow.setMouseOverSound(enabled ? "ui_button_mouseover" : null);
            arrow.setEnabled(enabled);
        }
        if (interactive != contextAllowsInput) {
            arrowSetActive.invoke(arrow, contextAllowsInput);
        }
    }

    private void verifyRegistry() throws Exception {
        verifyRegistry(false);
    }

    private void verifyRegistry(boolean retired) throws Exception {
        need(
                (retired || CampaignUiAccess.read(core, "getButtons") == bar)
                        && CampaignUiAccess.read(bar, "getButtons") == forward,
                "BAR_REGISTRY_OWNER");
        need(
                forward.equals(forwardCopy)
                        && reverse.equals(reverseCopy)
                        && last.read(bar) == lastValue,
                "REGISTRY_OR_LAST_CHANGED");
        // Native navigation may replace other Core children; validate only the owned bar.
        need(
                retired || CampaignUiAccess.inspectChildren(core).contains(bar),
                "BAR_DETACHED_FROM_CORE");
        movement.verifyIdentity();
    }

    void verify() throws Exception {
        verifyStructure();
        need(
                near(viewport.getXOffset(), model.offset())
                        && near(viewport.getYOffset(), 0)
                        && !Boolean.TRUE.equals(CampaignUiAccess.read(viewport, "isScrolling")),
                "UNEXPECTED_SCROLL");
    }

    private void verifyStructure() throws Exception {
        verifyStructure(false);
    }

    private void verifyStructure(boolean retired) throws Exception {
        for (CampaignBarHotkey hotkey : hotkeys.values()) {
            hotkey.verify();
        }
        verifyRegistry(retired);
        movement.verifyAt(assets.track, 0);
        need(
                near(bar.getPosition().getWidth(), 909)
                        && near(((Number) accumulator.read(bar)).floatValue(), 909),
                "BAR_DIMENSIONS_CHANGED");
        need(
                near(viewport.getPosition().getX(), bar.getPosition().getX() + VIEW_X)
                        && near(viewport.getPosition().getY(), bar.getPosition().getY()),
                "VIEWPORT_POSITION");
        need(
                near(baselineFrame.getOpacity(), 0) && near(frame.panel.getOpacity(), 1),
                "FRAME_OPACITY_CHANGED");
        List<?> now = CampaignUiAccess.inspectChildren(bar);
        need(now.size() == 4 && now.contains(frame.panel) && now.contains(viewport),
                "BAR_CHILDREN_CHANGED");
        need(CampaignUiAccess.read(viewport, "getParent") == bar
                        && CampaignUiAccess.read(frame.panel, "getParent") == bar,
                "OWNED_PARENT_CHANGED");
        need(near(viewport.getPosition().getWidth(), CampaignViewportModel.VIEW_WIDTH)
                        && near(viewport.getPosition().getHeight(), 35)
                        && near(frame.panel.getPosition().getWidth(), 914)
                        && near(frame.panel.getPosition().getHeight(), 35),
                "OWNED_SIZE_CHANGED");
        verifyArrowOwner(retired);
        List<?> tracks =
                CampaignUiAccess.inspectChildren(
                        CampaignUiAccess.read(viewport, "getContentContainer"));
        need(
                tracks.size() == 1
                        && tracks.get(0) == assets.track
                        && CampaignUiAccess.inspectChildren(assets.track).size() == model.count(),
                "TRACK_CHANGED");
    }

    void restore() throws Exception {
        restore(false);
    }

    /** Retires the old bar without touching its replacement. */
    void restoreRetired() throws Exception {
        restore(true);
    }

    void abandon() {
        active = false;
        try {
            // A retired bar may share the surviving TRIPAD decoration with its replacement.
            // Remove only this session's controls even when foreign changes prevent full restore.
            removeArrowComponents();
        } catch (Exception error) {
            sink.failure("TRIPAD_RETIRE_CLEANUP_FAILED", error);
        }
        session = null;
        pending = -1;
        insideEvents.clear();
        frame.dispose();
        assets.icons.dispose();
    }

    private void restore(boolean retired) throws Exception {
        if (restored) {
            return;
        }
        active = false;
        pending = -1;
        if (!started) {
            frame.dispose();
            assets.icons.dispose();
            session = null;
            restored = true;
            return;
        }
        // Reject foreign registry or parent changes before restoring.
        verifyRegistry(retired);
        if (committed) {
            verifyStructure(retired);
        }
        cancelButtons();
        for (ButtonAPI arrow : Arrays.asList(assets.left, assets.right)) {
            cancel(arrow);
        }
        removeArrowComponents();
        need(arrowOwnerChildren.equals(CampaignUiAccess.children(arrowOwner)),
                "TRIPAD_RESTORE_CHILDREN_CHANGED");
        for (UIComponentAPI owned : Arrays.asList(viewport, frame.panel)) {
            if (CampaignUiAccess.children(bar).contains(owned)) bar.removeComponent(owned);
        }
        movement.restore();
        for (CampaignBarHotkey hotkey : hotkeys.values()) {
            hotkey.restore();
        }
        if (!hotkeys.isEmpty()) {
            hotkeys.clear();
            movement.expectRenderer(null, null);
            sink.event("HOTKEY_ROLLBACK_CONFIRMED");
        }
        accumulator.write(bar, accumulatorValue);
        bar.getPosition().setSize(width, height);
        baselineFrame.setOpacity(baselineOpacity);
        movement.verifyOriginal();
        verifyRegistry(retired);
        frame.dispose();
        assets.icons.dispose();
        insideEvents.clear();
        restored = true;
        session = null;
        sink.event(
                "NAV_ROLLBACK_BASELINE_CONFIRMED width="
                        + width
                        + " parentsAnchorsRegistries=true selectedStateUntouched=true");
    }

    private static UIPanelAPI findTripadDecoration(UIPanelAPI core, UIPanelAPI bar) throws Exception {
        final String asset = "graphics/ui/tripad_bot_left_decor_extended.png";
        UIPanelAPI found = null;
        for (Object value : CampaignUiAccess.inspectChildren(core)) {
            if (!(value instanceof UIPanelAPI) || value == bar) continue;
            UIPanelAPI panel = (UIPanelAPI) value;
            PositionAPI p = panel.getPosition();
            if (CampaignUiAccess.read(p, "getBase") != bar.getPosition()
                    || CampaignUiAccess.read(p, "getParent") != core.getPosition()) continue;
            boolean matches = false;
            for (Object child : CampaignUiAccess.inspectChildren(panel)) {
                Object sprite = CampaignUiAccess.read(child, "getSprite");
                Object texture = CampaignUiAccess.read(sprite, "getTexture");
                JSONArray fields = CampaignFrameDiagnostics.textureStrings(texture);
                for (int i = 0; i < fields.length(); i++) {
                    JSONObject field = fields.getJSONObject(i);
                    if (asset.equals(field.optString("value", ""))) matches = true;
                }
            }
            if (!matches) continue;
            need(found == null, "AMBIGUOUS_TRIPAD_DECORATION");
            need(p.getWidth() >= RIGHT_X + ARROW_WIDTH && p.getHeight() >= ARROW_Y + ARROW_HEIGHT
                            && !Boolean.TRUE.equals(CampaignUiAccess.read(panel, "isClipping"))
                            && CampaignUiAccess.inspectChildren(panel).size() == 1,
                    "UNSUPPORTED_TRIPAD_DECORATION");
            need(CampaignUiAccess.method(panel.getClass(), "addComponent", UIComponentAPI.class)
                            .getDeclaringClass()
                            == CampaignUiAccess.method(bar.getClass(), "addComponent", UIComponentAPI.class)
                            .getDeclaringClass(), "FOREIGN_TRIPAD_PARENT_IMPLEMENTATION");
            found = panel;
        }
        need(found != null, "TRIPAD_DECORATION_UNAVAILABLE");
        return found;
    }

    private void verifyArrowOwner(boolean retired) throws Exception {
        need(retired || (CampaignUiAccess.read(arrowOwner, "getParent") == core
                        && CampaignUiAccess.inspectChildren(core).contains(arrowOwner)),
                "TRIPAD_DETACHED_FROM_CORE");
        List<?> children = CampaignUiAccess.inspectChildren(arrowOwner);
        int base = arrowOwnerChildren.size();
        need(children.size() == base + 3 && children.subList(0, base).equals(arrowOwnerChildren)
                        && children.get(base) == assets.left && children.get(base + 1) == assets.right
                        && children.get(base + 2) == assets.icons.panel,
                "TRIPAD_CHILDREN_CHANGED");
        need(CampaignUiAccess.read(assets.left, "getParent") == arrowOwner
                        && CampaignUiAccess.read(assets.right, "getParent") == arrowOwner
                        && CampaignUiAccess.read(assets.icons.panel, "getParent") == arrowOwner,
                "ARROW_PARENT_CHANGED");
        PositionAPI p = arrowOwner.getPosition();
        need(near(assets.left.getPosition().getX(), p.getX() + LEFT_X)
                        && near(assets.right.getPosition().getX(), p.getX() + RIGHT_X)
                        && near(assets.left.getPosition().getY(), p.getY() + ARROW_Y)
                        && near(assets.right.getPosition().getY(), p.getY() + ARROW_Y)
                        && near(assets.left.getPosition().getWidth(), ARROW_WIDTH)
                        && near(assets.right.getPosition().getWidth(), ARROW_WIDTH)
                        && near(assets.left.getPosition().getHeight(), ARROW_HEIGHT)
                        && near(assets.right.getPosition().getHeight(), ARROW_HEIGHT)
                        && near(assets.icons.panel.getPosition().getX(), p.getX() + LEFT_X)
                        && near(assets.icons.panel.getPosition().getY(), p.getY() + ARROW_Y)
                        && near(assets.icons.panel.getPosition().getWidth(), RIGHT_X - LEFT_X + ARROW_WIDTH)
                        && near(assets.icons.panel.getPosition().getHeight(), ARROW_HEIGHT),
                "ARROW_GEOMETRY_CHANGED");
    }

    private void removeArrowComponents() throws Exception {
        for (UIComponentAPI owned : Arrays.asList(assets.icons.panel, assets.left, assets.right)) {
            if (CampaignUiAccess.read(owned, "getParent") == arrowOwner) arrowOwner.removeComponent(owned);
        }
    }

    private void cancelButtons() throws Exception {
        for (ButtonAPI b : movement.buttons) {
            cancel(b);
        }
    }

    private void cancel(ButtonAPI b) throws Exception {
        call(b, "forceRemoveTooltip", new Class<?>[0]);
        assets.contract.cancelCapture.invoke(CampaignUiAccess.read(b, "getLogic"));
    }

    String geometry() {
        return "index="
                + model.index()
                + " visible="
                + model.index()
                + ".."
                + (model.index() + CampaignViewportModel.VISIBLE - 1)
                + " frame=914 viewportX=13 viewportWidth=890 arrows=TRIPAD offset="
                + viewport.getXOffset()
                + " leftEnabled="
                + assets.left.isEnabled()
                + " rightEnabled="
                + assets.right.isEnabled();
    }

    public void before(List<?> events) {
        insideEvents.clear();
        if (!active) {
            return;
        }
        try {
            // The campaign resource HUD consumes mouse events across the TRIPAD strip.
            // This viewport is dispatched before that HUD, so let the existing decoration
            // process its own arrows first. Keep the original events: native consumption
            // prevents a second activation when Core later dispatches the decoration.
            // The decoration also retains its native visibility/fade and modal gates.
            updateArrows();
            arrowEvents.clear();
            for (Object value : events) {
                InputEventAPI e = (InputEventAPI) value;
                if (!e.isConsumed() && e.isMouseEvent()) {
                    arrowEvents.add(e);
                }
            }
            if (!arrowEvents.isEmpty()) {
                // Include moves and releases outside the arrows for native hover/capture
                // cancellation. Native hit-testing leaves unrelated events untouched.
                arrowOwner.processInput(arrowEvents);
            }
            for (Object value : events) {
                InputEventAPI e = (InputEventAPI) value;
                if (e.isConsumed() || !e.isMouseEvent()) {
                    continue;
                }
                lastX = e.getX();
                lastY = e.getY();
                PositionAPI position = viewport.getPosition();
                if (!CampaignViewportModel.contains(
                        position.getX(),
                        position.getY(),
                        position.getWidth(),
                        position.getHeight(),
                        lastX,
                        lastY)) {
                    continue;
                }
                insideEvents.add(e);
                if (e.isMouseScrollEvent() || e.isRMBEvent()) {
                    e.consume();
                    long now = System.nanoTime();
                    if (now - lastBlockedLog > 750000000L) {
                        lastBlockedLog = now;
                        sink.event("NAV_SCROLL_DRAG_BLOCKED");
                    }
                }
            }
        } catch (Throwable error) {
            failure(error);
        } finally {
            arrowEvents.clear();
        }
    }

    public void after(List<?> events) {
        for (InputEventAPI e : insideEvents) {
            if (!e.isConsumed()) {
                e.consume();
            }
        }
        insideEvents.clear();
    }

    public void mouse(int slot, boolean entered) {}

    public void press(int slot, int state) {}

    public boolean activation(int slot, InputEventAPI event) {
        return false;
    } // Native buttons handle activation; the viewport does not dispatch it.

    public void failure(Throwable error) {
        if (!failed) {
            failed = true;
            sink.failure("NAV_INPUT_FAILED", error);
        }
    }

    private static Object call(Object o, String m, Class<?>[] p, Object... args) throws Exception {
        return CampaignUiAccess.call(o, m, p, args);
    }

    private static void need(boolean value, String why) {
        CampaignBarReparent.need(value, why);
    }

    private static boolean near(float a, float b) {
        return CampaignBarReparent.near(a, b);
    }
}
