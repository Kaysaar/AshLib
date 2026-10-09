package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.api.TabSpec;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.ButtonAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Ordered identities, independent of visual slots. Vanilla relative order is invariant. */
public final class CampaignTabCollection {
    public static final List<CoreUITabId> VANILLA =
            Collections.unmodifiableList(
                    Arrays.asList(
                            CoreUITabId.CHARACTER,
                            CoreUITabId.FLEET,
                            CoreUITabId.REFIT,
                            CoreUITabId.CARGO,
                            CoreUITabId.MAP,
                            CoreUITabId.INTEL,
                            CoreUITabId.OUTPOSTS));
    private final List<Object> ids;
    public final TabSpec extension;
    public final List<TabSpec> extensions;

    public CampaignTabCollection(TabSpec spec) {
        this(Collections.singletonList(spec));
    }

    public CampaignTabCollection(Collection<TabSpec> specs) {
        // ID order breaks ties for the same anchor/placement, regardless of registration order.
        Map<String, TabSpec> sorted = new TreeMap<String, TabSpec>();
        Set<Integer> keys = new HashSet<Integer>();
        for (TabSpec spec : specs) {
            if (spec == null || sorted.put(spec.id, spec) != null) {
                throw new IllegalArgumentException("Duplicate/null registration");
            }
            if (spec.key != 0 && !keys.add(spec.key)) {
                throw new IllegalArgumentException("DUPLICATE_EXTENSION_HOTKEY " + spec.key);
            }
        }
        extensions = Collections.unmodifiableList(new ArrayList<TabSpec>(sorted.values()));
        extension = extensions.isEmpty() ? null : extensions.get(0);
        for (TabSpec spec : extensions) {
            if (!VANILLA.contains(spec.anchor) && !sorted.containsKey(spec.anchor)) {
                throw new IllegalArgumentException(
                        "UNRESOLVED_ANCHOR " + spec.id + " -> " + spec.anchor);
            }
        }
        List<Object> ordered = new ArrayList<Object>();
        Set<Object> visiting = new HashSet<Object>(), visited = new HashSet<Object>();
        for (CoreUITabId nativeId : VANILLA) {
            append(nativeId, ordered, visiting, visited);
        }
        if (ordered.size() != VANILLA.size() + extensions.size()) {
            throw new IllegalArgumentException("CYCLIC_REGISTRATION_ANCHORS");
        }
        ids = Collections.unmodifiableList(ordered);
    }

    private void append(
            Object id, List<Object> ordered, Set<Object> visiting, Set<Object> visited) {
        if (!visiting.add(id)) {
            throw new IllegalArgumentException("CYCLIC_REGISTRATION_ANCHORS " + id);
        }
        for (TabSpec tabSpec : extensions) {
            if (tabSpec.placement == TabSpec.Placement.BEFORE && tabSpec.anchor.equals(id)) {
                append(tabSpec.id, ordered, visiting, visited);
            }
        }
        if (!visited.add(id)) {
            throw new IllegalArgumentException("DUPLICATE_ORDERED_ID " + id);
        }
        ordered.add(id);
        for (TabSpec tabSpec : extensions) {
            if (tabSpec.placement == TabSpec.Placement.AFTER && tabSpec.anchor.equals(id)) {
                append(tabSpec.id, ordered, visiting, visited);
            }
        }
        visiting.remove(id);
    }

    public List<Object> ids() {
        return ids;
    }

    public int size() {
        return ids.size();
    }

    public int extensionIndex() {
        return extension == null ? -1 : ids.indexOf(extension.id);
    }

    public int indexOf(String id) {
        return ids.indexOf(id);
    }

    public List<ButtonAPI> buttons(Map<?, ?> registry) {
        List<ButtonAPI> ordered = new ArrayList<ButtonAPI>();
        for (Object id : ids) {
            Object value = registry.get(id);
            if (!(value instanceof ButtonAPI) || ordered.contains(value)) {
                throw new IllegalStateException("Missing or duplicate tab " + id);
            }
            ordered.add((ButtonAPI) value);
        }
        return ordered;
    }
}
