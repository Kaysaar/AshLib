package ashlib.shmo.api.general;

import java.awt.*;

public class ColorRange {
    public static final ColorRange WHITE                = ColorRange.of(Color.WHITE, Color.WHITE);
    public static final ColorRange BLACK                = ColorRange.of(Color.BLACK, Color.BLACK);
    public static final ColorRange WHITE_TO_BLACK       = ColorRange.of(Color.WHITE, Color.BLACK);
    public static final ColorRange BLACK_TO_WHITE       = ColorRange.of(Color.BLACK, Color.WHITE);
    public static final ColorRange WHITE_TO_TRANSPARENT = ColorRange.of(Color.WHITE, new Color(255, 255, 255, 0));
    public static final ColorRange TRANSPARENT_TO_WHITE = ColorRange.of( new Color(255, 255, 255, 0), Color.WHITE);
    public static final ColorRange BLACK_TO_TRANSPARENT = ColorRange.of(Color.BLACK, new Color(0, 0, 0, 0));
    public static final ColorRange TRANSPARENT_TO_BLACK = ColorRange.of( new Color(0, 0, 0, 0), Color.BLACK);

    private final Color start;
    private final Color end;

    public ColorRange(Color start, Color end) {
        this.start = start;
        this.end = end;
    }

    public static ColorRange create() { return new ColorRange(Color.BLACK, Color.WHITE); }
    public static ColorRange of(Color start, Color end) { return new ColorRange(start, end); }
    public ColorRange withStart(Color start) { return new ColorRange(start, end); }
    public ColorRange withEnd(Color end) { return new ColorRange(start, end); }
    public Color sample(float point) { return Utilities.lerp(start, end, point); }
    public Color getStart() { return start; }
    public Color getEnd() { return end; }
}
