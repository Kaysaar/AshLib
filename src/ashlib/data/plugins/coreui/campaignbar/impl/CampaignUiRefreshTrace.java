package ashlib.data.plugins.coreui.campaignbar.impl;

import com.fs.starfarer.api.campaign.CoreUITabId;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Observes Core, bar and native button identities without modifying them or walking the UI tree.
 * Uses weak references and reference equality; identity hashes are log labels only.
 */
final class CampaignUiRefreshTrace {
    interface Sink {
        void event(String text);
    }

    interface Clock {
        long millis();

        long nanos();
    }

    private static final CoreUITabId[] IDS = {
        CoreUITabId.CHARACTER,
        CoreUITabId.FLEET,
        CoreUITabId.REFIT,
        CoreUITabId.CARGO,
        CoreUITabId.MAP,
        CoreUITabId.INTEL,
        CoreUITabId.OUTPOSTS
    };
    private final Sink sink;
    private final Clock clock;
    private final long began;
    private final String run;
    private long serial, sequence, observations;
    private boolean disabled;
    private final List<Ref> identities = new ArrayList<Ref>();
    private final List<State> states = new ArrayList<State>();
    private final Ref[] slots = new Ref[2];
    private String lastContext;
    private final Ref nil = new Ref(null, "none");

    static final class Ref {
        final WeakReference<Object> weak;
        final String label;
        final boolean wasNull;

        Ref(Object o, String label) {
            weak = new WeakReference<Object>(o);
            this.label = label;
            wasNull = o == null;
        }

        boolean same(Object o) {
            return wasNull ? o == null : o != null && weak.get() == o;
        }

        String prior() {
            return !wasNull && weak.get() == null ? label + "(collected)" : label;
        }
    }

    private static final class State {
        Ref core, bar, view;
        String tabId;
        Ref[] buttons;
        String context, registryStatus, outcome;
        long change;
        boolean observedOutside;

        State(Ref core) {
            this.core = core;
        }
    }

    CampaignUiRefreshTrace(Sink sink) {
        this(
                sink,
                new Clock() {
                    public long millis() {
                        return System.currentTimeMillis();
                    }

                    public long nanos() {
                        return System.nanoTime();
                    }
                });
    }

    CampaignUiRefreshTrace(Sink sink, Clock clock) {
        this.sink = sink;
        this.clock = clock;
        began = clock.nanos();
        run = clock.millis() + "-" + Long.toHexString(began);
        safeEvent("START", null, "enabled=true comparisons=REFERENCE_IDENTITY cause=NOT_INFERRED");
    }

    boolean active() {
        return !disabled;
    }

    private Ref ref(Object o) {
        if (o == null) {
            return nil;
        }
        for (Iterator<Ref> i = identities.iterator(); i.hasNext(); ) {
            Ref r = i.next();
            Object old = r.weak.get();
            if (old == o) {
                return r;
            }
            if (old == null) {
                i.remove();
            }
        }
        Ref r = new Ref(o, "r" + (++serial) + ":" + CampaignBarLifecycle.identity(o));
        identities.add(r);
        return r;
    }

    private State state(Object core) {
        if (core == null) {
            return null;
        }
        for (Iterator<State> i = states.iterator(); i.hasNext(); ) {
            State s = i.next();
            Object old = s.core.weak.get();
            if (old == core) {
                return s;
            }
            if (old == null) {
                i.remove();
            }
        }
        if (states.size() >= 64) {
            throw new IllegalStateException("OBSERVER_CAPACITY_64_LIVE_CORES");
        }
        State s = new State(ref(core));
        states.add(s);
        return s;
    }

    private void emit(String kind, State s, String detail) {
        sink.event(
                "UI_REFRESH run="
                        + run
                        + " seq="
                        + (++sequence)
                        + " timestampMs="
                        + clock.millis()
                        + " elapsedMs="
                        + ((clock.nanos() - began) / 1000000L)
                        + " event="
                        + kind
                        + " context="
                        + (s == null ? (lastContext == null ? "unknown" : lastContext) : s.context)
                        + " core="
                        + (s == null ? "unobserved" : s.core.prior())
                        + " bar="
                        + (s == null || s.bar == null ? "unobserved" : s.bar.prior())
                        + " "
                        + detail);
    }

    private void failure(Throwable e) {
        disabled = true;
        try {
            emit(
                    "OBSERVER_DISABLED",
                    null,
                    "reason=" + clean(String.valueOf(e)) + " functionalManagerUnaffected=true");
        } catch (Throwable ignored) {
        }
    }

    private void safeEvent(String kind, State s, String detail) {
        if (!disabled) {
            try {
                emit(kind, s, detail);
            } catch (Throwable error) {
                failure(error);
            }
        }
    }

    /** Samples Core identity before the manager updates the active context. */
    void core(Object core, boolean dialog, String context) {
        if (disabled) {
            return;
        }
        try {
            int slot = dialog ? 1 : 0;
            Ref old = slots[slot];
            State s = state(core);
            if (s != null) {
                s.context = context;
            }
            if (!Objects.equals(lastContext, context)) {
                emit(
                        "CONTEXT_OBSERVED",
                        s,
                        "oldContext="
                                + lastContext
                                + " newContext="
                                + context
                                + " recreation=NOT_IMPLIED");
                lastContext = context;
            }
            if (old == null || !old.same(core)) {
                String kind =
                        old == null
                                ? "CORE_FIRST_OBSERVED"
                                : old.weak.get() == null && !old.wasNull
                                        ? "CORE_AFTER_OBSERVATION_GAP"
                                        : core == null
                                                ? "CORE_NO_LONGER_EXPOSED"
                                                : old.wasNull
                                                        ? "CORE_NOW_EXPOSED"
                                                        : "CORE_REFERENCE_CHANGED";
                emit(
                        kind,
                        s,
                        "slot="
                                + (dialog ? "dialog" : "campaign")
                                + " previous="
                                + (old == null ? "unobserved" : old.prior())
                                + " current="
                                + ref(core).label
                                + " cause=NOT_INFERRED");
                slots[slot] = ref(core);
            }
        } catch (Throwable error) {
            failure(error);
        }
    }

    void sampleCore(Object core, String context, boolean ashlibWindow) {
        if (disabled) {
            return;
        }
        try {
            sample(core, CampaignUiAccess.read(core, "getButtons"), context, ashlibWindow);
        } catch (Throwable error) {
            failure(error);
        }
    }

    /** Observes the active tab separately from bar reconstruction. */
    void view(Object core) {
        if (disabled || core == null) {
            return;
        }
        try {
            State s = state(core);
            Object tab = CampaignUiAccess.read(core, "getCurrentTab"),
                    id = CampaignUiAccess.read(core, "getCurrentTabId");
            boolean idObservable =
                    CampaignUiAccess.method(core.getClass(), "getCurrentTabId") != null;
            String name =
                    !idObservable
                            ? "UNOBSERVABLE"
                            : id instanceof Enum
                                    ? ((Enum<?>) id).name()
                                    : id instanceof String ? (String) id : ref(id).label;
            if (s.view == null || !s.view.same(tab) || !name.equals(s.tabId)) {
                emit(
                        "TAB_OBSERVED",
                        s,
                        "previous="
                                + (s.view == null ? "unobserved" : s.view.prior())
                                + " current="
                                + ref(tab).label
                                + " previousId="
                                + s.tabId
                                + " currentId="
                                + clean(name)
                                + " recreation=NOT_IMPLIED nestedContent=NOT_OBSERVED");
                s.view = ref(tab);
                s.tabId = name;
            }
        } catch (Throwable error) {
            failure(error);
        }
    }

    /** Compares native registry identities; layout changes and custom tabs are excluded. */
    void sample(Object core, Object bar, String context, boolean ashlibWindow) {
        if (disabled) {
            return;
        }
        try {
            State s = state(core);
            if (s == null) {
                return;
            }
            s.context = context;
            String origin = ashlibWindow ? "ASHLIB_WINDOW" : "OUTSIDE_ASHLIB_WINDOW";
            observations++;
            if (s.bar == null || !s.bar.same(bar)) {
                String kind =
                        s.bar == null
                                ? "BAR_FIRST_OBSERVED"
                                : s.bar.weak.get() == null && !s.bar.wasNull
                                        ? "BAR_AFTER_OBSERVATION_GAP"
                                        : bar == null
                                                ? "BAR_NO_LONGER_EXPOSED"
                                                : s.bar.wasNull
                                                        ? "BAR_NOW_EXPOSED"
                                                        : "BAR_REFERENCE_CHANGED";
                emit(
                        kind,
                        s,
                        "previous="
                                + (s.bar == null ? "unobserved" : s.bar.prior())
                                + " current="
                                + ref(bar).label
                                + " origin="
                                + origin
                                + " cause=NOT_INFERRED");
                s.bar = ref(bar);
                s.change = sequence;
                s.outcome = null;
            }
            Object registry = CampaignUiAccess.read(bar, "getButtons");
            String status =
                    bar == null
                            ? "NO_BAR"
                            : registry instanceof Map ? "AVAILABLE" : "UNOBSERVABLE_REGISTRY";
            if (!status.equals(s.registryStatus)) {
                emit("NATIVE_REGISTRY_STATUS", s, "status=" + status + " origin=" + origin);
                s.registryStatus = status;
            }
            if (!(registry instanceof Map)) {
                return;
            }
            Map<?, ?> map = (Map<?, ?>) registry;
            if (s.buttons == null) {
                s.buttons = new Ref[IDS.length];
                StringBuilder inventory = new StringBuilder();
                for (int i = 0; i < IDS.length; i++) {
                    s.buttons[i] = ref(map.get(IDS[i]));
                    inventory.append(IDS[i]).append('=').append(s.buttons[i].label).append(';');
                }
                emit(
                        "NATIVE_BUTTONS_BASELINE",
                        s,
                        "values=" + inventory + " origin=" + origin + " recreation=NOT_PROVEN");
            } else {
                StringBuilder changed = new StringBuilder();
                for (int i = 0; i < IDS.length; i++) {
                    Object current = map.get(IDS[i]);
                    Ref old = s.buttons[i];
                    if (!old.same(current)) {
                        Ref next = ref(current);
                        changed.append(IDS[i])
                                .append(':')
                                .append(old.prior())
                                .append("->")
                                .append(next.label)
                                .append(';');
                        s.buttons[i] = next;
                    }
                }
                if (changed.length() > 0) {
                    emit(
                            "NATIVE_BUTTON_REFERENCES_CHANGED",
                            s,
                            "components=" + changed + " origin=" + origin + " cause=NOT_INFERRED");
                    s.change = sequence;
                    s.outcome = null;
                    s.observedOutside = !ashlibWindow;
                }
            }
        } catch (Throwable error) {
            failure(error);
        }
    }

    /** Records a lifecycle event without initiating recovery. */
    void milestone(Object core, String kind, String detail) {
        if (disabled) {
            return;
        }
        try {
            State s = state(core);
            emit(kind, s, detail + " relatedChange=" + (s == null ? 0 : s.change));
        } catch (Throwable error) {
            failure(error);
        }
    }

    void result(Object core, boolean active, String disposition) {
        if (disabled) {
            return;
        }
        try {
            State s = state(core);
            if (s == null) {
                return;
            }
            String value = active ? "ACTIVE_SESSION_REPORTED" : disposition;
            if (!value.equals(s.outcome)) {
                emit(
                        "RECOVERY_OBSERVATION",
                        s,
                        "result="
                                + clean(value)
                                + " relatedChange="
                                + s.change
                                + " recoveryProof=REQUIRES_INSTALL_CONFIRMED_AND_RUNTIME_CHECK"
                                + (s.observedOutside ? " nativeReplacementObserved=true" : ""));
                s.outcome = value;
            }
        } catch (Throwable error) {
            failure(error);
        }
    }

    void checkpoint(Object core, String label) {
        if (disabled) {
            return;
        }
        try {
            State s = state(core);
            emit(
                    "CHECKPOINT",
                    s,
                    "label="
                            + clean(label)
                            + " samples="
                            + observations
                            + " relatedChange="
                            + (s == null ? 0 : s.change)
                            + " sampling=ACTIVE_CONTEXT_UPDATE_BOUNDARIES transientBetweenSamples=NOT_OBSERVABLE");
        } catch (Throwable error) {
            failure(error);
        }
    }

    void stop() {
        safeEvent("STOP", null, "samples=" + observations);
        disabled = true;
        states.clear();
        identities.clear();
        Arrays.fill(slots, null);
    }

    private static String clean(String s) {
        if (s == null) {
            return "none";
        }
        return s.substring(0, Math.min(160, s.length())).replaceAll("[^A-Za-z0-9_.:/=-]", "_");
    }
}
