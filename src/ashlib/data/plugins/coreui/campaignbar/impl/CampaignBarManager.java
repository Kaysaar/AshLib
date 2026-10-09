package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.api.CampaignBar;
import ashlib.data.plugins.coreui.campaignbar.api.TabSpec;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.UIPanelAPI;

import org.json.JSONObject;
import org.lwjgl.input.Keyboard;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Manages campaign UI sessions in a transient script, outside saved sector state. */
public final class CampaignBarManager implements EveryFrameScript {
    public static final String BUILD = "ashlib-campaignbar-integration";
    private CampaignUiRefreshTrace refresh;
    private String refreshCheckpoint;
    private final List<TabSpec> specs =
            new CampaignTabCollection(CampaignBar.registrations()).extensions;
    private final Map<String, CampaignScreenHost> hosts =
            new LinkedHashMap<String, CampaignScreenHost>();
    private String requestedId;
    // A static entry must not retain an unloaded campaign.
    private static WeakReference<CampaignBarManager> live =
            new WeakReference<CampaignBarManager>(null);
    private static boolean enabled;
    private final CampaignStartupTrace startup =
            new CampaignStartupTrace(
                    new CampaignStartupTrace.Clock() {
                        public long nanoTime() {
                            return System.nanoTime();
                        }
                    },
                    new CampaignStartupTrace.Sink() {
                        public void event(String text) {
                            log(text);
                        }
                    });

    public CampaignBarManager() {
        for (final TabSpec spec : specs) {
            hosts.put(
                    spec.id,
                    new CampaignScreenHost(
                            spec.id,
                            new CampaignScreenHost.Sink() {
                                public void event(String message) {
                                    log("tab=" + spec.id + " " + message);
                                }

                                public void failure(String operation, Throwable error) {
                                    logFailure("tab=" + spec.id + " " + operation, error);
                                }
                            }));
        }
    }

    private CampaignScreenHost defaultHost() {
        return hosts.isEmpty() ? null : hosts.values().iterator().next();
    }

    private void resetHosts() {
        for (CampaignScreenHost host : hosts.values()) {
            host.reset();
        }
    }

    private final CampaignCapabilityGuard.Failures failures =
            new CampaignCapabilityGuard.Failures() {
                public void report(String operation, Throwable error) {
                    logFailure(operation, error);
                }
            };
    private final CampaignProbeRun.Sink probeSink =
            new CampaignProbeRun.Sink() {
                public void event(String message) {
                    probeLog(message);
                }

                public void failure(String operation, Throwable error) {
                    logFailure(operation, error);
                }
            };
    private final CampaignDevKeys keys =
            new CampaignDevKeys(
                    new CampaignDevKeys.Sink() {
                        public void event(String message) {
                            log(message);
                        }

                        public void probe() {
                            queueProbe("hotkey");
                        }

                        public void toggleHost() {
                            CampaignScreenHost host = defaultHost();
                            if (host == null) {
                                openResult("NO_REGISTERED_TABS");
                            } else if (host.isOpen()) {
                                host.requestClose("hotkey");
                            } else {
                                requestedButtonCore = null;
                                requestedId = specs.get(0).id;
                                openRequested = true;
                            }
                        }
                    });
    private final CampaignCapabilityGuard hostCapability =
            new CampaignCapabilityGuard("HOST", failures);
    private long discoveryRetryAt;
    private String discoveryFailure;
    private boolean probeRequested, openRequested, done;
    private boolean devHotkeys, autoProbePending;
    private String probeSource;
    private boolean functionalObserved;
    private int sample;
    private boolean enableBarInjection;
    private boolean enableNavigableBarPoc;
    private String barLayout;
    private WeakReference<UIPanelAPI> requestedButtonCore;
    private WeakReference<UIPanelAPI> requestedButtonBar;
    private WeakReference<UIPanelAPI> campaignCore = new WeakReference<UIPanelAPI>(null),
            dialogCore = new WeakReference<UIPanelAPI>(null);
    private int lastNavigationIndex;
    private final Map<UIPanelAPI, CampaignBarLifecycle> barStates =
            new WeakHashMap<UIPanelAPI, CampaignBarLifecycle>();
    // Sessions reference Core objects; weak values prevent retaining the weak keys.
    private final Map<UIPanelAPI, WeakReference<CampaignBarInjector.Session>> bars =
            new WeakHashMap<UIPanelAPI, WeakReference<CampaignBarInjector.Session>>();

    public static void onGameLoad() {
        CampaignBarManager previous = live.get();
        if (previous != null) {
            previous.done = true;
            previous.removeBars();
            previous.resetHosts();
        }
        live.clear();
        enabled = false;
        try {
            Global.getSector().removeTransientScriptsOfClass(CampaignBarManager.class);
            JSONObject config = Global.getSettings().getJSONObject("ashlibCampaignBar");
            enabled = config.optBoolean("enabled", false);
            if (!enabled) {
                return;
            }
            CampaignBarManager script = new CampaignBarManager();
            script.devHotkeys = config.optBoolean("enableDevHotkeys", false);
            script.autoProbePending = config.optBoolean("captureOnceAfterLoad", false);
            script.enableBarInjection = config.optBoolean("enableBarInjection", false);
            script.barLayout = config.optString("barLayout", "119/6");
            script.enableNavigableBarPoc = config.optBoolean("enableNavigableBarPoc", false);
            if (script.specs.isEmpty()
                    && (!script.enableBarInjection || !script.enableNavigableBarPoc)) {
                enabled = false;
                log("NO_REGISTRATION navigationDisabled=true vanillaUntouched=true");
                return;
            }
            if (config.optBoolean("enableUiRefreshDiagnostics", false)) {
                script.refresh =
                        new CampaignUiRefreshTrace(
                                new CampaignUiRefreshTrace.Sink() {
                                    public void event(String text) {
                                        log(text);
                                    }
                                });
            }
            live = new WeakReference<CampaignBarManager>(script);
            Global.getSector().addTransientScript(script);
            log(
                    "INSTALLED build="
                            + BUILD
                            + " version="
                            + Global.getSettings().getVersionString()
                            + " loader="
                            + loaderChain()
                            + " barInjection="
                            + script.enableBarInjection
                            + " layout="
                            + script.barLayout
                            + " owner=AshLib");
            log(
                    "DEV_INPUT_CONFIG enabled="
                            + script.devHotkeys
                            + " probe=CTRL_SHIFT_F9 host=CTRL_SHIFT_F10 fallback=INTERNAL_DIAGNOSTIC_API"
                            + " captureOnceAfterLoad="
                            + script.autoProbePending);
            log(
                    "NAV_CONFIG build="
                            + BUILD
                            + " enabled="
                            + script.enableNavigableBarPoc
                            + " registrations="
                            + script.hosts.keySet()
                            + " campaignOnly=false compatibleNativeBars=true owner=AshLib");
            log(
                    "UI_REFRESH_CONFIG enabled="
                            + (script.refresh != null)
                            + " readOnly=true owner=EXISTING_MANAGER");
        } catch (Throwable e) {
            enabled = false;
            logFailure("INSTALL", e);
        }
    }

    public static boolean isEnabled() {
        CampaignBarManager script = live.get();
        return enabled && script != null && !script.done;
    }

    /** Diagnostic entry for Console Commands RunCode; no dependency on that mod. */
    public static void requestProbe() {
        requestProbe("api");
    }

    public static void requestProbe(String source) {
        CampaignBarManager manager = live.get();
        if (enabled && manager != null && !manager.done) {
            manager.queueProbe("market-option".equals(source) ? source : "api");
        } else {
            probeLog("PROBE_FAILED source=api status=CAMPAIGN_BAR_NOT_ACTIVE");
        }
    }

    private void queueProbe(String source) {
        probeLog(
                "PROBE_REQUESTED source="
                        + source
                        + " build="
                        + BUILD
                        + " coalesced="
                        + probeRequested);
        if (!probeRequested) {
            probeSource = source;
        }
        probeRequested = true;
    }

    public static void requestOpen() {
        CampaignBarManager manager = live.get();
        if (enabled && manager != null && !manager.done) {
            if (manager.specs.isEmpty()) {
                openResult("NO_REGISTERED_TABS");
                return;
            }
            manager.requestedButtonCore = null;
            manager.requestedId = manager.specs.get(0).id;
            manager.openRequested = true;
        }
    }

    public static void requestClose() {
        CampaignBarManager manager = live.get();
        if (enabled && manager != null && !manager.done) {
            for (CampaignScreenHost host : manager.hosts.values()) {
                host.requestClose();
            }
        }
    }

    /** Queues a diagnostic checkpoint without changing UI state. */
    public static void requestRefreshCheckpoint(String label) {
        CampaignBarManager manager = live.get();
        if (manager != null
                && !manager.done
                && manager.refresh != null
                && manager.refresh.active()) {
            manager.refreshCheckpoint = label == null ? "manual" : label;
        }
    }

    public boolean isDone() {
        return done;
    }

    public boolean runWhilePaused() {
        return true;
    }

    public void advance(float amount) {
        if (done || Global.getSector() == null) {
            return;
        }
        try {
            pollDevelopmentKeys();
        } catch (Throwable e) {
            devHotkeys = false;
            logFailure("DEV_INPUT", e);
        }
        final CampaignUIAPI ui;
        try {
            ui = Global.getSector().getCampaignUI();
        } catch (Throwable e) {
            failPendingProbe("PUBLIC_CAMPAIGN_UI");
            logFailure("PUBLIC_CAMPAIGN_UI", e);
            done = true;
            resetHosts();
            return;
        }
        if (ui == null) {
            failPendingProbe("NO_CAMPAIGN_UI");
            return;
        }
        // Defer automatic diagnostics until a later update after functional initialization.
        boolean autoCapture = autoProbePending && functionalObserved && !ui.isShowingMenu();
        final boolean opening = openRequested;
        openRequested = false;
        if (opening || System.nanoTime() >= discoveryRetryAt) {
            try {
                advanceFunctional(ui, opening);
                discoveryRetryAt = 0;
                discoveryFailure = null;
            } catch (Throwable error) {
                discoveryRetryAt = System.nanoTime() + 500000000L;
                String reason = String.valueOf(error);
                if (!reason.equals(discoveryFailure) || opening) {
                    logFailure("CORE_DISCOVERY/LOCATE_ACTIVE_CORE", error);
                    discoveryFailure = reason;
                }
            }
        }
        if (autoCapture) {
            autoProbePending = false;
            queueProbe("automatic-after-functional-init");
        }
        if (probeRequested) {
            probeRequested = false;
            sample++;
            try {
                notice("Campaign probe #" + sample + ": started (" + probeSource + ").");
                boolean campaignOk =
                        CampaignProbeRun.run(
                                sample,
                                probeSource,
                                "campaign",
                                new CampaignProbeRun.Capture() {
                                    public JSONObject capture() throws Exception {
                                        return snapshot(
                                                CampaignUiAccess.findProbeCore(ui),
                                                "campaign",
                                                ui.getCurrentInteractionDialog() == null);
                                    }
                                },
                                probeSink);
                boolean dialogOk = true;
                if (ui.getCurrentInteractionDialog() != null) {
                    dialogOk =
                            CampaignProbeRun.run(
                                    sample,
                                    probeSource,
                                    "dialog",
                                    new CampaignProbeRun.Capture() {
                                        public JSONObject capture() throws Exception {
                                            Object encounter =
                                                    CampaignUiAccess.read(ui, "getEncounterDialog");
                                            return snapshot(
                                                    CampaignUiAccess.findProbeCore(encounter),
                                                    "dialog",
                                                    true);
                                        }
                                    },
                                    probeSink);
                }
                probeLog(
                        "PROBE_FINISHED sample="
                                + sample
                                + " campaign="
                                + campaignOk
                                + " dialog="
                                + (ui.getCurrentInteractionDialog() == null
                                        ? "NOT_APPLICABLE"
                                        : String.valueOf(dialogOk)));
                notice(
                        campaignOk && dialogOk
                                ? "Campaign probe #"
                                        + sample
                                        + ": success; geometry in starsector.log ([ASHLIB_BAR_PROBE])."
                                : "Campaign probe #"
                                        + sample
                                        + ": failed/partial; see [ASHLIB_BAR_PROBE] in starsector.log.");
            } catch (Throwable error) {
                probeLog(
                        "PROBE_FAILED sample="
                                + sample
                                + " source="
                                + probeSource
                                + " status=CONTEXT_OR_NOTIFICATION_EXCEPTION");
                logFailure("PROBE_DISPATCH", error);
            }
        }
    }

    private void failPendingProbe(String reason) {
        if (!probeRequested) {
            return;
        }
        probeRequested = false;
        probeLog(
                "PROBE_FAILED source=" + probeSource + " status=" + reason + " beforeCapture=true");
    }

    private void advanceFunctional(final CampaignUIAPI ui, final boolean opening) throws Exception {
        final WeakReference<UIPanelAPI> origin = opening ? requestedButtonCore : null;
        final WeakReference<UIPanelAPI> originBar =
                opening && origin != null ? requestedButtonBar : null;
        if (opening) {
            requestedButtonCore = null;
            requestedButtonBar = null;
        }
        // Only a validated registered-button request may use Core's Refit transition.
        if (opening
                && ui.getCurrentCoreTab() == CoreUITabId.REFIT
                && (!enableNavigableBarPoc || origin == null)) {
            log("REFIT_TRANSITION_BLOCKED reason=LEGACY_OR_UNREGISTERED_ENTRY");
            openResult("REFIT_PROTECTED");
            return;
        }
        if (ui.isShowingMenu()) {
            if (opening) {
                openResult("MENU_ACTIVE");
            }
            return;
        }
        final boolean inDialog = ui.getCurrentInteractionDialog() != null;
        Object owner = inDialog ? CampaignUiAccess.read(ui, "getEncounterDialog") : ui;
        // Bar discovery remains available if host preparation fails.
        final UIPanelAPI active = CampaignUiAccess.findProbeCore(owner);
        String context = inDialog ? "dialog" : "campaign";
        if (inDialog
                && ui.getCurrentInteractionDialog().getInteractionTarget() != null
                && ui.getCurrentInteractionDialog().getInteractionTarget().getMarket() != null) {
            context = "market";
        }
        if (refresh != null) {
            refresh.core(active, inDialog, context);
            if (!inDialog && dialogCore.get() != null) {
                refresh.core(null, true, context);
            }
        }
        rememberCore(active, inDialog, context);
        if (!inDialog) {
            rememberCore(null, true, "dialog-ended");
        }
        if (active != null && !startup.hasMarked(active, "CORE_UI_AVAILABLE")) {
            startup.mark(active, "CORE_UI_AVAILABLE", "context=" + context);
        }
        considerBar(active, context);
        if (!hosts.isEmpty() || opening) {
            boolean hostOk =
                    hostCapability.run(
                            "PREPARE_OPEN_OR_CLOSE",
                            opening,
                            new CampaignCapabilityGuard.Work() {
                                public void run() throws Exception {
                                    advanceHost(ui, active, inDialog, opening, origin, originBar);
                                }
                            });
            if (!hostOk && opening) {
                notice(
                        "Campaign host unavailable; see full diagnostics in starsector.log. Bar/probe remain independent.");
            }
        }
        if (!enableBarInjection && active != null) {
            functionalObserved = true;
        }
        if (refresh != null) {
            refresh.view(active);
        }
        if (refresh != null && refreshCheckpoint != null) {
            refresh.checkpoint(active, refreshCheckpoint);
            refreshCheckpoint = null;
        }
    }

    private void advanceHost(
            CampaignUIAPI ui,
            UIPanelAPI active,
            boolean inDialog,
            boolean opening,
            WeakReference<UIPanelAPI> origin,
            WeakReference<UIPanelAPI> originBar)
            throws Exception {
        if (hosts.isEmpty()) {
            if (opening) {
                openResult("NO_REGISTERED_TABS");
            }
            return;
        }
        for (CampaignScreenHost host : hosts.values()) {
            host.advance(active);
        }
        long began = System.nanoTime();
        boolean alreadyPrepared = true, prepared = true;
        for (CampaignScreenHost host : hosts.values()) {
            alreadyPrepared &= host.isPrepared();
            host.prepare(active, Global.getSettings().getVersionString());
            host.observe(active);
            prepared &= host.isPrepared();
        }
        if (active != null && prepared) {
            startup.mark(active, "HOST_PREPARED", "source=metadata-or-passive");
            if (!alreadyPrepared) {
                startup.duration(active, "HOST_PREPARATION", began);
            }
        }
        if (opening) {
            TabSpec spec = null;
            for (TabSpec tabSpec : specs) {
                if (tabSpec.id.equals(requestedId)) {
                    spec = tabSpec;
                }
            }
            if (spec == null) {
                openResult("UNKNOWN_REGISTERED_TAB");
                return;
            }
            CampaignScreenHost host = hosts.get(spec.id);
            long openingBegan = System.nanoTime();
            String result;
            if (origin != null
                    && (origin.get() != active
                            || originBar == null
                            || originBar.get() != CampaignUiAccess.read(active, "getButtons"))) {
                result = "STALE_BUTTON_CONTEXT";
            } else if (!CampaignCompatibility.GAME_VERSION.equals(
                    Global.getSettings().getVersionString())) {
                result = "UNSUPPORTED_GAME_VERSION";
            } else if (!inDialog
                    && ui.isShowingDialog()
                    && CampaignUiAccess.read(active, "getCurrentTab") == null) {
                result = "OTHER_DIALOG_ACTIVE";
            } else if (origin != null) {
                WeakReference<CampaignBarInjector.Session> ref = bars.get(active);
                CampaignBarInjector.Session session = ref == null ? null : ref.get();
                result =
                        session == null || !session.isActive()
                                ? "NAVIGATION_BUTTON_NOT_OWNED"
                                : host.open(
                                        active,
                                        ui.getCurrentCoreTab() == CoreUITabId.REFIT,
                                        spec.content,
                                        session.button(spec.id),
                                        enableNavigableBarPoc);
            } else {
                result =
                        host.open(
                                active, ui.getCurrentCoreTab() == CoreUITabId.REFIT, spec.content);
            }
            if ("OPENED".equals(result)) {
                startup.mark(active, "FIRST_OPEN_SUCCEEDED", "nativeIdentityConfirmed=true");
            }
            if ("OPENED".equals(result) || "ALREADY_OPEN".equals(result)) {
                WeakReference<CampaignBarInjector.Session> ref = bars.get(active);
                CampaignBarInjector.Session session = ref == null ? null : ref.get();
                if (session != null) {
                    session.revealButton(spec.id);
                }
            }
            startup.duration(active, "OPEN_REQUEST_" + result, openingBegan);
            log("ROUTED_OPEN id=" + spec.id + " result=" + result);
            openResult(result);
        }
    }

    private void rememberCore(UIPanelAPI core, boolean dialog, String context) {
        WeakReference<UIPanelAPI> previous = dialog ? dialogCore : campaignCore;
        UIPanelAPI old = previous.get();
        if (old == core) {
            return;
        }
        log(
                "LIFECYCLE CORE_CHANGED context="
                        + context
                        + " oldCore="
                        + CampaignBarLifecycle.identity(old)
                        + " newCore="
                        + CampaignBarLifecycle.identity(core));
        if (old != null) {
            retireBar(old, "CORE_REPLACED");
        }
        if (dialog) {
            dialogCore = new WeakReference<UIPanelAPI>(core);
        } else {
            campaignCore = new WeakReference<UIPanelAPI>(core);
        }
    }

    private CampaignBarInjector.Session session(UIPanelAPI core) {
        WeakReference<CampaignBarInjector.Session> reference = bars.get(core);
        return reference == null ? null : reference.get();
    }

    private void retireBar(UIPanelAPI core, String reason) {
        CampaignBarInjector.Session old = session(core);
        if (old != null) {
            lastNavigationIndex = old.navigationIndex();
            boolean restored = old.retire();
            log(
                    "LIFECYCLE BAR_RETIRED reason="
                            + reason
                            + " core="
                            + CampaignBarLifecycle.identity(core)
                            + " bar="
                            + CampaignBarLifecycle.identity(old.bar())
                            + " restored="
                            + restored
                            + " index="
                            + lastNavigationIndex);
            if (refresh != null) {
                refresh.milestone(
                        core,
                        "BAR_RETIRED",
                        "reason="
                                + reason
                                + " retiredBar="
                                + CampaignBarLifecycle.identity(old.bar())
                                + " restored="
                                + restored);
            }
        }
        bars.remove(core);
        barStates.remove(core);
    }

    private void considerBar(final UIPanelAPI core, final String context) {
        if (!enableBarInjection || core == null) {
            return;
        }
        CampaignBarLifecycle state = barStates.get(core);
        try {
            Object value = CampaignUiAccess.read(core, "getButtons");
            final UIPanelAPI bar = value instanceof UIPanelAPI ? (UIPanelAPI) value : null;
            if (refresh != null) {
                refresh.sample(core, bar, context, false);
            }
            CampaignBarInjector.Session existing = session(core);
            if (state == null
                    || state.bar.get() != bar
                    || (existing != null && existing.bar() != bar)) {
                Object previous = state == null ? null : state.bar.get();
                int index =
                        existing == null
                                ? (state == null ? lastNavigationIndex : state.index)
                                : existing.navigationIndex();
                if (existing != null) {
                    retireBar(core, "BAR_REPLACED");
                }
                state = new CampaignBarLifecycle(bar, index);
                barStates.put(core, state);
                log(
                        "LIFECYCLE BAR_CHANGED context="
                                + context
                                + " core="
                                + CampaignBarLifecycle.identity(core)
                                + " oldBar="
                                + CampaignBarLifecycle.identity(previous)
                                + " newBar="
                                + CampaignBarLifecycle.identity(bar)
                                + " resumeIndex="
                                + index);
                existing = null;
            }
            if (bar != null && !startup.hasMarked(core, "NATIVE_BAR_LOCATED")) {
                startup.mark(
                        core,
                        "NATIVE_BAR_LOCATED",
                        "context=" + context + " bar=" + CampaignBarLifecycle.identity(bar));
            }
            if (existing != null && existing.isActive()) {
                existing.checkVisualHealth();
                if (enableNavigableBarPoc) {
                    existing.considerNavigation(false);
                }
                if (existing.isActive()) {
                    state.index = existing.navigationIndex();
                    lastNavigationIndex = state.index;
                    return;
                }
                bars.remove(core);
                state.attempting();
                state.failed(System.nanoTime(), "RUNTIME_SESSION_INACTIVE");
            }
            String readiness = CampaignBarReadiness.inspect(core, enableNavigableBarPoc);
            if (!readiness.equals(state.readiness)) {
                state.readiness = readiness;
                log(
                        "BAR_READINESS "
                                + readiness
                                + " context="
                                + context
                                + " core="
                                + CampaignBarLifecycle.identity(core)
                                + " bar="
                                + CampaignBarLifecycle.identity(bar));
                if (refresh != null) {
                    refresh.milestone(core, "BAR_READINESS", "status=" + readiness);
                }
            }
            state.observe(CampaignBarLifecycle.signature(core, bar));
            if (!"READY_FOR_PREFLIGHT".equals(readiness) || !state.mayAttempt(System.nanoTime())) {
                return;
            }
            if (org.lwjgl.input.Mouse.isCreated()
                    && (org.lwjgl.input.Mouse.isButtonDown(0)
                            || org.lwjgl.input.Mouse.isButtonDown(1))) {
                return;
            }
            state.attempting();
            functionalObserved = true;
            final String[] failure = {null};
            final WeakReference<UIPanelAPI> originBar = new WeakReference<UIPanelAPI>(bar);
            long began = System.nanoTime();
            SettingsAPI s = Global.getSettings();
            CampaignBarProbe probe = new CampaignBarProbe();
            JSONObject before =
                    enableNavigableBarPoc
                            ? probe.captureBarLayout(
                                    core,
                                    context,
                                    s.getVersionString(),
                                    s.getScreenWidth(),
                                    s.getScreenHeight(),
                                    s.getScreenWidthPixels(),
                                    s.getScreenHeightPixels(),
                                    s.getScreenScaleMult())
                            : probe.captureLayout(
                                    core,
                                    context,
                                    s.getVersionString(),
                                    s.getScreenWidth(),
                                    s.getScreenHeight(),
                                    s.getScreenWidthPixels(),
                                    s.getScreenHeightPixels(),
                                    s.getScreenScaleMult());
            final WeakReference<CampaignBarManager> script =
                    new WeakReference<CampaignBarManager>(this);
            final WeakReference<UIPanelAPI> origin = new WeakReference<UIPanelAPI>(core);
            CampaignBarInjector.Click click =
                    new CampaignBarInjector.Click() {
                        public void open(String id) {
                            CampaignBarManager current = script.get();
                            if (current == null
                                    || current.done
                                    || !enabled
                                    || origin.get() == null
                                    || originBar.get() == null) {
                                return;
                            }
                            current.requestedButtonCore = origin;
                            current.requestedButtonBar = originBar;
                            current.openRequested = true;
                            current.requestedId = id;
                            current.startup.mark(
                                    origin.get(),
                                    "FIRST_CLICK",
                                    "source=registered-button context=" + context);
                            log(
                                    "OPEN_REQUEST source=registered-button id="
                                            + id
                                            + " context="
                                            + context);
                        }
                    };
            CampaignBarInjector.Sink sink =
                    new CampaignBarInjector.Sink() {
                        public void event(String text) {
                            if (text.startsWith("PREFLIGHT_OK")) {
                                startup.mark(core, "PREFLIGHT_APPROVED", "context=" + context);
                            }
                            if (text.startsWith("INSERTED")) {
                                startup.mark(core, "TAB_INSERTED", "context=" + context);
                            }
                            log("BAR " + text + " context=" + context);
                            if (refresh != null
                                    && (text.startsWith("NAV_INDEX_APPLIED")
                                            || text.contains("ROLLBACK")
                                            || text.startsWith("PREFLIGHT_OK"))) {
                                refresh.milestone(core, "ASHLIB_OPERATION", text);
                            }
                        }

                        public void failure(String operation, Throwable error) {
                            failure[0] = operation + ":" + error;
                            for (Throwable cause = error.getCause();
                                    cause != null;
                                    cause = cause.getCause()) {
                                failure[0] += " <- " + cause;
                            }
                            logFailure("BAR_" + operation + " context=" + context, error);
                            if (refresh != null) {
                                refresh.milestone(
                                        core,
                                        "ASHLIB_OPERATION_FAILED",
                                        "operation=" + operation + " error=" + error);
                            }
                        }

                        public void snapshot(String phase, JSONObject data) {
                            log(
                                    "BAR_GEOMETRY phase="
                                            + phase
                                            + " context="
                                            + context
                                            + " data="
                                            + data);
                        }
                    };
            log(
                    "LIFECYCLE INSTALL_ATTEMPT context="
                            + context
                            + " core="
                            + CampaignBarLifecycle.identity(core)
                            + " bar="
                            + CampaignBarLifecycle.identity(bar)
                            + " attempt="
                            + state.attempts()
                            + " index="
                            + state.index);
            if (refresh != null) {
                refresh.milestone(
                        core,
                        "INSTALL_ATTEMPT",
                        "attempt=" + state.attempts() + " index=" + state.index);
            }
            CampaignBarInjector.Session installed =
                    CampaignBarInjector.install(
                            specs,
                            core,
                            barLayout,
                            before,
                            click,
                            sink,
                            enableNavigableBarPoc,
                            state.index);
            if (installed != null) {
                bars.put(core, new WeakReference<CampaignBarInjector.Session>(installed));
                state.index = installed.navigationIndex();
                log(
                        "LIFECYCLE INSTALL_CONFIRMED context="
                                + context
                                + " core="
                                + CampaignBarLifecycle.identity(core)
                                + " bar="
                                + CampaignBarLifecycle.identity(bar)
                                + " index="
                                + state.index
                                + " navigable="
                                + installed.navigable());
                if (refresh != null) {
                    refresh.milestone(
                            core,
                            "INSTALL_CONFIRMED",
                            "index=" + state.index + " navigable=" + installed.navigable());
                }
            } else {
                state.failed(System.nanoTime(), failure[0]);
                log(
                        "LIFECYCLE INSTALL_DEFERRED context="
                                + context
                                + " core="
                                + CampaignBarLifecycle.identity(core)
                                + " bar="
                                + CampaignBarLifecycle.identity(bar)
                                + " disposition="
                                + state.disposition()
                                + " reason="
                                + state.reason);
                if (refresh != null) {
                    refresh.milestone(
                            core,
                            "INSTALL_DEFERRED",
                            "disposition=" + state.disposition() + " reason=" + state.reason);
                }
            }
            startup.duration(core, "BAR_TRANSACTION", began);
        } catch (Throwable error) {
            if (state == null) {
                state = new CampaignBarLifecycle(null, lastNavigationIndex);
                barStates.put(core, state);
            }
            // Keep observing this Core so a later UI change can allow a retry.
            if (state.mayAttempt(System.nanoTime())) {
                state.attempting();
                state.failed(System.nanoTime(), "DISCOVERY:" + error);
                logFailure("BAR_DISCOVERY context=" + context, error);
            }
        } finally {
            if (refresh != null) {
                refresh.sampleCore(core, context, true);
                CampaignBarInjector.Session current = session(core);
                refresh.result(
                        core,
                        current != null && current.isActive(),
                        state == null
                                ? "NOT_OBSERVABLE"
                                : "READY_FOR_PREFLIGHT".equals(state.readiness)
                                        ? state.disposition()
                                        : "READINESS_" + state.readiness);
            }
        }
    }

    private void removeBars() {
        for (WeakReference<CampaignBarInjector.Session> reference : bars.values()) {
            CampaignBarInjector.Session session = reference.get();
            if (session != null && session.isActive()) {
                boolean restored = session.retire();
                if (refresh != null) {
                    refresh.milestone(
                            session.core(),
                            "BAR_RETIRED",
                            "reason=MANAGER_RELOAD restored=" + restored);
                }
            }
        }
        bars.clear();
        barStates.clear();
        requestedButtonCore = null;
        requestedButtonBar = null;
        campaignCore.clear();
        dialogCore.clear();
        if (refresh != null) {
            refresh.stop();
        }
    }

    private static void openResult(String result) {
        log("OPEN_REQUEST_RESULT " + result);
        notice("Campaign Bar: " + result);
    }

    private static void logFailure(String operation, Throwable error) {
        String version =
                Global.getSettings() == null ? "unknown" : Global.getSettings().getVersionString();
        Global.getLogger(CampaignBarManager.class)
                .error(
                        "[ASHLIB_CAMPAIGN_BAR] CAPABILITY_UNAVAILABLE build="
                                + BUILD
                                + " version="
                                + version
                                + " operation="
                                + operation,
                        error);
    }

    private static String loaderChain() {
        StringBuilder chain = new StringBuilder();
        for (ClassLoader loader = CampaignBarManager.class.getClassLoader();
                loader != null;
                loader = loader.getParent()) {
            if (chain.length() > 0) {
                chain.append(" -> ");
            }
            chain.append(loader.getClass().getName());
        }
        return chain.append(" -> bootstrap").toString();
    }

    /** Polls diagnostic shortcuts on key transitions without consuming game input. */
    private void pollDevelopmentKeys() {
        if (!devHotkeys) {
            keys.update(false, false, false, false, false, false);
            return;
        }
        boolean ready = Keyboard.isCreated(), read = devHotkeys && ready;
        boolean control =
                read
                        && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                                || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL));
        boolean shift =
                read
                        && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        keys.update(
                devHotkeys,
                ready,
                control,
                shift,
                read && Keyboard.isKeyDown(Keyboard.KEY_F9),
                read && Keyboard.isKeyDown(Keyboard.KEY_F10));
    }

    private JSONObject snapshot(UIPanelAPI core, String context, boolean active) throws Exception {
        SettingsAPI s = Global.getSettings();
        JSONObject data =
                new CampaignBarProbe()
                        .capture(
                                core,
                                context,
                                s.getVersionString(),
                                s.getScreenWidth(),
                                s.getScreenHeight(),
                                s.getScreenWidthPixels(),
                                s.getScreenHeightPixels(),
                                s.getScreenScaleMult());
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        return data.put("activeContext", active)
                .put(
                        "publicCoreTab",
                        ui.getCurrentCoreTab() == null
                                ? JSONObject.NULL
                                : ui.getCurrentCoreTab().name())
                .put("showingMenu", ui.isShowingMenu())
                .put("showingDialog", ui.isShowingDialog());
    }

    private static void notice(String message) {
        if (Global.getSector() != null && Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage(message);
        }
    }

    private static void log(String message) {
        Global.getLogger(CampaignBarManager.class).info("[ASHLIB_CAMPAIGN_BAR] " + message);
    }

    private static void probeLog(String message) {
        Global.getLogger(CampaignBarManager.class).info("[ASHLIB_BAR_PROBE] " + message);
    }
}
