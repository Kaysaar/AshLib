package ashlib.shmo.impl.campaign;

import ashlib.shmo.api.ShmoGlobal;
import ashlib.shmo.api.campaign.CampaignData;
import ashlib.shmo.api.campaign.StarSiphonEffect;
import ashlib.shmo.api.campaign.StarSiphonParams;
import ashlib.shmo.api.general.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.util.Misc;

import org.lwjgl.util.vector.Vector2f;


import java.awt.Color;
import java.util.List;

class StarSiphonEffectImpl implements StarSiphonEffect {
    private static final ParticleParams PARTICLE_PARAMS = ParticleParams.create()
            .withParticleCount(500)
            .withLifetimeRange(Range.of(1.5f, 2.5f))
            .withSizeRange(Range.of(6.0f, 14.0f))
            .withSizeRamp(Range.ONE.withEnd(0.4f), Curve.EASE_IN_SINE)
            .withAngleRange(Range.of(-180.0f, 180.0f))
            .withSpinSpeedRange(Range.of(-180.0f, 180.0f))
            .withVelocityRange(Range2D.of(-300, 300, -300, 300))
            .withVelocityRamp(Range2D.ONE.withEndX(0.0f).withEndY(0.0f), Curve.EASE_IN_CUBIC)
            .withMinimumSpeed(50.0f)
            .withColorRamp(PolyColorRange.of(
                            new Color(100,0,0,25),
                            java.util.List.of(
                                    Color.WHITE,
                                    Color.WHITE,
                                    Color.WHITE
                            ),
                            new Color(80,80,80,0)),
                    Curve.EASE_IN_CUBIC
            )
            .withJitterRange(Range.of(0.0f, 50.0f))
            .withJitterRamp(Range.of(1.0f, 10.0f), Curve.EASE_IN_QUAD)
            .withAdditiveBlending();

    private static final String PARTICLE_SYSTEM_KEY        = "particleSystem";
    private static final String BACKGROUND_SPRITE_KEY      = "backgroundSprite";
    private static final String BACKGROUND_SPRITE_TINT_KEY = "backgroundSpriteTint";
    private static final String MIDGROUND_SPRITE_KEY       = "midgroundSprite";
    private static final String MIDGROUND_SPRITE_TINT_KEY  = "midgroundSpriteTint";
    private static final String FOREGROUND_SPRITE_KEY      = "foregroundSprite";
    private static final String FOREGROUND_SPRITE_TINT_KEY = "foregroundSpriteTint";
    private static final String FLARE_SPRITE_KEY           = "flareSprite";
    private static final String FLARE_SPRITE_TINT_KEY      = "flareSpriteTint";
    private static final String FLASH_SPRITE_KEY           = "flashSprite";
    private static final String FLASH_SPRITE_TINT_KEY      = "flashSpriteTint";
    private static final String IN_SOUND_KEY               = "inSound";
    private static final String OUT_SOUND_KEY              = "outSound";
    private static final String LOOP_SOUND_KEY             = "loopSound";
    private static final String LOOP_SOUND_SOURCE_1_KEY    = "loopSoundSource1";
    private static final String LOOP_SOUND_SOURCE_2_KEY    = "loopSoundSource2";
    private static final String STATE_KEY                  = "state";
    private static final String STATE_PROGRESS_KEY         = "stateProgress";
    private static final String PARAMS_KEY                 = "params";
    private static final String STAR_KEY                   = "star";
    private static final String CONSUMER_KEY               = "consumer";
    private static final String TIME_ELAPSED_KEY           = "timeElapsed";
    private static final String CONSUMER_LOCATION_KEY      = "consumerLocation";
    private static final String CONSUMER_RADIUS_KEY        = "consumerRadius";
    private static final String STAR_LOCATION_KEY          = "starLocation";
    private static final String STAR_RADIUS_KEY            = "starRadius";
    private static final String STAR_LIGHT_COLOR_KEY       = "starLightColor";
    private static final String TARGET_INTENSITY_KEY       = "targetIntensity";
    private static final String INTENSITY_KEY              = "intensity";
    private static final String TARGET_FLARE_INTENSITY_KEY = "targetFlareIntensity";
    private static final String FLARE_INTENSITY_KEY        = "flareIntensity";
    private static final String DAMPED_FLARE_INTENSITY_KEY = "dampedFlareIntensity";
    private static final String FLASH_INTENSITY_KEY        = "flashIntensity";
    private static final String DAMPED_FLASH_INTENSITY_KEY = "dampedFlashIntensity";
    private static final String TIME_SCALE_KEY             = "timeScale";
    private static final String COLLECTION_OFFSET_KEY      = "collectionOffset";

    private static final float MAX_PARTICLE_RANGE          = 3000.0f;

    private static final float IN_STATE_PROGRESS_RATE      = 1.0f;
    private static final float ACTIVE_STATE_PROGRESS_RATE  = 0.5f;
    private static final float OUT_STATE_PROGRESS_RATE     = 0.25f;

    private static final float FLARE_DAMP_AMOUNT           = 5.0f;
    private static final float FLASH_DAMP_AMOUNT           = 10.0f;
    private static final float FLASH_REDUCE_RATE           = 0.5f;

    private static final float BACKGROUND_SCROLL_SPEED     = 0.25f;
    private static final float MIDGROUND_SCROLL_SPEED      = 0.5f;
    private static final float FOREGROUND_SCROLL_SPEED     = 1.0f;

    private enum State {
        INACTIVE,
        IN,
        ACTIVE,
        OUT
    }

    private final CampaignData data = new MapCampaignData();

    public StarSiphonEffectImpl() {
        setParams(StarSiphonParams.create());
    }

    public StarSiphonEffectImpl(StarSiphonParams params) {
        setParams(params);
    }

    private ParticleSystem getParticleSystem() {
        return data.getOrConstruct(PARTICLE_SYSTEM_KEY, () -> ShmoGlobal.getGeneralFactory().createParticleSystem(PARTICLE_PARAMS));
    }

    private SavableSprite getBackgroundSprite() {
        return data.getOrConstruct(BACKGROUND_SPRITE_KEY, SavableSprite::create);
    }

    private void setBackgroundSprite(String spriteName) {
        getBackgroundSprite().changeSprite(spriteName);
    }

    private Color getBackgroundTint() {
        return data.getOrConstruct(BACKGROUND_SPRITE_TINT_KEY, () -> Color.WHITE);
    }

    private void setBackgroundTint(Color tint) {
        data.set(BACKGROUND_SPRITE_TINT_KEY, tint);
    }

    private SavableSprite getMidgroundSprite() {
        return data.getOrConstruct(MIDGROUND_SPRITE_KEY, SavableSprite::create);
    }

    private void setMidgroundSprite(String spriteName) {
        getMidgroundSprite().changeSprite(spriteName);
    }

    private Color getMidgroundTint() {
        return data.getOrConstruct(MIDGROUND_SPRITE_TINT_KEY, () -> Color.WHITE);
    }

    private void setMidgroundTint(Color tint) {
        data.set(MIDGROUND_SPRITE_TINT_KEY, tint);
    }

    private SavableSprite getForegroundSprite() {
        return data.getOrConstruct(FOREGROUND_SPRITE_KEY, SavableSprite::create);
    }

    private void setForegroundSprite(String spriteName) {
        getForegroundSprite().changeSprite(spriteName);
    }

    private Color getForegroundTint() {
        return data.getOrConstruct(FOREGROUND_SPRITE_TINT_KEY, () -> Color.WHITE);
    }

    private void setForegroundTint(Color tint) {
        data.set(FOREGROUND_SPRITE_TINT_KEY, tint);
    }

    private SavableSprite getFlareSprite() {
        return data.getOrConstruct(FLARE_SPRITE_KEY, SavableSprite::create);
    }

    private void setFlareSprite(String spriteName) {
        getFlareSprite().changeSprite(spriteName);
    }

    private Color getFlareTint() {
        return data.getOrConstruct(FLARE_SPRITE_TINT_KEY, () -> Color.WHITE);
    }

    private void setFlareTint(Color tint) {
        data.set(FLARE_SPRITE_TINT_KEY, tint);
    }

    private SavableSprite getFlashSprite() {
        return data.getOrConstruct(FLASH_SPRITE_KEY, SavableSprite::create);
    }

    private void setFlashSprite(String spriteName) {
        getFlashSprite().changeSprite(spriteName);
    }

    private Color getFlashTint() {
        return data.getOrConstruct(FLASH_SPRITE_TINT_KEY, () -> Color.WHITE);
    }

    private void setFlashTint(Color tint) {
        data.set(FLASH_SPRITE_TINT_KEY, tint);
    }

    private SoundParams getInSound() {
        return data.getOrConstruct(IN_SOUND_KEY, SoundParams::create);
    }

    private void setInSound(SoundParams sound) {
        data.set(IN_SOUND_KEY, sound);
    }

    private SoundParams getOutSound() {
        return data.getOrConstruct(OUT_SOUND_KEY, SoundParams::create);
    }

    private void setOutSound(SoundParams sound) {
        data.set(OUT_SOUND_KEY, sound);
    }

    private SoundParams getLoopSound() {
        return data.getOrConstruct(LOOP_SOUND_KEY, SoundParams::create);
    }

    private void setLoopSound(SoundParams sound) {
        data.set(LOOP_SOUND_KEY, sound);
    }

    private Object getLoopSoundSource1() {
        return data.getOrConstruct(LOOP_SOUND_SOURCE_1_KEY, Object::new);
    }

    private Object getLoopSoundSource2() {
        return data.getOrConstruct(LOOP_SOUND_SOURCE_2_KEY, Object::new);
    }


    private State getState() {
        return data.getOrConstruct(STATE_KEY, () -> State.INACTIVE);
    }

    private void setState(State state) {
        State currentState = getState();
        if (currentState == state) { return; }
        exitState(currentState);
        enterState(state);
        data.set(STATE_KEY, state);
    }

    private float getStateProgress() {
        return data.getOrConstruct(STATE_PROGRESS_KEY, () -> 0.0f);
    }

    private void setStateProgress(float progress) {
        data.set(STATE_PROGRESS_KEY, Utilities.clamp01(progress));
    }

    private void increaseStateProgress(float amount) {
        setStateProgress(getStateProgress() + amount);
    }

    private boolean isStateFinished() {
        return getStateProgress() >= 1.0f;
    }

    private List<String> getParticleSpriteNames() {
        return getParticleSystem().getParams().getSpriteNames();
    }

    private void setParticleSprites(List<String> spriteNames) {
        final ParticleSystem particleSystem = getParticleSystem();
        particleSystem.setParams(particleSystem.getParams().withSprites(spriteNames));
    }

    private void setParticleTint(Color tint) {
        final ParticleSystem particleSystem = getParticleSystem();
        particleSystem.setParams(particleSystem.getParams().withColorRamp(
                PolyColorRange.of(
                        new Color(100,0,0,25),
                        java.util.List.of(
                                tint,
                                tint,
                                tint
                        ),
                        new Color(80,80,80,0)),
                Curve.EASE_IN_CUBIC
        ));
    }

    private float getElapsedTime() {
        return data.getOrConstruct(TIME_ELAPSED_KEY, () -> 0.0f);
    }

    private void advanceElapsedTime(float amount) {
        data.set(TIME_ELAPSED_KEY, getElapsedTime() + amount);
    }

    private void applyParams(StarSiphonParams params) {
        getParticleSystem().setParams(PARTICLE_PARAMS);
        setBackgroundSprite(params.getBackgroundSpriteName());
        setBackgroundTint(params.getBackgroundTint());
        setMidgroundSprite(params.getMidgroundSpriteName());
        setMidgroundTint(params.getMidgroundTint());
        setForegroundSprite(params.getForegroundSpriteName());
        setForegroundTint(params.getForegroundTint());
        setFlareSprite(params.getFlareSpriteName());
        setFlareTint(params.getFlareTint());
        setFlashSprite(params.getFlashSpriteName());
        setFlashTint(params.getFlashTint());
        setParticleSprites(params.getParticleSpriteNames());
        setParticleTint(params.getParticleTint());
        setInSound(params.getInSound());
        setOutSound(params.getOutSound());
        setLoopSound(params.getLoopSound());
    }

    private float calculateScrollOffset(float speed) {
        return (speed * getElapsedTime()) % 1.0f;
    }

    private float getTargetIntensity() {
        return data.getOrConstruct(TARGET_INTENSITY_KEY, () -> 0.0f);
    }

    private void setTargetIntensity(float intensity) {
        data.set(TARGET_INTENSITY_KEY, intensity);
    }

    private float getIntensity() {
        return data.getOrConstruct(INTENSITY_KEY, () -> 0.0f);
    }

    private void setIntensity(float intensity) {
        data.set(INTENSITY_KEY, intensity);
    }

    private void approachTargetIntensity(float amount) {
        final float start = getIntensity();
        final float end   = getTargetIntensity();
        if (start > end) {
            setIntensity(Math.max(start - amount, end));
        } else if (start < end) {
            setIntensity(Math.min(start + amount, end));
        }
    }

    private float getTargetFlareIntensity() {
        return data.getOrConstruct(TARGET_FLARE_INTENSITY_KEY, () -> 0.0f);
    }

    private void setTargetFlareIntensity(float intensity) {
        data.set(TARGET_FLARE_INTENSITY_KEY, intensity);
    }

    private float getDampedFlareIntensity() {
        return data.getOrConstruct(DAMPED_FLARE_INTENSITY_KEY, () -> 0.0f);
    }

    private void setDampedFlareIntensity(float intensity) {
        data.set(DAMPED_FLARE_INTENSITY_KEY, intensity);
    }

    private float getFlareIntensity() {
        return data.getOrConstruct(FLARE_INTENSITY_KEY, () -> 0.0f);
    }

    private void setFlareIntensity(float intensity) {
        data.set(FLARE_INTENSITY_KEY, intensity);
    }

    private void approachTargetFlareIntensity(float amount) {
        final float start = getFlareIntensity();
        final float end   = getTargetFlareIntensity();
        if (start > end) {
            setFlareIntensity(Math.max(start - amount, end));
        } else if (start < end) {
            setFlareIntensity(Math.min(start + amount, end));
        }
    }

    private void dampenFlareIntensity(float amount) {
        final float start = getDampedFlareIntensity();
        final float end   = getFlareIntensity();
        setDampedFlareIntensity(Utilities.lerp(start, end, FLARE_DAMP_AMOUNT * amount));
    }

    private float getFlashIntensity() {
        return data.getOrConstruct(FLASH_INTENSITY_KEY, () -> 0.0f);
    }

    private void setFlashIntensity(float intensity) {
        data.set(FLASH_INTENSITY_KEY, intensity);
    }

    private float getDampedFlashIntensity() {
        return data.getOrConstruct(DAMPED_FLASH_INTENSITY_KEY, () -> 0.0f);
    }

    private void setDampedFlashIntensity(float intensity) {
        data.set(DAMPED_FLASH_INTENSITY_KEY, intensity);
    }

    private void dampenFlashIntensity(float amount) {
        final float start = getDampedFlashIntensity();
        final float end   = getFlashIntensity();
        setDampedFlashIntensity(Utilities.lerp(start, end, FLASH_DAMP_AMOUNT * amount));
    }

    private void reduceFlashIntensity(float amount) {
        setFlashIntensity(Math.max(getFlashIntensity() - amount * FLASH_REDUCE_RATE, 0.0f));
    }

    private Vector2f getConsumerLocation() {
        return data.getOrConstruct(CONSUMER_LOCATION_KEY, () -> new Vector2f(0f, 0f));
    }

    private Vector2f getStarLocation() {
        return data.getOrConstruct(STAR_LOCATION_KEY, () -> new Vector2f(0f, 0f));
    }

    private float getConsumerRadius() {
        return data.getOrConstruct(CONSUMER_RADIUS_KEY, () -> 1.0f);
    }

    private void setConsumerRadius(float radius) {
        data.set(CONSUMER_RADIUS_KEY, radius);
    }

    private float getStarRadius() {
        return data.getOrConstruct(STAR_RADIUS_KEY, () -> 1.0f);
    }

    private void setStarRadius(float radius) {
        data.set(STAR_RADIUS_KEY, radius);
    }

    private Color getStarLightColor() {
        return data.getOrConstruct(STAR_LIGHT_COLOR_KEY, () -> Color.WHITE);
    }

    private void setStarLightColor(Color color) {
        data.set(STAR_LIGHT_COLOR_KEY, color);
    }

    @Override
    public StarSiphonParams getParams() {
        return data.getOrConstruct(PARAMS_KEY, () -> {
            StarSiphonParams newParams = StarSiphonParams.create();
            applyParams(newParams);
            return newParams;
        });
    }

    @Override
    public void setParams(StarSiphonParams params) {
        data.set(PARAMS_KEY, params);
        applyParams(params);
    }

    @Override
    public void activate() {
        if (isActive()) { return; }
        setState(State.IN);
    }

    @Override
    public void deactivate() {
        if (!isActive()) { return; }
        setState(State.OUT);
    }

    @Override
    public void setActive(boolean active) {
        if (!active) {
            deactivate();
        } else {
            activate();
        }
    }

    @Override
    public void setTimeScale(float timeScale) {
        data.set(TIME_SCALE_KEY, timeScale);
    }

    @Override
    public void setCollectionOffset(float x, float y) {
        data.set(COLLECTION_OFFSET_KEY, new Vector2f(x, y));
    }

    @Override
    public Vector2f getCollectionOffset() {
        return new Vector2f(data.getOrConstruct(COLLECTION_OFFSET_KEY, Vector2f::new));
    }

    @Override
    public float getCollectionOffsetX() {
        return getCollectionOffset().x;
    }

    @Override
    public float getCollectionOffsetY() {
        return getCollectionOffset().y;
    }

    @Override
    public float getTimeScale() {
        return data.getOrConstruct(TIME_SCALE_KEY, () -> 1.0f);
    }

    @Override
    public boolean isActive() {
        final State state = getState();
        return state != State.INACTIVE && state != State.OUT;
    }

    @Override
    public boolean isBusy() {
        return getState() != State.INACTIVE;
    }

    @Override
    public void setStar(PlanetAPI star) {
        data.set(STAR_KEY, star);
    }

    @Override
    public void setConsumer(SectorEntityToken consumer) {
        data.set(CONSUMER_KEY, consumer);
    }

    @Override
    public Option<PlanetAPI> getStar() {
        return data.getAs(STAR_KEY);
    }

    @Override
    public Option<SectorEntityToken> getConsumer() {
        return data.getAs(CONSUMER_KEY);
    }

    @Override
    public void advance(float amount) {
        amount *= getTimeScale();
        advanceElapsedTime(amount * EasingFunctions.easeInQuad(getIntensity()));
        reduceFlashIntensity(amount);
        dampenFlareIntensity(amount);
        dampenFlashIntensity(amount);
        advanceState(getState(), amount);
        getParticleSystem().advance(amount);
    }

    @Override
    public void renderBackground() {
        renderLayer(
                getBackgroundSprite(),
                false,
                BACKGROUND_SCROLL_SPEED,
                getIntensity(),
                1.5f,
                getIntensity(),
                getBackgroundTint()
        );
    }

    @Override
    public void renderMidground() {
        renderLayer(
                getMidgroundSprite(),
                true,
                MIDGROUND_SCROLL_SPEED,
                getIntensity() * 6.0f,
                0.75f,
                1.0f,
                getMidgroundTint()
        );
    }

    @Override
    public void renderForeground() {
        renderLayer(
                getForegroundSprite(),
                true,
                FOREGROUND_SCROLL_SPEED,
                32.0f - getIntensity() * 16.0f,
                0.25f,
                1.0f,
                getForegroundTint()
        );
    }

    @Override
    public void renderFlash() {
        final float consumerIntensity = EasingFunctions.easeOutQuad(getDampedFlashIntensity());
        final float starIntensity     = EasingFunctions.easeInQuad(getDampedFlashIntensity());

        if (consumerIntensity <= 0.0f && starIntensity <= 0.0f) {
            return;
        }
        final SavableSprite sprite = getFlashSprite();
        if (sprite.isInvalid()) {
            return;
        }

        final Vector2f starLocation = getStarLocation();
        final Vector2f consumerLocation = getConsumerLocation();
        final float starRadius = getStarRadius();
        final float consumerRadius = getConsumerRadius();
        final Color starLightColor = getStarLightColor();
        final Color tint = getFlashTint();

        final Vector2f starFlashLocation = Vector2f.sub(consumerLocation, starLocation, null);
        starFlashLocation.normalise();
        Vector2f offset = getCollectionOffset();
        offset = Misc.rotateAroundOrigin(offset, Misc.getAngleInDegrees(starFlashLocation));
        starFlashLocation.scale(starRadius);

        final Vector2f consumerFlashLocation = Vector2f.add(consumerLocation, offset, null);

        final float starFlashSize     = starIntensity * 1000f;
        final float consumerFlashSize = consumerIntensity * consumerRadius * 8f;

        sprite.setAdditiveBlend();
        sprite.setColor(Utilities.multiply(tint, starLightColor));

        sprite.setSize(starFlashSize, starFlashSize);
        sprite.setCenter(starFlashSize / 2.0f, starFlashSize / 2.0f);
        sprite.setAlphaMult(starIntensity);
        sprite.renderAtCenter(starFlashLocation.x, starFlashLocation.y);

        sprite.setSize(consumerFlashSize, consumerFlashSize);
        sprite.setCenter(consumerFlashSize / 2.0f, consumerFlashSize / 2.0f);
        sprite.setAlphaMult(consumerIntensity);
        sprite.renderAtCenter(consumerFlashLocation.x, consumerFlashLocation.y);
    }

    @Override
    public void renderFlare() {
        final float intensity = EasingFunctions.easeInQuad(getDampedFlareIntensity());
        if (intensity <= 0.0f) {
            return;
        }
        final SavableSprite sprite = getFlareSprite();
        if (sprite.isInvalid()) {
            return;
        }

        final Vector2f starLocation = getStarLocation();
        final Vector2f consumerLocation = getConsumerLocation();
        final float starRadius = getStarRadius();
        final Color starLightColor = getStarLightColor();

        final Vector2f locationDifference = Vector2f.sub(consumerLocation, starLocation, null);
        locationDifference.normalise();
        final float angle = Misc.getAngleInDegrees(locationDifference);

        final float arcLength = 30.0f + 60.0f * (1.0f - intensity);

        Utilities.renderArc(
                sprite,
                Global.getSector().getViewport(),
                starLocation.x,
                starLocation.y,
                starRadius,
                starRadius + (EasingFunctions.easeInSine(intensity) * 1000f),
                angle,
                arcLength,
                getElapsedTime(),
                Utilities.multiply(getFlareTint(), starLightColor),
                intensity,
                true,
                intensity,
                5.0f * getElapsedTime(),
                0.25f,
                0.5f
        );
    }

    @Override
    public void renderParticles() {
        getParticleSystem().render();
    }

    private void updateStateData() {
        Option<PlanetAPI>         potentialStar     = getStar();
        Option<SectorEntityToken> potentialConsumer = getConsumer();

        if (potentialStar.isNone() || potentialConsumer.isNone()) {
            return;
        }

        final PlanetAPI star             = potentialStar.unwrap();
        final SectorEntityToken consumer = potentialConsumer.unwrap();
        final Vector2f consumerLocation  = consumer.getLocation();
        final Vector2f starLocation      = star.getLocation();
        final Color starLightColor       = star.getLightColor();
        final float starRadius           = star.getRadius();
        final float consumerRadius       = consumer.getRadius();

        getStarLocation().set(starLocation);
        getConsumerLocation().set(consumerLocation);
        setStarRadius(starRadius);
        setConsumerRadius(consumerRadius);
        setStarLightColor(Option.of(starLightColor).reduce((c) -> c, Color.WHITE));
    }

    private void enterInactiveState() {
        setTargetIntensity(0.0f);
        setIntensity(0.0f);
        setTargetFlareIntensity(0.0f);
        setFlareIntensity(0.0f);
    }

    private void advanceInactiveState(float amount) { }

    private void exitInactiveState() { }

    private void enterInState() {
        final Vector2f starLocation = getStarLocation();
        final Vector2f consumerLocation = getConsumerLocation();
        final float starRadius = getStarRadius();

        final Vector2f locationDifference = Vector2f.sub(consumerLocation, starLocation, null);
        locationDifference.normalise();
        locationDifference.scale(starRadius);

        setStateProgress(0.0f);
        setTargetIntensity(0.75f);
        setTargetFlareIntensity(1.0f);
        setFlashIntensity(1.0f);

        getInSound().playSound(consumerLocation, new Vector2f());
        getInSound().playSound(locationDifference, new Vector2f());
    }

    private void advanceInState(float amount) {
        increaseStateProgress(amount * IN_STATE_PROGRESS_RATE);
        approachTargetIntensity(amount * IN_STATE_PROGRESS_RATE * 0.6666f);
        approachTargetFlareIntensity(amount * IN_STATE_PROGRESS_RATE);
        if (isStateFinished()) {
            setState(State.ACTIVE);
        }
    }

    private void exitInState() {

    }

    private void enterActiveState() {
        getParticleSystem().clearAttractors();
        getParticleSystem().clearRepulsors();
        getParticleSystem().setEmitting(true);
        setStateProgress(0.0f);
        setTargetIntensity(1.0f);
        setTargetFlareIntensity(0.66f);
    }

    private void advanceActiveState(float amount) {
        increaseStateProgress(amount * ACTIVE_STATE_PROGRESS_RATE);
        approachTargetIntensity(amount * ACTIVE_STATE_PROGRESS_RATE);
        approachTargetFlareIntensity(amount * ACTIVE_STATE_PROGRESS_RATE);
        float progress = getStateProgress();

        final Vector2f consumerLocation = getConsumerLocation();
        final Vector2f starLocation = getStarLocation();
        final float starRadius = getStarRadius();
        final Color starLightColor = getStarLightColor();

        final ParticleSystem particleSystem = getParticleSystem();

        final Vector2f emitterPosition = new Vector2f();
        Vector2f.sub(consumerLocation, starLocation, emitterPosition);

        final float distance = emitterPosition.length();
        emitterPosition.normalise();

        Vector2f offset = getCollectionOffset();
        offset = Misc.rotateAroundOrigin(offset, Misc.getAngleInDegrees(emitterPosition));
        final Vector2f finalConsumerPosition = new Vector2f(consumerLocation.x + offset.x, consumerLocation.y + offset.y);

        emitterPosition.scale(starRadius * 0.75f);
        particleSystem.setEmitterLocation(emitterPosition);

        Vector2f emitterPositionCopy = new Vector2f(emitterPosition);
        emitterPositionCopy.normalise();
        Vector2f up   = Misc.rotateAroundOrigin(emitterPositionCopy,  90.0f);
        Vector2f down = Misc.rotateAroundOrigin(emitterPositionCopy, -90.0f);

        float squeezeDistance = Utilities.lerp(starRadius, distance, 0.5f);
        emitterPositionCopy.scale(squeezeDistance);
        squeezeDistance = starRadius;

        Vector2f squeezePosition1 = new Vector2f(emitterPositionCopy);
        squeezePosition1.x += starLocation.x + up.x * squeezeDistance;
        squeezePosition1.y += starLocation.y + up.y * squeezeDistance;

        Vector2f squeezePosition2 = new Vector2f(emitterPositionCopy);
        squeezePosition2.x += starLocation.x + down.x * squeezeDistance;
        squeezePosition2.y += starLocation.y + down.y * squeezeDistance;

        final float lifetimeFactor = Math.max(1f, distance - starRadius) / MAX_PARTICLE_RANGE;

        particleSystem.setParams(particleSystem.getParams()
                .withOffsetRange(Range2D.of(-starRadius * 0.75f, starRadius * 0.75f, -starRadius * 0.75f, starRadius * 0.75f))
                .withColorRange(ColorRange.of(new Color(20, 0, 0, 255), starLightColor))
                .withLifetimeRange(Range.of(2.5f * lifetimeFactor, 3.5f * lifetimeFactor))
        );

        final float baseAttractorStrength  = 100f;
        final float elapsedTime            = getElapsedTime();
        final float sinOfTime              = (float) Math.sin(elapsedTime * 6.0f);
        final float clampedSinOfTime       = Utilities.clampN11(sinOfTime * 1.5f + 0.5f);
        final float finalAttractorStrength = (0.85f + (clampedSinOfTime * 0.15f)) * baseAttractorStrength;

        final float forceMultiplier = EasingFunctions.easeInCubic(progress);
        particleSystem.addAttractor("attractor", finalConsumerPosition,  finalAttractorStrength * forceMultiplier, MAX_PARTICLE_RANGE);
        particleSystem.addRepulsor("squeeze1",  squeezePosition1, 200f * forceMultiplier, squeezeDistance);
        particleSystem.addRepulsor("squeeze2",  squeezePosition2, 200f * forceMultiplier, squeezeDistance);

        getLoopSound().playLoop(getLoopSoundSource1(), consumerLocation, new Vector2f(), 0.1f, 1.0f);
        getLoopSound().playLoop(getLoopSoundSource2(), emitterPosition, new Vector2f(), 0.1f, 1.0f);
    }

    private void exitActiveState() {
        getParticleSystem().setEmitting(false);
    }

    private void enterOutState() {
        final Vector2f starLocation = getStarLocation();
        final Vector2f consumerLocation = getConsumerLocation();
        final float starRadius = getStarRadius();

        final Vector2f locationDifference = Vector2f.sub(consumerLocation, starLocation, null);
        locationDifference.normalise();
        locationDifference.scale(starRadius);

        setStateProgress(0.0f);
        setTargetIntensity(0.0f);
        setTargetFlareIntensity(0.0f);

        getOutSound().playSound(consumerLocation, new Vector2f());
        getOutSound().playSound(locationDifference, new Vector2f());
    }

    private void advanceOutState(float amount) {
        increaseStateProgress(amount * OUT_STATE_PROGRESS_RATE);
        approachTargetIntensity(amount * OUT_STATE_PROGRESS_RATE);
        approachTargetFlareIntensity(amount * OUT_STATE_PROGRESS_RATE * 0.5f);
        if (isStateFinished()) {
            setState(State.INACTIVE);
        }
    }

    private void exitOutState() {

    }

    private void enterState(State state) {
        updateStateData();
        switch (state) {
            case INACTIVE -> enterInactiveState();
            case IN -> enterInState();
            case ACTIVE -> enterActiveState();
            case OUT -> enterOutState();
        }
    }

    private void exitState(State state) {
        updateStateData();
        switch (state) {
            case INACTIVE -> exitInactiveState();
            case IN -> exitInState();
            case ACTIVE -> exitActiveState();
            case OUT -> exitOutState();
        }
    }

    private void advanceState(State state, float amount) {
        updateStateData();
        switch (state) {
            case INACTIVE -> advanceInactiveState(amount);
            case IN -> advanceInState(amount);
            case ACTIVE -> advanceActiveState(amount);
            case OUT -> advanceOutState(amount);
        }
    }

    private void renderLayer(
            SavableSprite texture,
            boolean additive,
            float scrollSpeed,
            float turbulence,
            float widthMult,
            float alphaMult,
            Color tint
    ) {
        if (texture.isInvalid()) { return; }

        final ViewportAPI viewport = Global.getSector().getViewport();
        final Vector2f starLocation = getStarLocation();
        final Vector2f consumerLocation = getConsumerLocation();
        final float starRadius = getStarRadius();
        final float consumerRadius = getConsumerRadius();
        final Color starLightColor = getStarLightColor();
        final float scrollOffset = calculateScrollOffset(scrollSpeed);

        float intensity = EasingFunctions.easeInCubic(getIntensity());

        if (intensity <= 0.0f) {
            return;
        }

        final Vector2f locationDifference = Vector2f.sub(consumerLocation, starLocation, null);
        locationDifference.normalise();
        Vector2f offset = getCollectionOffset();
        offset = Misc.rotateAroundOrigin(offset, Misc.getAngleInDegrees(locationDifference));

        final Vector2f finalConsumerPosition = new Vector2f(consumerLocation.x + offset.x, consumerLocation.y + offset.y);
        final Vector2f finalLocationDifference = Vector2f.sub(finalConsumerPosition, starLocation, null);
        final float finalDistance = finalLocationDifference.length() - (starRadius - consumerRadius);

        locationDifference.scale(starRadius);
        final Vector2f startPosition = Vector2f.add(starLocation, locationDifference, null);

        finalLocationDifference.normalise();
        finalLocationDifference.scale(starRadius + consumerRadius + (finalDistance * Math.min(1.0f, 0.5f + 25.0f * EasingFunctions.easeOutSine(intensity))));
        final Vector2f endPosition = Vector2f.add(starLocation, finalLocationDifference, null);

        Utilities.renderPinchedShape(
                texture,
                viewport,
                startPosition.x,
                startPosition.y,
                starRadius * 0.75f * widthMult * intensity,
                endPosition.x,
                endPosition.y,
                consumerRadius * widthMult * intensity,
                2.0f + (1.0f - EasingFunctions.easeOutCubic(intensity)),
                0.1f + 0.6f * intensity, scrollOffset,
                Utilities.multiply(starLightColor, tint),
                intensity * alphaMult,
                additive,
                turbulence,
                scrollSpeed * 0.75f * getElapsedTime(),
                0.1f
        );
    }
}
