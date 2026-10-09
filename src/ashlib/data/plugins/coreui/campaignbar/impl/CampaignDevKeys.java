package ashlib.data.plugins.coreui.campaignbar.impl;

/** Detects key transitions from supplied states without consuming input events. */
public final class CampaignDevKeys {
    public interface Sink {
        void event(String message);

        void probe();

        void toggleHost();
    }

    private final Sink sink;
    private boolean initialized,
            enabledBefore,
            readyBefore,
            f9Before,
            f10Before,
            probeBefore,
            hostBefore;

    public CampaignDevKeys(Sink sink) {
        this.sink = sink;
    }

    public void update(
            boolean enabled, boolean ready, boolean ctrl, boolean shift, boolean f9, boolean f10) {
        if (!initialized || enabled != enabledBefore || ready != readyBefore) {
            sink.event(
                    "DEV_INPUT_STATUS enabled="
                            + enabled
                            + " keyboardReady="
                            + ready
                            + " mode=NON_CONSUMING_POLL");
            initialized = true;
            enabledBefore = enabled;
            readyBefore = ready;
        }
        if (!enabled || !ready) {
            f9Before = false;
            f10Before = false;
            probeBefore = false;
            hostBefore = false;
            return;
        }
        if (f9 && !f9Before) {
            sink.event("KEY_OBSERVED key=F9 ctrl=" + ctrl + " shift=" + shift);
        }
        if (f10 && !f10Before) {
            sink.event("KEY_OBSERVED key=F10 ctrl=" + ctrl + " shift=" + shift);
        }
        boolean probe = ctrl && shift && f9, host = ctrl && shift && f10;
        if (probe && !probeBefore) {
            sink.event("HOTKEY_RECOGNIZED chord=CTRL_SHIFT_F9");
            sink.probe();
        }
        if (host && !hostBefore) {
            sink.event("HOTKEY_RECOGNIZED chord=CTRL_SHIFT_F10");
            sink.toggleHost();
        }
        f9Before = f9;
        f10Before = f10;
        probeBefore = probe;
        hostBefore = host;
    }
}
