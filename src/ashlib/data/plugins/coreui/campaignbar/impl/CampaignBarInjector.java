package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.api.TabSpec;
import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.FieldRef;
import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Inserts registered tabs as a synchronous, reversible transaction. */
public final class CampaignBarInjector {

    private static final CoreUITabId[] ORDER = {
        CoreUITabId.CHARACTER,
        CoreUITabId.FLEET,
        CoreUITabId.REFIT,
        CoreUITabId.CARGO,
        CoreUITabId.MAP,
        CoreUITabId.INTEL,
        CoreUITabId.OUTPOSTS
    };

    public interface Sink {
        void event(String message);

        void failure(String operation, Throwable error);

        void snapshot(String phase, JSONObject data);
    }

    public interface Click {
        void open(String id);
    }

    private CampaignBarInjector() {}

    public static Session install(
            TabSpec spec,
            UIPanelAPI core,
            String layout,
            JSONObject before,
            Runnable click,
            Sink sink) {
        return install(spec, core, layout, before, click, sink, CampaignFrameSurface.NATIVE);
    }

    static Session install(
            TabSpec spec,
            UIPanelAPI core,
            String layout,
            JSONObject before,
            Runnable click,
            Sink sink,
            CampaignFrameSurface.Factory visuals) {
        return install(spec, core, layout, before, click, sink, visuals, false, 0);
    }

    public static Session installNavigable(
            TabSpec spec,
            UIPanelAPI core,
            String layout,
            JSONObject before,
            Runnable click,
            Sink sink,
            int index) {
        return install(
                spec, core, layout, before, click, sink, CampaignFrameSurface.NATIVE, true, index);
    }

    static Session install(
            TabSpec spec,
            UIPanelAPI core,
            String layout,
            JSONObject before,
            Runnable click,
            Sink sink,
            CampaignFrameSurface.Factory visuals,
            boolean fixed,
            int index) {
        return install(
                spec,
                core,
                layout,
                before,
                click,
                sink,
                visuals,
                fixed,
                index,
                CampaignNavigableBar.NATIVE,
                CampaignNavigableBar.NONE);
    }

    static Session install(
            TabSpec spec,
            UIPanelAPI core,
            String layout,
            JSONObject before,
            Runnable click,
            Sink sink,
            CampaignFrameSurface.Factory visuals,
            boolean fixed,
            int index,
            CampaignNavigableBar.Factory assets,
            CampaignNavigableBar.Checkpoint checkpoint) {
        return install(
                Collections.singletonList(spec),
                core,
                layout,
                before,
                new Click() {
                    public void open(String id) {
                        click.run();
                    }
                },
                sink,
                visuals,
                fixed,
                index,
                assets,
                checkpoint);
    }

    public static Session install(
            List<TabSpec> specs,
            UIPanelAPI core,
            String layout,
            JSONObject before,
            Click click,
            Sink sink,
            boolean fixed,
            int index) {
        return install(
                specs,
                core,
                layout,
                before,
                click,
                sink,
                CampaignFrameSurface.NATIVE,
                fixed,
                index,
                CampaignNavigableBar.NATIVE,
                CampaignNavigableBar.NONE);
    }

    static Session install(
            List<TabSpec> specs,
            UIPanelAPI core,
            String layout,
            JSONObject before,
            Click click,
            Sink sink,
            CampaignFrameSurface.Factory visuals,
            boolean fixed,
            int index,
            CampaignNavigableBar.Factory assets,
            CampaignNavigableBar.Checkpoint checkpoint) {
        Session session = null;
        try {
            sink.event("INSERT_REQUEST layout=" + layout);
            sink.snapshot("BEFORE", before);
            session = new Session(specs, core, layout, before, click, sink, visuals);
            session.fixedNavigation = fixed;
            session.navigationAssets = assets;
            session.navigationCheckpoint = checkpoint;
            session.preflight();
            session.apply();
            require(
                    session.originalCoreChildren.equals(CampaignUiAccess.children(core)),
                    "CORE_LAYER_ORDER_CHANGED");
            session.verify(true);
            session.active = true;
            for (ButtonAPI button : session.created.values()) {
                button.setCustomData(session);
            }
            if (fixed) {
                session.applyNavigation();
                session.navigation.resumeIndex(index);
                session.synchronizeEnabled(false);
                require(session.navigation.isActive(), "NAVIGATION_NOT_COMMITTED");
                session.verifyFootprint();
            }
            JSONObject after = session.capture();
            if (!fixed) {
                session.verifySpace(after);
            }
            sink.snapshot("AFTER", after);
            sink.event(
                    "INSERTED layout="
                            + layout
                            + " buttonCount="
                            + session.collection.size()
                            + " originalInstancesPreserved=true safeInsertionCertified=false"
                            + " lastButton="
                            + session.collection.ids().get(session.collection.size() - 1)
                            + " frameWidth="
                            + session.bar.getPosition().getWidth()
                            + " metalFrame="
                            + session.frame.snapshot());
            return session;
        } catch (Throwable error) {
            sink.failure(
                    session != null && session.started ? "INSERT_FAILED" : "PREFLIGHT_ABORTED",
                    error);
            if (session != null && session.started) {
                try {
                    if (session.navigation != null) {
                        session.navigation.restore();
                        session.navigation = null;
                    }
                    session.rollback();
                    sink.snapshot("ROLLBACK", session.capture());
                    sink.event("ROLLBACK_CONFIRMED");
                } catch (Throwable rollback) {
                    error.addSuppressed(rollback);
                    sink.failure("ROLLBACK_FAILED_RESTART_REQUIRED", error);
                }
            } else if (session != null && session.frame != null) {
                session.frame.disposePrepared();
            }
            return null;
        }
    }

    public static final class Session {
        private final CampaignTabCollection collection;
        private final UIPanelAPI core;
        private final String layout;
        private final JSONObject before;
        private final Click click;
        private final Sink sink;
        private final CampaignFrameSurface.Factory visuals;
        private UIPanelAPI bar;
        private Map<Object, Object> forward, reverse;
        private FieldRef widthField, padField, lastField, accumulatorField;
        private MethodRef add;
        private final Map<String, Object> actions = new LinkedHashMap<String, Object>();
        private final Map<String, ButtonAPI> created = new LinkedHashMap<String, ButtonAPI>();
        private CampaignBarFrame frame;
        private final List<ButtonAPI> nativeButtons = new ArrayList<ButtonAPI>();
        private List<?> originalChildren;
        private List<?> originalCoreChildren;
        private float originalWidth,
                originalPad,
                originalBarWidth,
                originalBarHeight,
                firstOffset,
                firstY,
                height;
        private float width, gap, limit;
        private boolean started, active;
        private boolean fixedNavigation;
        private final Set<ButtonAPI> disabledByPolicy =
                Collections.newSetFromMap(new IdentityHashMap<ButtonAPI, Boolean>());
        private boolean navigationAttempted;
        private CampaignNavigableBar navigation;
        private CampaignNavigableBar.Factory navigationAssets = CampaignNavigableBar.NATIVE;
        private CampaignNavigableBar.Checkpoint navigationCheckpoint = CampaignNavigableBar.NONE;

        private Session(
                List<TabSpec> specs,
                UIPanelAPI core,
                String layout,
                JSONObject before,
                Click click,
                Sink sink,
                CampaignFrameSurface.Factory visuals) {
            this.collection = new CampaignTabCollection(specs);
            this.core = core;
            this.layout = layout;
            this.before = before;
            this.click = click;
            this.sink = sink;
            this.visuals = visuals;
        }

        public UIPanelAPI core() {
            return core;
        }

        public ButtonAPI button() {
            return collection.extension == null ? null : button(collection.extension.id);
        }

        public ButtonAPI button(String id) {
            return created.get(id);
        }

        public boolean isActive() {
            return active;
        }

        UIPanelAPI bar() {
            return bar;
        }

        int navigationIndex() {
            return navigation == null ? 0 : navigation.model.index();
        }

        boolean navigable() {
            return navigation != null && navigation.isActive();
        }

        private boolean nativeEnabled() {
            for (ButtonAPI b : nativeButtons) {
                if (!b.isEnabled()) {
                    return false;
                }
            }
            return true;
        }

        private void synchronizeEnabled(boolean blocked) {
            boolean allowed = !blocked && nativeEnabled();
            for (ButtonAPI b : created.values()) {
                if (!allowed && b.isEnabled()) {
                    b.setEnabled(false);
                    disabledByPolicy.add(b);
                } else if (allowed && disabledByPolicy.remove(b)) {
                    b.setEnabled(true);
                }
            }
        }

        void revealButton() {
            if (collection.extension != null) {
                revealButton(collection.extension.id);
            }
        }

        void revealButton(String id) {
            if (navigation != null) {
                navigation.reveal(collection.indexOf(id));
            }
        }

        /** Applies optional navigation and handles restoration after failure. */
        void considerNavigation(boolean blocked) {
            if (fixedNavigation) {
                synchronizeEnabled(blocked);
            }
            if (navigation != null) {
                navigation.blocked(blocked);
                navigation.advance(
                        org.lwjgl.input.Mouse.isCreated()
                                && (org.lwjgl.input.Mouse.isButtonDown(0)
                                        || org.lwjgl.input.Mouse.isButtonDown(1)));
                if (navigation.isRestored()) {
                    navigation = null;
                    if (fixedNavigation) {
                        restore();
                    } // Fixed navigation must not expose the expanded staging layout.
                }
                return;
            }
            if (navigationAttempted || !active || blocked) {
                return;
            }
            if (org.lwjgl.input.Mouse.isCreated()
                    && (org.lwjgl.input.Mouse.isButtonDown(0)
                            || org.lwjgl.input.Mouse.isButtonDown(1))) {
                return;
            }
            navigationAttempted = true;
            CampaignFrameSurface prepared = null;
            try {
                require("119/6".equals(layout), "NAV_REQUIRES_119_6");
                verify(true);
                prepared = frame.prepareNavigation(visuals);
                List<ButtonAPI> order = collection.buttons(forward);
                navigation =
                        new CampaignNavigableBar(
                                core,
                                bar,
                                order,
                                forward,
                                reverse,
                                accumulatorField,
                                lastField,
                                frame.decoration(),
                                prepared,
                                sink,
                                CampaignNavigableBar.NATIVE,
                                CampaignNavigableBar.NONE);
                navigation.retainSession(this);
                navigation.apply();
                prepareHotkeys();
            } catch (Throwable error) {
                sink.failure("NAV_PREFLIGHT_OR_APPLY_FAILED_BASELINE_FALLBACK", error);
                if (navigation == null) {
                    if (prepared != null) {
                        prepared.dispose();
                    }
                } else if (navigation.isRestored()) {
                    navigation = null;
                }
            }
        }

        /** Applies navigation and hotkeys before the next render. */
        private void applyNavigation() throws Exception {
            require("119/6".equals(layout), "NAV_REQUIRES_119_6");
            verify(true);
            CampaignFrameSurface prepared = frame.prepareNavigation(visuals);
            try {
                navigation =
                        new CampaignNavigableBar(
                                core,
                                bar,
                                collection.buttons(forward),
                                forward,
                                reverse,
                                accumulatorField,
                                lastField,
                                frame.decoration(),
                                prepared,
                                sink,
                                navigationAssets,
                                navigationCheckpoint);
                navigation.retainSession(this);
                navigation.apply();
                prepareHotkeys();
                navigationAttempted = true;
            } catch (Throwable error) {
                if (navigation == null) {
                    prepared.dispose();
                }
                throw new Exception("FIXED_NAV_TRANSACTION", error);
            }
        }

        private void prepareHotkeys() {
            for (TabSpec tabSpec : collection.extensions) {
                navigation.prepareHotkey(tabSpec, created.get(tabSpec.id));
            }
        }

        @SuppressWarnings("unchecked")
        private void preflight() throws Exception {
            require(
                    fixedNavigation || !collection.extensions.isEmpty(),
                    "NO_EXTENSIONS_REQUIRES_NAVIGATION");
            require(
                    CampaignCompatibility.GAME_VERSION.equals(before.optString("gameVersion")),
                    "UNSUPPORTED_GAME_VERSION");
            if ("119/6".equals(layout)) {
                width = 119;
                gap = 6;
            } else if ("122/3".equals(layout)) {
                width = 122;
                gap = 3;
            } else {
                throw new IllegalStateException("UNKNOWN_LAYOUT " + layout);
            }
            require(
                    "SNAPSHOT_ONLY".equals(before.optString("status"))
                            && !before.optBoolean("truncated", true),
                    "INCOMPLETE_SNAPSHOT");
            require(
                    fixedNavigation
                            || !"NATIVE_BAR_SUBTREE".equals(before.optString("snapshotScope")),
                    "SCOPED_SNAPSHOT_CANNOT_AUTHORIZE_EXPANSION");
            if (fixedNavigation) {
                require(
                        "NATIVE_BAR_SUBTREE".equals(before.optString("snapshotScope")),
                        "FIXED_SNAPSHOT_REQUIRED");
            }
            Object candidate = CampaignUiAccess.read(core, "getButtons");
            require(candidate instanceof UIPanelAPI, "MISSING_BAR_GETTER");
            bar = (UIPanelAPI) candidate;
            require(CampaignUiAccess.children(core).contains(bar), "BAR_NOT_DIRECT_CHILD");
            originalCoreChildren = CampaignUiAccess.children(core);
            Object registry = CampaignUiAccess.read(bar, "getButtons");
            require(registry instanceof Map, "MISSING_REGISTRY");
            forward = (Map<Object, Object>) registry;
            require(forward.size() == 7, "FOREIGN_OR_DUPLICATE_BUTTONS");
            require(plainMap(forward), "UNKNOWN_MAP_IMPLEMENTATION");
            for (CoreUITabId id : ORDER) {
                Object button = forward.get(id);
                require(button instanceof ButtonAPI, "MISSING_ID " + id);
                require(
                        fixedNavigation || ((ButtonAPI) button).isEnabled(),
                        "NATIVE_TABS_DISABLED_OR_MODAL_ACTIVE");
                require(!nativeButtons.contains(button), "DUPLICATE_NATIVE_IDENTITY");
                nativeButtons.add((ButtonAPI) button);
            }
            originalChildren = CampaignUiAccess.children(bar);
            require(
                    originalChildren.size() == 8 && originalChildren.containsAll(nativeButtons),
                    "UNEXPECTED_BAR_CHILDREN");
            int decorations = 0;
            for (Object child : originalChildren) {
                if (!nativeButtons.contains(child)) {
                    require(
                            child instanceof UIComponentAPI && !(child instanceof ButtonAPI),
                            "UNKNOWN_BAR_CONTROL");
                    sink.event(
                            "FRAME_CANDIDATE data="
                                    + CampaignFrameDiagnostics.candidate(
                                            (UIComponentAPI) child, bar));
                    frame = new CampaignBarFrame((UIComponentAPI) child, bar);
                    decorations++;
                }
            }
            require(decorations == 1, "UNEXPECTED_FRAME_PROFILE");
            for (MethodRef method : CampaignHandleAccess.methods(bar.getClass())) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (!method.isBridge()
                        && method.getName().equals("addButton")
                        && method.getReturnType() == void.class
                        && parameterTypes.length == 4
                        && parameterTypes[0] == String.class
                        && parameterTypes[1] == Object.class
                        && !parameterTypes[2].isPrimitive()
                        && parameterTypes[3].isInterface()) {
                    require(add == null, "AMBIGUOUS_ADD_BUTTON");
                    add = method;
                }
            }
            require(add != null, "NO_NATIVE_ADD_BUTTON");
            originalBarWidth = bar.getPosition().getWidth();
            originalBarHeight = bar.getPosition().getHeight();
            for (FieldRef field : CampaignUiAccess.fields(bar.getClass())) {
                if (field.getName().equals("BUTTON_WIDTH") && field.getType() == float.class) {
                    require(widthField == null, "AMBIGUOUS_WIDTH");
                    widthField = field;
                }
                if (field.getName().equals("BUTTON_PAD") && field.getType() == float.class) {
                    require(padField == null, "AMBIGUOUS_PAD");
                    padField = field;
                }
                if (Map.class.isAssignableFrom(field.getType())) {
                    Object value = field.read(bar);
                    if (value instanceof Map && value != forward && isReverse((Map<?, ?>) value)) {
                        require(reverse == null, "AMBIGUOUS_REVERSE_REGISTRY");
                        reverse = (Map<Object, Object>) value;
                    }
                }
                if (UIComponentAPI.class.isAssignableFrom(field.getType())
                        && field.read(bar) == nativeButtons.get(6)) {
                    require(lastField == null, "AMBIGUOUS_LAST_BUTTON");
                    lastField = field;
                }
                if (field.getType() == float.class
                        && !CampaignHandleAccess.isPublic(field.getModifiers())
                        && near(((Number) field.read(bar)).floatValue(), originalBarWidth)) {
                    require(accumulatorField == null, "AMBIGUOUS_WIDTH_ACCUMULATOR");
                    accumulatorField = field;
                }
            }
            require(
                    widthField != null
                            && padField != null
                            && lastField != null
                            && accumulatorField != null
                            && reverse != null,
                    "INCOMPLETE_BAR_STATE");
            require(plainMap(reverse), "UNKNOWN_REVERSE_MAP");
            for (FieldRef field :
                    new FieldRef[] {widthField, padField, lastField, accumulatorField}) {
                require(
                        !CampaignHandleAccess.isFinal(field.getModifiers())
                                && field.getDeclaringClass() == bar.getClass(),
                        "FOREIGN_OR_IMMUTABLE_BAR_PROFILE");
            }
            originalWidth = ((Number) widthField.read(bar)).floatValue();
            originalPad = ((Number) padField.read(bar)).floatValue();
            require(
                    near(originalWidth, 122) && near(originalPad, 6),
                    "NATIVE_LAYOUT_ALREADY_MODIFIED");
            PositionAPI first = nativeButtons.get(0).getPosition();
            firstOffset = number(CampaignUiAccess.read(first, "getXAlignOffset"));
            firstY = number(CampaignUiAccess.read(first, "getYAlignOffset"));
            height = first.getHeight();
            require(near(height, 21) && firstOffset >= 0, "UNEXPECTED_BUTTON_PROFILE");
            verify(false);
            require(
                    near(originalBarWidth, firstOffset + 7 * (originalWidth + originalPad)),
                    "ACCUMULATOR_GEOMETRY_MISMATCH");
            require(near(firstOffset, 13) && near(firstY, 4), "UNKNOWN_FRAME_BUTTON_OFFSETS");
            frame.prepare(collection.size(), width, gap, visuals);
            limit =
                    (float)
                            Math.min(
                                    before.getDouble("logicalWidth"),
                                    core.getPosition().getX() + core.getPosition().getWidth());
            if (!fixedNavigation) {
                limit =
                        Math.min(
                                limit,
                                (float)
                                        (before.getDouble("rightmostButtonRight")
                                                + before.getDouble(
                                                        "trailingSpaceToObservedObstruction")));
            }
            JSONObject clip = before.getJSONObject("barDeclaredAncestorClipModel");
            require(!clip.getBoolean("unknownAncestorCapability"), "UNKNOWN_CLIPPING");
            limit = Math.min(limit, (float) (clip.getDouble("x") + clip.getDouble("width")));
            float right = first.getX() + collection.size() * width + (collection.size() - 1) * gap;
            float frameRight =
                    frame.projectedRight(firstOffset + collection.size() * (width + gap));
            if (fixedNavigation) {
                require(
                        "119/6".equals(layout)
                                && near(originalBarWidth, 909)
                                && near(originalBarHeight, 34),
                        "UNKNOWN_FIXED_FOOTPRINT");
                verifyFootprint();
                sink.event(
                        "FIXED_FOOTPRINT_PREFLIGHT width=914 originalLayerOrder=true expansionAuthorized=false");
            } else {
                require(
                        right + 8 <= limit
                                && bar.getPosition().getX()
                                                + firstOffset
                                                + collection.size() * (width + gap)
                                        <= limit,
                        "INSUFFICIENT_GEOMETRIC_SPACE");
                require(frameRight + 2 <= limit, "INSUFFICIENT_FRAME_SPACE");
            }
            sink.event(
                    "FRAME_PLAN before="
                            + frame.snapshot()
                            + " projectedRight="
                            + frameRight
                            + " margin="
                            + (limit - frameRight));
            for (final TabSpec spec : collection.extensions) {
                actions.put(
                        spec.id,
                        CampaignRuntimeBridge.actionListener(
                                add.getParameterTypes()[3],
                                new Runnable() {
                                    public void run() {
                                        ButtonAPI b = created.get(spec.id);
                                        if (active
                                                && b != null
                                                && b.isEnabled()
                                                && nativeEnabled()) {
                                            sink.event("BUTTON_CLICK id=" + spec.id);
                                            click.open(spec.id);
                                        }
                                    }
                                }));
            }
            sink.event(
                    "PREFLIGHT_OK layout="
                            + layout
                            + " projectedButtonRight="
                            + right
                            + " observedLimit="
                            + limit
                            + " margin="
                            + (limit - right)
                            + " clippingAndSpritesRequireRuntimeValidation=true");
        }

        private void verifyFootprint() throws Exception {
            PositionAPI p = bar.getPosition();
            JSONObject clip = before.getJSONObject("barDeclaredAncestorClipModel");
            require(near(p.getWidth(), 909) && near(p.getHeight(), 34), "FIXED_BAR_SIZE_CHANGED");
            require(
                    p.getX() >= clip.getDouble("x")
                            && p.getY() >= clip.getDouble("y")
                            && p.getX() + 914 <= limit
                            && p.getY() + 35
                                    <= Math.min(
                                            before.getDouble("logicalHeight"),
                                            clip.getDouble("y") + clip.getDouble("height")),
                    "FIXED_FOOTPRINT_CLIPPED");
        }

        private boolean isReverse(Map<?, ?> map) {
            if (map.size() != 7) {
                return false;
            }
            for (int i = 0; i < 7; i++) {
                if (map.get(nativeButtons.get(i)) != ORDER[i]) {
                    return false;
                }
            }
            return true;
        }

        private void apply() throws Exception {
            started = true;
            widthField.write(bar, Float.valueOf(width));
            padField.write(bar, Float.valueOf(gap));
            // The native factory initializes each button and both registry mappings.
            for (TabSpec spec : collection.extensions) {
                add.invoke(bar, spec.title, spec.id, null, actions.get(spec.id));
                Object value = forward.get(spec.id);
                require(value instanceof ButtonAPI, "NATIVE_ADD_DID_NOT_REGISTER");
                require(
                        !nativeButtons.contains(value) && !created.containsValue(value),
                        "NATIVE_BUTTON_REPLACED");
                created.put(spec.id, (ButtonAPI) value);
            }
            List<ButtonAPI> order = collection.buttons(forward);
            // Detach all factory anchors before reordering to prevent sibling dependency cycles.
            for (ButtonAPI b : order) {
                detach(b);
            }
            for (int i = 0; i < order.size(); i++) {
                PositionAPI position = order.get(i).getPosition();
                position.setSize(width, height);
                if (i == 0) {
                    position.inBL(firstOffset, firstY);
                } else {
                    position.rightOfMid(order.get(i - 1), gap);
                }
            }
            lastField.write(bar, order.get(order.size() - 1));
            float accumulated = firstOffset + collection.size() * (width + gap);
            accumulatorField.write(bar, Float.valueOf(accumulated));
            bar.getPosition().setSize(accumulated, originalBarHeight);
            frame.apply(accumulated);
        }

        private void detach(ButtonAPI button) {
            PositionAPI p = button.getPosition(), b = bar.getPosition();
            p.inBL(p.getX() - b.getX(), p.getY() - b.getY());
        }

        private void verify(boolean inserted) throws Exception {
            int count = inserted ? collection.size() : ORDER.length;
            require(forward.size() == count && reverse.size() == count, "REGISTRY_SIZE_CHANGED");
            List<ButtonAPI> order = new ArrayList<ButtonAPI>(nativeButtons);
            if (inserted) {
                for (TabSpec spec : collection.extensions) {
                    require(
                            forward.get(spec.id) == created.get(spec.id)
                                    && spec.id.equals(reverse.get(created.get(spec.id))),
                            "EXTENSION_REGISTRY_MISMATCH");
                }
                order = collection.buttons(forward);
            }
            float w = inserted ? width : originalWidth, g = inserted ? gap : originalPad;
            List<?> children = CampaignUiAccess.children(bar);
            require(
                    children.size()
                                    == originalChildren.size()
                                            + (inserted ? collection.extensions.size() + 1 : 0)
                            && children.containsAll(originalChildren),
                    "BAR_CHILDREN_CHANGED");
            for (int i = 0; i < 7; i++) {
                require(
                        forward.get(ORDER[i]) == nativeButtons.get(i)
                                && reverse.get(nativeButtons.get(i)) == ORDER[i],
                        "NATIVE_IDENTITY_CHANGED " + ORDER[i]);
            }
            float x = bar.getPosition().getX() + firstOffset;
            for (int i = 0; i < order.size(); i++) {
                PositionAPI p = order.get(i).getPosition();
                require(
                        children.contains(order.get(i)) && !p.isSuspendRecompute(),
                        "DETACHED_OR_SUSPENDED_POSITION");
                require(
                        CampaignUiAccess.read(p, "getParent") == bar.getPosition(),
                        "POSITION_PARENT_CHANGED");
                require(
                        CampaignUiAccess.read(p, "getBase")
                                == (i == 0 ? null : order.get(i - 1).getPosition()),
                        "ANCHOR_CHAIN_CHANGED");
                require(
                        near(p.getWidth(), w)
                                && near(p.getHeight(), height)
                                && near(p.getX(), x)
                                && near(p.getY(), bar.getPosition().getY() + firstY),
                        "GEOMETRY_MISMATCH index=" + i);
                require(
                        near(
                                        number(CampaignUiAccess.read(p, "getXAlignOffset")),
                                        i == 0 ? firstOffset : g)
                                && near(
                                        number(CampaignUiAccess.read(p, "getYAlignOffset")),
                                        i == 0 ? firstY : 0),
                        "ALIGNMENT_OFFSET_CHANGED");
                x += w + g;
            }
            require(
                    lastField.read(bar) == order.get(order.size() - 1),
                    "LAST_BUTTON_NOT_FINAL_ENTRY");
            require(
                    near(number(widthField.read(bar)), w) && near(number(padField.read(bar)), g),
                    "LAYOUT_STATE_CHANGED");
            require(
                    near(number(accumulatorField.read(bar)), bar.getPosition().getWidth()),
                    "ACCUMULATOR_MISMATCH");
            frame.verify(inserted, bar.getPosition().getWidth());
        }

        private void verifySpace(JSONObject after) throws Exception {
            require(
                    "SNAPSHOT_ONLY".equals(after.optString("status"))
                            && !after.getBoolean("truncated"),
                    "AFTER_SNAPSHOT_INCOMPLETE");
            float right = (float) after.getDouble("rightmostButtonRight");
            require(
                    right + 8 <= limit
                            && after.getDouble("trailingSpaceToObservedObstruction") >= 8,
                    "AFTER_COLLISION");
            require(
                    frame.projectedRight(bar.getPosition().getWidth()) + 2 <= limit,
                    "AFTER_FRAME_COLLISION");
        }

        private JSONObject capture() throws Exception {
            CampaignBarProbe probe = new CampaignBarProbe();
            if (fixedNavigation) {
                return probe.captureBarLayout(
                                core,
                                before.getString("context"),
                                before.getString("gameVersion"),
                                (float) before.getDouble("logicalWidth"),
                                (float) before.getDouble("logicalHeight"),
                                (float) before.getDouble("pixelWidth"),
                                (float) before.getDouble("pixelHeight"),
                                (float) before.getDouble("uiScale"))
                        .put("fixedFootprintWidth", 914)
                        .put(
                                "activeNavigation",
                                navigation == null ? JSONObject.NULL : navigation.geometry())
                        .put(
                                "expandedFrameIsHiddenStagingOnly",
                                navigation != null && navigation.isActive())
                        .put("barFrameVisual", frame.snapshot());
            }
            return probe.captureLayout(
                            core,
                            before.getString("context"),
                            before.getString("gameVersion"),
                            (float) before.getDouble("logicalWidth"),
                            (float) before.getDouble("logicalHeight"),
                            (float) before.getDouble("pixelWidth"),
                            (float) before.getDouble("pixelHeight"),
                            (float) before.getDouble("uiScale"))
                    .put("barFrameVisual", frame.snapshot());
        }

        /** Restores the layout only while this session still owns it. */
        public boolean restore() {
            return restore(false);
        }

        boolean retire() {
            return restore(true);
        }

        private boolean restore(boolean retired) {
            if (!started) {
                return true;
            }
            active = false;
            if (navigation != null) {
                try {
                    if (retired) {
                        navigation.restoreRetired();
                    } else {
                        navigation.restore();
                    }
                    navigation = null;
                } catch (Throwable error) {
                    if (retired) {
                        navigation.abandon();
                        frame.disposePrepared();
                        for (ButtonAPI b : created.values()) {
                            if (b.getCustomData() == this) {
                                b.setCustomData(null);
                            }
                        }
                    }
                    sink.failure("NAV_RESTORE_REFUSED_RESTART_REQUIRED", error);
                    return false;
                }
            }
            try {
                verify(true);
            } catch (Throwable changed) {
                sink.failure("RESTORE_REFUSED_FOREIGN_CHANGES_RESTART_REQUIRED", changed);
                return false;
            }
            try {
                rollback();
                if (!retired) {
                    sink.snapshot("REMOVED", capture());
                }
                sink.event(retired ? "RETIRED_BAR_RELEASED" : "REMOVAL_CONFIRMED");
                return true;
            } catch (Throwable error) {
                sink.failure("RESTORE_FAILED_RESTART_REQUIRED", error);
                return false;
            }
        }

        /** Handles decoration failures during update, outside the render pass. */
        void checkVisualHealth() {
            if (!active || frame.renderFailure() == null) {
                return;
            }
            sink.failure("FRAME_RENDER_FAILED", frame.renderFailure());
            restore();
        }

        private void rollback() throws Exception {
            active = false;
            frame.restore();
            // Include a button attached by a native factory that threw before either registry was
            // updated.
            List<ButtonAPI> extras = new ArrayList<ButtonAPI>();
            for (Object child : CampaignUiAccess.children(bar)) {
                if (!originalChildren.contains(child)) {
                    require(child instanceof ButtonAPI, "UNEXPECTED_PARTIAL_CHILD");
                    extras.add((ButtonAPI) child);
                }
            }
            require(
                    extras.size() <= collection.extensions.size(),
                    "UNEXPECTED_PARTIAL_CHILDREN_COUNT");
            for (ButtonAPI extra : extras) {
                detach(extra);
            }
            for (ButtonAPI b : nativeButtons) {
                detach(b);
            }
            for (int i = 0; i < 7; i++) {
                PositionAPI p = nativeButtons.get(i).getPosition();
                p.setSize(originalWidth, height);
                if (i == 0) {
                    p.inBL(firstOffset, firstY);
                } else {
                    p.rightOfMid(nativeButtons.get(i - 1), originalPad);
                }
            }
            for (TabSpec spec : collection.extensions) {
                Object registered = forward.remove(spec.id);
                if (registered != null) {
                    reverse.remove(registered);
                }
            }
            for (ButtonAPI extra : extras) {
                reverse.remove(extra);
                extra.setCustomData(null);
                bar.removeComponent(extra);
            }
            widthField.write(bar, Float.valueOf(originalWidth));
            padField.write(bar, Float.valueOf(originalPad));
            lastField.write(bar, nativeButtons.get(6));
            accumulatorField.write(bar, Float.valueOf(originalBarWidth));
            bar.getPosition().setSize(originalBarWidth, originalBarHeight);
            verify(false);
            created.clear();
            disabledByPolicy.clear();
            started = false;
        }
    }

    private static boolean plainMap(Map<?, ?> map) {
        return map.getClass() == HashMap.class || map.getClass() == LinkedHashMap.class;
    }

    private static boolean near(float a, float b) {
        return Float.isFinite(a) && Float.isFinite(b) && Math.abs(a - b) < 0.1f;
    }

    private static float number(Object value) {
        if (!(value instanceof Number)) {
            throw new IllegalStateException("Missing numeric capability");
        }
        return ((Number) value).floatValue();
    }

    private static void require(boolean ok, String reason) {
        if (!ok) {
            throw new IllegalStateException(reason);
        }
    }
}
