package ashlib.shmo.api.general;

public final class EasingFunctions {
    public static final EasingFunction LINEAR = new EasingFunction() {
        @Override
        public float execute(float value) {
            return value;
        }
    };

    public static final EasingFunction EASE_IN_SINE = new EasingFunction() {
        @Override
        public float execute(float value) {
            return easeInSine(value);
        }
    };

    public static final EasingFunction EASE_OUT_SINE = new EasingFunction() {
        @Override
        public float execute(float value) {
            return easeOutSine(value);
        }
    };

    public static final EasingFunction EASE_IN_OUT_SINE = new EasingFunction() {
        @Override
        public float execute(float value) {
            return easeInOutSine(value);
        }
    };

    public static final EasingFunction EASE_IN_QUAD = new EasingFunction() {
        @Override
        public float execute(float value) {
            return easeInQuad(value);
        }
    };

    public static final EasingFunction EASE_OUT_QUAD = new EasingFunction() {
        @Override
        public float execute(float value) {
            return easeOutQuad(value);
        }
    };

    public static final EasingFunction EASE_IN_OUT_QUAD = new EasingFunction() {
        @Override
        public float execute(float value) {
            return easeInOutQuad(value);
        }
    };

    public static final EasingFunction EASE_IN_CUBIC = new EasingFunction() {
        @Override
        public float execute(float value) {
            return easeInCubic(value);
        }
    };

    public static final EasingFunction EASE_OUT_CUBIC = new EasingFunction() {
        @Override
        public float execute(float value) {
            return easeOutCubic(value);
        }
    };

    public static final EasingFunction EASE_IN_OUT_CUBIC = new EasingFunction() {
        @Override
        public float execute(float value) {
            return easeInOutCubic(value);
        }
    };

    public static float easeInSine(float x) { return (float)(1.0 - Math.cos(Utilities.clamp01(x) * Math.PI / 2.0));}
    public static float easeOutSine(float x) { return (float)(Math.sin(Utilities.clamp01(x) * Math.PI  / 2.0)); }
    public static float easeInOutSine(float x) {  return (float)(-(Math.cos(Math.PI * x) - 1.0) / 2.0); }
    public static float easeInQuad(float x) { return x * x; }
    public static float easeOutQuad(float x) { return 1.0f - (1.0f - x) * (1.0f - x); }
    public static float easeInOutQuad(float x) { return (float)(x < 0.5 ? 2.0 * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 2.0) / 2.0); }
    public static float easeInCubic(float x) { return x * x * x; }
    public static float easeOutCubic(float x) { return 1.0f - (1.0f - x) * (1.0f - x) * (1.0f - x); }
    public static float easeInOutCubic(float x) { return (float)(x < 0.5 ? 4.0 * x * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 3.0) / 2.0); }
}
