package ashlib.shmo.api.general;

import java.util.List;

public final class ParticleParams {
    private int          particleCount;
    private boolean      additiveBlending;
    private Range        lifetimeRange;
    private Range2D      offsetRange;
    private Range        sizeRange;
    private Range        sizeRampRange;
    private Curve        sizeRampCurve;
    private ColorRange   colorRange;
    private ColorRange   colorRampRange;
    private Curve        colorRampCurve;
    private Range2D      velocityRange;
    private Range2D      velocityRampRange;
    private Curve        velocityRampCurve;
    private Range        jitterRange;
    private Range        jitterRampRange;
    private Curve        jitterRampCurve;
    private Range        angleRange;
    private Range        spinSpeedRange;
    private Range        spinSpeedRampRange;
    private Curve        spinSpeedRampCurve;
    float                minimumSpeed;
    float                maximumSpeed;
    private List<String> spriteNames;

    public ParticleParams(
            int          particleCount,
            boolean      additiveBlending,
            Range        lifetimeRange,
            Range2D      offsetRange,
            Range        sizeRange,
            Range        sizeRampRange,
            Curve        sizeRampCurve,
            ColorRange   colorRange,
            ColorRange   colorRampRange,
            Curve        colorRampCurve,
            Range2D      velocityRange,
            Range2D      velocityRampRange,
            Curve        velocityRampCurve,
            Range        jitterRange,
            Range        jitterRampRange,
            Curve        jitterRampCurve,
            Range        angleRange,
            Range        spinSpeedRange,
            Range        spinSpeedRampRange,
            Curve        spinSpeedRampCurve,
            float        minimumSpeed,
            float        maximumSpeed,
            List<String> spriteNames
    ) {
        this.particleCount = particleCount;
        this.additiveBlending = additiveBlending;
        this.lifetimeRange = lifetimeRange;
        this.offsetRange = offsetRange;
        this.sizeRange = sizeRange;
        this.sizeRampRange = sizeRampRange;
        this.sizeRampCurve = sizeRampCurve;
        this.colorRange = colorRange;
        this.colorRampRange = colorRampRange;
        this.colorRampCurve = colorRampCurve;
        this.velocityRange = velocityRange;
        this.velocityRampRange = velocityRampRange;
        this.velocityRampCurve = velocityRampCurve;
        this.jitterRange = jitterRange;
        this.jitterRampRange = jitterRampRange;
        this.jitterRampCurve = jitterRampCurve;
        this.angleRange = angleRange;
        this.spinSpeedRange = spinSpeedRange;
        this.spinSpeedRampRange = spinSpeedRampRange;
        this.spinSpeedRampCurve = spinSpeedRampCurve;
        this.minimumSpeed = minimumSpeed;
        this.maximumSpeed = maximumSpeed;
        this.spriteNames = spriteNames;
    }

    public ParticleParams(ParticleParams params) {
        particleCount = params.particleCount;
        additiveBlending = params.additiveBlending;
        lifetimeRange = params.lifetimeRange;
        offsetRange = params.offsetRange;
        sizeRange = params.sizeRange;
        sizeRampRange = params.sizeRampRange;
        sizeRampCurve = params.sizeRampCurve;
        colorRange = params.colorRange;
        colorRampRange = params.colorRampRange;
        colorRampCurve = params.colorRampCurve;
        velocityRange = params.velocityRange;
        velocityRampRange = params.velocityRampRange;
        velocityRampCurve = params.velocityRampCurve;
        jitterRange = params.jitterRange;
        jitterRampRange = params.jitterRampRange;
        jitterRampCurve = params.jitterRampCurve;
        angleRange = params.angleRange;
        spinSpeedRange = params.spinSpeedRange;
        spinSpeedRampRange = params.spinSpeedRampRange;
        spinSpeedRampCurve = params.spinSpeedRampCurve;
        minimumSpeed = params.minimumSpeed;
        maximumSpeed = params.maximumSpeed;
        spriteNames = params.spriteNames;
    }

    private ParticleParams copy() {
        return new ParticleParams(
                particleCount,
                additiveBlending,
                lifetimeRange,
                offsetRange,
                sizeRange,
                sizeRampRange,
                sizeRampCurve,
                colorRange,
                colorRampRange,
                colorRampCurve,
                velocityRange,
                velocityRampRange,
                velocityRampCurve,
                jitterRange,
                jitterRampRange,
                jitterRampCurve,
                angleRange,
                spinSpeedRange,
                spinSpeedRampRange,
                spinSpeedRampCurve,
                minimumSpeed,
                maximumSpeed,
                spriteNames
        );
    }

    public static ParticleParams create() {
        return new ParticleParams(
                8,
                true,
                Range.ONE,
                Range2D.ZERO,
                Range.ONE,
                Range.ONE,
                Curve.LINEAR,
                ColorRange.WHITE,
                ColorRange.WHITE,
                Curve.LINEAR,
                Range2D.ZERO,
                Range2D.ONE,
                Curve.LINEAR,
                Range.ZERO,
                Range.ONE,
                Curve.LINEAR,
                Range.ZERO,
                Range.ZERO,
                Range.ONE,
                Curve.LINEAR,
                -1.0f,
                -1.0f,
                List.of()
        );
    }

    public ParticleParams withParticleCount(int particleCount) {
        ParticleParams newParams = copy();
        newParams.particleCount = particleCount;
        return newParams;
    }

    public ParticleParams withAdditiveBlending() {
        ParticleParams newParams = copy();
        newParams.additiveBlending = true;
        return newParams;
    }

    public ParticleParams withNormalBlending() {
        ParticleParams newParams = copy();
        newParams.additiveBlending = false;
        return newParams;
    }

    public ParticleParams withLifetimeRange(Range lifetimeRange) {
        ParticleParams newParams = copy();
        newParams.lifetimeRange = lifetimeRange;
        return newParams;
    }

    public ParticleParams withOffsetRange(Range2D offsetRange) {
        ParticleParams newParams = copy();
        newParams.offsetRange = offsetRange;
        return newParams;
    }

    public ParticleParams withSizeRange(Range sizeRange) {
        ParticleParams newParams = copy();
        newParams.sizeRange = sizeRange;
        return newParams;
    }

    public ParticleParams withSizeRamp(Range sizeRampRange, Curve sizeRampCurve) {
        ParticleParams newParams = copy();
        newParams.sizeRampRange = sizeRampRange;
        newParams.sizeRampCurve = sizeRampCurve;
        return newParams;
    }

    public ParticleParams withColorRange(ColorRange colorRange) {
        ParticleParams newParams = copy();
        newParams.colorRange = colorRange;
        return newParams;
    }

    public ParticleParams withColorRamp(ColorRange colorRampRange, Curve colorRampCurve) {
        ParticleParams newParams = copy();
        newParams.colorRampRange = colorRampRange;
        newParams.colorRampCurve = colorRampCurve;
        return newParams;
    }

    public ParticleParams withVelocityRange(Range2D velocityRange) {
        ParticleParams newParams = copy();
        newParams.velocityRange = velocityRange;
        return newParams;
    }

    public ParticleParams withVelocityRamp(Range2D velocityRampRange, Curve velocityRampCurve) {
        ParticleParams newParams = copy();
        newParams.velocityRampRange = velocityRampRange;
        newParams.velocityRampCurve = velocityRampCurve;
        return newParams;
    }

    public ParticleParams withJitterRange(Range jitterRange) {
        ParticleParams newParams = copy();
        newParams.jitterRange = jitterRange;
        return newParams;
    }

    public ParticleParams withJitterRamp(Range jitterRampRange, Curve jitterRampCurve) {
        ParticleParams newParams = copy();
        newParams.jitterRampRange = jitterRampRange;
        newParams.jitterRampCurve = jitterRampCurve;
        return newParams;
    }

    public ParticleParams withAngleRange(Range angleRange) {
        ParticleParams newParams = copy();
        newParams.angleRange = angleRange;
        return newParams;
    }

    public ParticleParams withSpinSpeedRange(Range spinSpeedRange) {
        ParticleParams newParams = copy();
        newParams.spinSpeedRange = spinSpeedRange;
        return newParams;
    }

    public ParticleParams withSpinSpeedRamp(Range spinSpeedRampRange, Curve spinSpeedRampCurve) {
        ParticleParams newParams = copy();
        newParams.spinSpeedRampRange = spinSpeedRampRange;
        newParams.spinSpeedRampCurve = spinSpeedRampCurve;
        return newParams;
    }

    public ParticleParams withMinimumSpeed(float minimumSpeed) {
        ParticleParams newParams = copy();
        newParams.minimumSpeed = minimumSpeed;
        return newParams;
    }

    public ParticleParams withMaximumSpeed(float maximumSpeed) {
        ParticleParams newParams = copy();
        newParams.maximumSpeed = maximumSpeed;
        return newParams;
    }

    public ParticleParams withoutMinimumSpeed() {
        ParticleParams newParams = copy();
        newParams.minimumSpeed = -1.0f;
        return newParams;
    }

    public ParticleParams withoutMaximumSpeed() {
        ParticleParams newParams = copy();
        newParams.maximumSpeed = -1.0f;
        return newParams;
    }

    public ParticleParams withSprites(List<String> spriteNames) {
        ParticleParams newParams = copy();
        newParams.spriteNames = spriteNames;
        return newParams;
    }

    public boolean isAdditiveBlending() {
        return additiveBlending;
    }

    public ColorRange getColorRampRange() {
        return colorRampRange;
    }

    public ColorRange getColorRange() {
        return colorRange;
    }

    public Curve getColorRampCurve() {
        return colorRampCurve;
    }

    public Curve getJitterRampCurve() {
        return jitterRampCurve;
    }

    public Curve getSizeRampCurve() {
        return sizeRampCurve;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public Curve getVelocityRampCurve() {
        return velocityRampCurve;
    }

    public Range getAngleRange() {
        return angleRange;
    }

    public Curve getSpinSpeedRampCurve() {
        return spinSpeedRampCurve;
    }

    public Range getJitterRampRange() {
        return jitterRampRange;
    }

    public Range getJitterRange() {
        return jitterRange;
    }

    public Range getLifetimeRange() {
        return lifetimeRange;
    }

    public Range getSizeRampRange() {
        return sizeRampRange;
    }

    public List<String> getSpriteNames() {
        return spriteNames;
    }

    public Range getSizeRange() {
        return sizeRange;
    }

    public Range getSpinSpeedRampRange() {
        return spinSpeedRampRange;
    }

    public Range getSpinSpeedRange() {
        return spinSpeedRange;
    }

    public Range2D getOffsetRange() {
        return offsetRange;
    }

    public Range2D getVelocityRampRange() {
        return velocityRampRange;
    }

    public Range2D getVelocityRange() {
        return velocityRange;
    }

    public float getMaximumSpeed() {
        return maximumSpeed;
    }

    public boolean hasMaximumSpeed() {
        return maximumSpeed >= 0.0f;
    }

    public float getMinimumSpeed() {
        return minimumSpeed;
    }

    public boolean hasMinimumSpeed() {
        return minimumSpeed >= 0.0f;
    }
}
