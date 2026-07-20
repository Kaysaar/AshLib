package ashlib.shmo.aotd.lib.api.general;

public class Range {
    public static final Range ZERO             = Range.of(0.0f, 0.0f);
    public static final Range ONE              = Range.of(1.0f, 1.0f);
    public static final Range ZERO_TO_ONE      = Range.of(0.0f, 1.0f);
    public static final Range ONE_TO_ZERO      = Range.of(1.0f, 0.0f);
    public static final Range MINUS_ONE_TO_ONE = Range.of(-1.0f, 1.0f);
    public static final Range ONE_TO_MINUS_ONE = Range.of(1.0f, -1.0f);

    private final float start;
    private final float end;

    public Range(float start, float end) {
        this.start = start;
        this.end = end;
    }

    public static Range create() { return new Range(0.0f, 1.0f); }
    public static Range of(float start, float end) { return new Range(start, end); }
    public Range withStart(float start) { return new Range(start, end); }
    public Range withEnd(float end) { return new Range(start, end); }

    public float sample(float point) {
        return Utilities.lerp(start, end, point);
    }

    public float getStart() {
        return start;
    }

    public float getEnd() {
        return end;
    }
}
