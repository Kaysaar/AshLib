package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.api.TabSpec;
import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.lang.invoke.MethodHandle;
import java.util.Map;

/** Owns custom-tab content and lifecycle; leaves native buttons and registries unchanged. */
public final class CampaignScreenHost {
    public interface ContentFactory extends TabSpec.ContentFactory {}

    public interface Sink {
        void event(String message);

        void failure(String operation, Throwable error);
    }

    private final Sink sink;
    private final String tabId;
    private TabSpec.ContentFactory bodyFactory;
    private UIPanelAPI ownedBody;
    private Class<?> observedBase, observedContract;
    private MethodHandle constructor;
    private Class<?> attemptedCoreType;
    private String attemptedVersion;
    private UIPanelAPI core, shell;
    private boolean closeRequested, failed;
    private long generation;

    public CampaignScreenHost(String tabId, Sink sink) {
        this.tabId = tabId;
        this.sink = sink;
    }

    /** Prepares the host bridge without constructing panels or opening native tabs. */
    public boolean prepare(UIPanelAPI candidate, String version) {
        if (candidate == null || failed) {
            return false;
        }
        Class<?> type = candidate.getClass();
        if (attemptedCoreType == type && version.equals(attemptedVersion)) {
            return constructor != null;
        }
        attemptedCoreType = type;
        attemptedVersion = version;
        constructor = null;
        observedBase = null;
        observedContract = null;
        try {
            CampaignHostContract contract = CampaignHostContract.resolve(type, version);
            MethodHandle ready = CampaignRuntimeBridge.prepare(contract.base, contract.closed);
            observedBase = contract.base;
            observedContract = contract.closed;
            constructor = ready;
            sink.event(
                    "CONTRACT_PREPARED source=CORE_TYPE_METADATA profile="
                            + CampaignHostContract.PROFILE
                            + " base="
                            + contract.base.getName()
                            + " callback="
                            + contract.closed.getName()
                            + " witness="
                            + contract.witness.getName()
                            + " nativeTabOpened=false");
            return true;
        } catch (Throwable error) {
            sink.event("CONTRACT_PREPARATION_UNAVAILABLE fallback=PASSIVE_ONLY reason=" + error);
            sink.failure("HOST_PREPARE_METADATA", error);
            return false;
        }
    }

    public boolean isPrepared() {
        return constructor != null;
    }

    /** Discovers the host contract from the current native tab without changing it. */
    public void observe(UIPanelAPI candidate) throws Exception {
        if (candidate == null) {
            return;
        }
        MethodRef show = CampaignUiAccess.showMethod(candidate.getClass());
        if (show == null) {
            return;
        }
        Object tab = CampaignUiAccess.read(candidate, "getCurrentTab");
        if (tab == null || tab == shell) {
            return;
        }
        Class<?> contract = CampaignUiAccess.closingContract(tab.getClass());
        Class<?> base = show.parameterType(1);
        if (contract == null || !base.isInstance(tab)) {
            return;
        }
        if (observedBase != base || observedContract != contract) {
            observedBase = base;
            observedContract = contract;
            constructor = null;
            sink.event(
                    "CONTRACT_OBSERVED base=" + base.getName() + " callback=" + contract.getName());
        }
    }

    public String open(
            final UIPanelAPI target, boolean refitActive, TabSpec.ContentFactory content) {
        return open(target, refitActive, content, null);
    }

    /** Opens with a registered-button identity and the conservative Refit guard. */
    public String open(
            final UIPanelAPI target,
            boolean refitActive,
            TabSpec.ContentFactory content,
            ButtonAPI navigationButton) {
        return open(target, refitActive, content, navigationButton, false);
    }

    /** A validated registered button lets Core resume its own deferred Refit transition. */
    public String open(
            final UIPanelAPI target,
            boolean refitActive,
            TabSpec.ContentFactory content,
            ButtonAPI navigationButton,
            boolean nativeRefit) {
        if (failed) {
            return "DISABLED_AFTER_FAILURE";
        }
        if (target == null) {
            return "NO_ACTIVE_CORE";
        }
        try {
            Object tab = CampaignUiAccess.read(target, "getCurrentTab");
            Object tabId = CampaignUiAccess.read(target, "getCurrentTabId");
            boolean refit =
                    refitActive
                            || tabId == CoreUITabId.REFIT
                            || (tab != null
                                    && CampaignUiAccess.method(tab.getClass(), "getRefitPanel")
                                            != null);
            CampaignRefitTransition transition = null;
            if (refit) {
                if (!nativeRefit) {
                    sink.event("REFIT_TRANSITION_BLOCKED reason=LEGACY_OR_UNREGISTERED_ENTRY");
                    return "REFIT_PROTECTED";
                }
                try {
                    transition =
                            CampaignRefitTransition.prepare(
                                    target, navigationButton, attemptedVersion, this.tabId);
                    sink.event(
                            "REFIT_TRANSITION_REQUEST "
                                    + transition.state()
                                    + " publicRefit="
                                    + refitActive
                                    + " coreId="
                                    + tabId);
                } catch (Throwable unavailable) {
                    sink.event(
                            "REFIT_TRANSITION_BLOCKED reason=UNVALIDATED_OR_BUSY_CORE detail="
                                    + unavailable.getMessage());
                    sink.failure("REFIT_TRANSITION_PREFLIGHT", unavailable);
                    return "REFIT_PROTECTED";
                }
            }
            if (navigationButton != null) {
                Object buttons =
                        CampaignUiAccess.read(
                                CampaignUiAccess.read(target, "getButtons"), "getButtons");
                if (!(buttons instanceof Map)
                        || ((Map<?, ?>) buttons).get(this.tabId) != navigationButton) {
                    return "NAVIGATION_BUTTON_NOT_OWNED";
                }
            }
            if (shell != null) {
                return core == target && tab == shell
                        ? "ALREADY_OPEN"
                        : "ANOTHER_CONTEXT_OWNS_SCREEN";
            }
            observe(target);
            MethodRef show = CampaignUiAccess.showMethod(target.getClass());
            MethodRef dismiss = CampaignUiAccess.method(target.getClass(), "dismiss", int.class);
            if (show == null || dismiss == null || dismiss.getReturnType() != void.class) {
                return "HOST_CAPABILITY_MISSING";
            }
            if (observedBase == null
                    || observedContract == null
                    || show.parameterType(1) != observedBase) {
                return "HOST_CONTRACT_UNAVAILABLE_SAFE_FALLBACK";
            }
            float width = Math.min(1440f, target.getPosition().getWidth() - 40f);
            float height = Math.min(920f, target.getPosition().getHeight() - 110f);
            if (width < 320 || height < 200) {
                return "HOST_TOO_SMALL";
            }
            if (constructor == null) {
                constructor = CampaignRuntimeBridge.prepare(observedBase, observedContract);
                sink.event("BRIDGE_DEFINED_NOT_YET_OPENED");
            }
            final long ticket = ++generation;
            UIPanelAPI created =
                    (UIPanelAPI)
                            constructor.invokeWithArguments(
                                    width,
                                    height,
                                    new Runnable() {
                                        public void run() {
                                            release(ticket, "NATIVE_TAB_CLOSED");
                                        }
                                    });
            UIPanelAPI body =
                    content.create(
                            width,
                            height,
                            new Runnable() {
                                public void run() {
                                    if (generation == ticket && shell != null) {
                                        requestClose("content-button");
                                    }
                                }
                            });
            if (body == null) {
                throw new IllegalStateException("Content factory returned null");
            }
            ownedBody = body;
            bodyFactory = content;
            created.addComponent(body).inTL(0, 0);
            core = target;
            shell = created;
            show.invoke(target, navigationButton, created, Boolean.TRUE, Boolean.FALSE);
            if (CampaignUiAccess.read(target, "getCurrentTab") != created) {
                boolean deferred = transition != null && transition.deferred();
                release(ticket, "OPEN_NOT_ACCEPTED");
                if (deferred) {
                    sink.event(
                            "REFIT_TRANSITION_BLOCKED reason=NATIVE_CONFIRMATION_PENDING resume=REGISTERED_BUTTON");
                    return "REFIT_TRANSITION_DEFERRED";
                }
                sink.event("CORE_TRANSITION_REFUSED refit=" + refit + " generation=" + ticket);
                return "OPEN_NOT_ACCEPTED";
            }
            if (transition != null) {
                sink.event("REFIT_TRANSITION_ALLOWED owner=NATIVE_CORE generation=" + ticket);
            }
            sink.event("OPEN_CONFIRMED_BY_CORE_IDENTITY generation=" + ticket);
            if (transition != null) {
                sink.event("REFIT_TRANSITION_COMPLETED host=OPENED generation=" + ticket);
            }
            return "OPENED";
        } catch (Throwable e) {
            failed = true;
            sink.failure("HOST_OPEN", e);
            // After failure, dismiss only the tab owned by this host.
            try {
                if (shell != null && CampaignUiAccess.read(core, "getCurrentTab") == shell) {
                    dismissOwned();
                }
            } catch (Throwable recovery) {
                sink.failure("HOST_OWNED_CLOSE", recovery);
            }
            release(generation, "FAILED_ATTEMPT_RELEASED");
            return "HOST_FAILED_SEE_LOG";
        }
    }

    public void requestClose() {
        requestClose("api");
    }

    public void requestClose(String source) {
        if (shell == null) {
            return;
        }
        if (!closeRequested) {
            sink.event("HOST_CLOSE_REQUESTED source=" + source + " generation=" + generation);
        }
        closeRequested = true;
    }

    public void advance(UIPanelAPI activeCore) {
        if (shell == null) {
            closeRequested = false;
            return;
        }
        try {
            if (activeCore != core) {
                release(generation, "HOST_CONTEXT_CHANGED");
                return;
            }
            if (CampaignUiAccess.read(core, "getCurrentTab") != shell) {
                sink.event(
                        "HOST_TAB_CHANGE_OBSERVED nextId="
                                + CampaignUiAccess.read(core, "getCurrentTabId")
                                + " generation="
                                + generation);
                release(generation, "HOST_OR_TAB_CHANGED");
                return;
            }
            if (closeRequested) {
                closeRequested = false;
                dismissOwned();
            }
        } catch (Throwable e) {
            failed = true;
            sink.failure("HOST_LIFECYCLE", e);
            release(generation, "LIFECYCLE_RELEASED");
        }
    }

    private void dismissOwned() throws Exception {
        if (core == null
                || shell == null
                || CampaignUiAccess.read(core, "getCurrentTab") != shell) {
            return;
        }
        MethodRef dismiss = CampaignUiAccess.method(core.getClass(), "dismiss", int.class);
        if (dismiss == null) {
            throw new IllegalStateException("Dismiss capability disappeared");
        }
        dismiss.invoke(core, Integer.valueOf(0));
        if (shell != null && CampaignUiAccess.read(core, "getCurrentTab") != shell) {
            release(generation, "DISMISS_OBSERVED");
        }
    }

    private void release(long ticket, String reason) {
        if (ticket != generation) {
            return;
        }
        boolean owned = shell != null;
        UIPanelAPI released = ownedBody;
        TabSpec.ContentFactory cleanup = bodyFactory;
        ownedBody = null;
        bodyFactory = null;
        shell = null;
        core = null;
        closeRequested = false;
        if (released != null && cleanup != null) {
            try {
                cleanup.dispose(released);
            } catch (Throwable error) {
                sink.failure("CONTENT_DISPOSE", error);
            }
        }
        if (owned) {
            sink.event(reason + " generation=" + ticket);
        }
    }

    /** Drops state without invoking methods on a stale campaign UI. */
    public void reset() {
        release(generation, "RESET_REFERENCES");
        generation++;
        constructor = null;
        observedBase = null;
        observedContract = null;
        failed = false;
        attemptedCoreType = null;
        attemptedVersion = null;
    }

    public boolean isOpen() {
        return shell != null;
    }
}
