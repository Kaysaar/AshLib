package ashlib.shmo.api.general;

public final class Curve {
    public static final Curve LINEAR            = create();
    public static final Curve FLAT              = create().withEasingFunction((x) -> 1.0f);
    public static final Curve EASE_IN_SINE      = create().withEasingFunction(EasingFunctions.EASE_IN_SINE);
    public static final Curve EASE_OUT_SINE     = create().withEasingFunction(EasingFunctions.EASE_OUT_SINE);
    public static final Curve EASE_IN_OUT_SINE  = create().withEasingFunction(EasingFunctions.EASE_IN_OUT_SINE);
    public static final Curve EASE_IN_QUAD      = create().withEasingFunction(EasingFunctions.EASE_IN_QUAD);
    public static final Curve EASE_OUT_QUAD     = create().withEasingFunction(EasingFunctions.EASE_OUT_QUAD);
    public static final Curve EASE_IN_OUT_QUAD  = create().withEasingFunction(EasingFunctions.EASE_IN_OUT_QUAD);
    public static final Curve EASE_IN_CUBIC     = create().withEasingFunction(EasingFunctions.EASE_IN_CUBIC);
    public static final Curve EASE_OUT_CUBIC    = create().withEasingFunction(EasingFunctions.EASE_OUT_CUBIC);
    public static final Curve EASE_IN_OUT_CUBIC = create().withEasingFunction(EasingFunctions.EASE_IN_OUT_CUBIC);

    private final float          minDomain;
    private final float          maxDomain;
    private final float          minValue;
    private final float          maxValue;
    private final EasingFunction easingFunction;

    public Curve(
        float          minDomain,
        float          maxDomain,
        float          minValue,
        float          maxValue,
        EasingFunction easingFunction
    ) {
        this.minDomain = minDomain;
        this.maxDomain = maxDomain;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.easingFunction = easingFunction;
    }

    public static Curve create() {
        return new Curve(
                0.0f,
                1.0f,
                0.0f,
                1.0f,
                EasingFunctions.LINEAR
        );
    }

    public Curve withDomainRange(float minDomain, float maxDomain) {
        return new Curve(
                minDomain,
                maxDomain,
                minValue,
                maxValue,
                easingFunction
        );
    }

    public Curve withValueRange(float minValue, float maxValue) {
        return new Curve(
                minDomain,
                maxDomain,
                minValue,
                maxValue,
                easingFunction
        );
    }

    public Curve withEasingFunction(EasingFunction easingFunction) {
        return new Curve(
                minDomain,
                maxDomain,
                minValue,
                maxValue,
                easingFunction
        );
    }

    public float sample(float domainPoint) {
        return Utilities.lerp(
                minValue,
                maxValue,
                easingFunction.execute(Utilities.inverseLerp(domainPoint, minDomain, maxDomain))
        );
    }

    public float getMinDomain() {
        return minDomain;
    }

    public float getMaxDomain() {
        return maxDomain;
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }
}
