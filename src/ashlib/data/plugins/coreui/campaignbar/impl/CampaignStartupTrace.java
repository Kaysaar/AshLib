package ashlib.data.plugins.coreui.campaignbar.impl;

import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Per-load monotonic telemetry; values never retain Core instances. */
final class CampaignStartupTrace {
    interface Clock {
        long nanoTime();
    }

    interface Sink {
        void event(String text);
    }

    private final Clock clock;
    private final Sink sink;
    private final long start;
    private final Map<UIPanelAPI, Set<String>> seen = new WeakHashMap<UIPanelAPI, Set<String>>();

    CampaignStartupTrace(Clock clock, Sink sink) {
        this.clock = clock;
        this.sink = sink;
        start = clock.nanoTime();
    }

    boolean hasMarked(UIPanelAPI core, String event) {
        Set<String> events = seen.get(core);
        return events != null && events.contains(event);
    }

    void mark(UIPanelAPI core, String event, String detail) {
        Set<String> events = seen.get(core);
        if (events == null) {
            events = new HashSet<String>();
            seen.put(core, events);
        }
        if (events.add(event)) {
            sink.event(
                    "STARTUP event="
                            + event
                            + " load="
                            + Long.toHexString(start)
                            + " core="
                            + Integer.toHexString(System.identityHashCode(core))
                            + " elapsed_ms="
                            + ms(clock.nanoTime() - start)
                            + " "
                            + detail);
        }
    }

    void duration(UIPanelAPI core, String phase, long began) {
        sink.event(
                "STARTUP_WORK phase="
                        + phase
                        + " core="
                        + Integer.toHexString(System.identityHashCode(core))
                        + " duration_ms="
                        + ms(clock.nanoTime() - began));
    }

    private static double ms(long nanos) {
        return Math.max(0, nanos) / 1000000d;
    }
}
