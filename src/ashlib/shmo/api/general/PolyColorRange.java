package ashlib.shmo.api.general;


import java.awt.Color;
import java.util.List;

public class PolyColorRange extends ColorRange {
    private final List<Color> midpoints;

    public PolyColorRange(Color start, Color end) {
        super(start, end);
        midpoints = null;
    }

    public PolyColorRange(Color start, List<Color> midpoints, Color end) {
        super(start, end);
        if (midpoints.isEmpty()) {
            this.midpoints = List.of();
            return;
        }
        this.midpoints = midpoints;
    }

    public static PolyColorRange create() {
        return new PolyColorRange(Color.BLACK, Color.WHITE);
    }

    public static PolyColorRange of(Color start, List<Color> midpoints, Color end) {
        return new PolyColorRange(start, midpoints, end);
    }

    @Override
    public ColorRange withStart(Color start) {
        return new PolyColorRange(start, midpoints, getEnd());
    }

    @Override
    public ColorRange withEnd(Color end) {
        return new PolyColorRange(getStart(), midpoints, end);
    }

    public ColorRange withMidpoints(List<Color> midpoints) {
        return new PolyColorRange(getStart(), midpoints, getEnd());
    }

    public List<Color> getMidpoints() {
        return midpoints;
    }

    @Override
    public Color sample(float point) {
        if (midpoints == null || midpoints.isEmpty()) {
            return super.sample(point);
        }

        int segments = midpoints.size() + 1; // number of intervals between control colors
        float scaled = point * segments;

        int index = (int) Math.floor(scaled);
        if (index < 0) {
            index = 0;
        } else if (index >= segments) {
            index = segments - 1;
        }

        float localT = scaled - index;

        Color from = (index == 0) ? getStart() : midpoints.get(index - 1);
        Color to = (index == segments - 1) ? getEnd() : midpoints.get(index);

        return Utilities.lerp(from, to, localT);
    }
}
