package ashlib.data.plugins.coreui.campaignbar.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Composes the native 914x35 frame from pixel-preserving sprite slices. */
final class CampaignFrameSlices {
    static final int SOURCE_WIDTH = 914,
            HEIGHT = 35,
            FIRST = 13,
            SLOT = 122,
            GAP = 6,
            RIGHT_CAP = 11;

    static final class Slice {
        final int sourceX, x, width;

        Slice(int sourceX, int x, int width) {
            this.sourceX = sourceX;
            this.x = x;
            this.width = width;
        }
    }

    final List<Slice> slices;
    final int width, buttonWidth, gap, count;

    CampaignFrameSlices(int count, int buttonWidth, int gap) {
        if (count < 7 || (buttonWidth != 119 && buttonWidth != 122) || (gap != 3 && gap != 6)) {
            throw new IllegalArgumentException("UNSUPPORTED_FRAME_SLICE_PROFILE");
        }
        this.count = count;
        this.buttonWidth = buttonWidth;
        this.gap = gap;
        List<Slice> result = new ArrayList<Slice>();
        int x = 0;
        result.add(new Slice(0, x, FIRST));
        x += FIRST;
        for (int i = 0; i < count; i++) {
            // Reuse native bay artwork without scaling its details.
            int sourceIndex = count == 7 ? i : Math.min(6, i <= 3 ? i : i - 1),
                    source = FIRST + sourceIndex * (SLOT + GAP);
            int left = buttonWidth / 2, right = buttonWidth - left;
            result.add(new Slice(source, x, left));
            x += left;
            // Trim neutral center columns; preserve corner and bevel pixels.
            result.add(new Slice(source + SLOT - right, x, right));
            x += right;
            if (i < count - 1) {
                int separator = FIRST + Math.min(sourceIndex, 5) * (SLOT + GAP) + SLOT;
                int a = gap / 2, b = gap - a;
                result.add(new Slice(separator, x, a));
                x += a;
                result.add(new Slice(separator + GAP - b, x, b));
                x += b;
            }
        }
        result.add(new Slice(SOURCE_WIDTH - RIGHT_CAP, x, RIGHT_CAP));
        x += RIGHT_CAP;
        width = x;
        slices = Collections.unmodifiableList(result);
    }

    /** The original seven native bays; paging controls live beside TRIPAD. */
    static CampaignFrameSlices navigable() {
        return new CampaignFrameSlices(CampaignViewportModel.VISIBLE, CampaignViewportModel.WIDTH,
                CampaignViewportModel.GAP);
    }
}
