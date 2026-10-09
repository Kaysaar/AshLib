package ashlib.data.plugins.coreui.campaignbar.api;

import ashlib.data.plugins.coreui.campaignbar.impl.CampaignBarManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Register tab specifications from onApplicationLoad; create UI panels through the content factory. */
public final class CampaignBar {
    private static final Map<String, TabSpec> registrations = new TreeMap<String, TabSpec>();

    private CampaignBar() {}

    public static synchronized void register(TabSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("Null registration");
        }
        TabSpec previous = registrations.get(spec.id);
        // The same instance may register again; another instance with this ID is rejected.
        if (previous == spec) {
            return;
        }
        if (previous != null) {
            throw new IllegalStateException("DUPLICATE_REGISTRATION_ID " + spec.id);
        }
        registrations.put(spec.id, spec);
    }

    /** AshLibPlugin lifecycle hook; consumers only register tabs. */
    public static void onGameLoad() {
        CampaignBarManager.onGameLoad();
    }

    /** Legacy single-consumer accessor. New consumers use registrations(). */
    public static synchronized TabSpec registration() {
        return registrations.isEmpty() ? null : registrations.values().iterator().next();
    }

    /** Returns an immutable snapshot of registered tab specifications. */
    public static synchronized List<TabSpec> registrations() {
        return Collections.unmodifiableList(new ArrayList<TabSpec>(registrations.values()));
    }
}
