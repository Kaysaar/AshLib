package ashlib.data.plugins.coreui.campaignbar.api;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.UIPanelAPI;

/** Immutable tab registration that preserves the relative order of native tabs. */
public final class TabSpec {
    public enum Placement {
        BEFORE,
        AFTER
    }

    public interface ContentFactory {
        UIPanelAPI create(float width, float height, Runnable requestClose);

        /** Releases each returned panel once, including after refused or deferred opens. Do not modify native tabs. */
        default void dispose(UIPanelAPI panel) {}
    }

    public final String id, title;

    /** Native anchor retained for API compatibility; null for string anchors. */
    public final CoreUITabId afterNative;

    /** CoreUITabId, or a namespaced registration ID. Forward references resolve on game load. */
    public final Object anchor;

    public final Placement placement;
    public final int key, mnemonicIndex;
    public final ContentFactory content;

    public TabSpec(
            String id,
            String title,
            CoreUITabId afterNative,
            int key,
            int mnemonicIndex,
            ContentFactory content) {
        this(id, title, afterNative, Placement.AFTER, key, mnemonicIndex, content);
    }

    public TabSpec(
            String id,
            String title,
            Object anchor,
            Placement placement,
            int key,
            int mnemonicIndex,
            ContentFactory content) {
        if (id == null
                || !id.matches("[a-z0-9_.-]+:[a-z0-9_.-]+")
                || title == null
                || title.length() == 0
                || (!(anchor instanceof CoreUITabId)
                        && !(anchor instanceof String
                                && ((String) anchor).matches("[a-z0-9_.-]+:[a-z0-9_.-]+")))
                || id.equals(anchor)
                || placement == null
                || content == null) {
            throw new IllegalArgumentException("Invalid tab registration / anchor");
        }
        if (key < 0
                || key > 255
                || (key == 0
                        ? mnemonicIndex != -1
                        : mnemonicIndex < 0 || mnemonicIndex >= title.length())) {
            throw new IllegalArgumentException("Invalid native key/mnemonic");
        }
        // Native setShortcut highlights only the first matching letter.
        if (key != 0) {
            String keyName = org.lwjgl.input.Keyboard.getKeyName(key);
            String letter = title.substring(mnemonicIndex, mnemonicIndex + 1);
            if (keyName == null
                    || keyName.length() != 1
                    || !keyName.equalsIgnoreCase(letter)
                    || title.toLowerCase(java.util.Locale.ROOT)
                                    .indexOf(letter.toLowerCase(java.util.Locale.ROOT))
                            != mnemonicIndex) {
                throw new IllegalArgumentException(
                        "Mnemonic cannot be represented by the native renderer");
            }
        }
        this.id = id;
        this.title = title;
        this.anchor = anchor;
        this.placement = placement;
        this.afterNative = anchor instanceof CoreUITabId ? (CoreUITabId) anchor : null;
        this.key = key;
        this.mnemonicIndex = mnemonicIndex;
        this.content = content;
    }
}
