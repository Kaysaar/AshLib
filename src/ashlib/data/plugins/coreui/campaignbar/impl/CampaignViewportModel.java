package ashlib.data.plugins.coreui.campaignbar.impl;

/** Pure window geometry; no UI objects or campaign state. */
public final class CampaignViewportModel {
    public static final int COUNT = 8, VISIBLE = 7, WIDTH = 122, GAP = 6, HEIGHT = 35;
    public static final int STRIDE = WIDTH + GAP,
            VIEW_WIDTH = VISIBLE * STRIDE - GAP,
            TRACK_WIDTH = COUNT * STRIDE - GAP;
    private final int count;
    private int index;

    public CampaignViewportModel() {
        this(COUNT);
    }

    public CampaignViewportModel(int count) {
        if (count < VISIBLE) {
            throw new IllegalArgumentException("TOO_FEW_TABS");
        }
        this.count = count;
    }

    public int count() {
        return count;
    }

    public int maxIndex() {
        return count - VISIBLE;
    }

    public int trackWidth() {
        return count * STRIDE - GAP;
    }

    public int index() {
        return index;
    }

    public void setIndex(int value) {
        if (value < 0 || value > maxIndex()) {
            throw new IllegalArgumentException("INDEX_OUT_OF_RANGE " + value);
        }
        index = value;
    }

    public float offset() {
        return -index * STRIDE;
    }

    public boolean visible(int slot) {
        return slot >= index && slot < index + VISIBLE;
    }

    public int nearestIndex(int slot) {
        if (slot < 0 || slot >= count) {
            throw new IllegalArgumentException("SLOT_OUT_OF_RANGE " + slot);
        }
        return slot < index ? slot : slot >= index + VISIBLE ? slot - VISIBLE + 1 : index;
    }

    public int hit(float localX, float localY) {
        if (localX < 0 || localX >= VIEW_WIDTH || localY < 4 || localY >= 25) {
            return -1;
        }
        int cell = (int) localX / STRIDE;
        return localX - cell * STRIDE < WIDTH ? index + cell : -1;
    }

    public static boolean contains(float x, float y, float w, float h, float px, float py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
}
