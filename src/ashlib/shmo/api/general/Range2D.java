package ashlib.shmo.api.general;

public class Range2D {
    public static final Range2D ZERO        = Range2D.of(0.0f, 0.0f, 0.0f, 0.0f);
    public static final Range2D ONE         = Range2D.of(1.0f, 1.0f, 1.0f, 1.0f);
    public static final Range2D ZERO_TO_ONE = Range2D.of(0.0f, 1.0f, 0.0f, 1.0f);
    public static final Range2D ONE_TO_ZERO = Range2D.of(1.0f, 0.0f, 1.0f, 0.0f);

    private final float startX;
    private final float endX;
    private final float startY;
    private final float endY;

    public Range2D(float startX, float endX, float startY, float endY) {
        this.startX = startX;
        this.endX   = endX;
        this.startY = startY;
        this.endY   = endY;
    }

    public static Range2D create() { return new Range2D(0.0f, 1.0f, 0.0f, 0.0f); }
    public static Range2D of(float startX, float endX, float startY, float endY) { return new Range2D(startX, endX, startY, endY); }
    public Range2D withStartX(float startX) { return new Range2D(startX, endX, startY, endY); }
    public Range2D withEndX(float endX) { return new Range2D(startX, endX, startY, endY); }
    public Range2D withStartY(float startY) { return new Range2D(startX, endX, startY, endY); }
    public Range2D withEndY(float endY) { return new Range2D(startX, endX, startY, endY); }

    public float sampleX(float point) {
        return Utilities.lerp(startX, endX, point);
    }

    public float sampleY(float point) {
        return Utilities.lerp(startY, endY, point);
    }

    public float getStartX() {
        return startX;
    }

    public float getEndX() {
        return endX;
    }

    public float getStartY() {
        return startY;
    }

    public float getEndY() {
        return endY;
    }
}
