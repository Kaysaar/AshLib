package ashlib.shmo.aotd.lib.impl.general;

import ashlib.shmo.aotd.lib.api.general.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;

import org.lwjgl.util.vector.Vector2f;


import java.awt.*;
import java.util.*;
import java.util.List;

class CPUParticleSystem implements ParticleSystem {
    private static class Particle {
        public float                 x, y;
        public float                 angle;

        public float                 size;
        public float                 velocityX, velocityY;
        public float                 jitter;
        public Color                 color;
        public float                 spinSpeed;

        public float                 startSize;
        public float                 startVelocityX, startVelocityY;
        public float                 startJitter;
        public Color                 startColor;
        public float                 startSpinSpeed;

        public float                 startLifetime;
        public float                 lifetime;
        public Option<SavableSprite> sprite;

        public Particle() {
            x              = 0.0f;
            y              = 0.0f;
            angle          = 0.0f;

            size           = 1.0f;
            velocityX      = 0.0f;
            velocityY      = 0.0f;
            jitter         = 0.0f;
            color          = Color.WHITE;
            spinSpeed      = 0.0f;

            startSize      = 1.0f;
            startVelocityX = 0.0f;
            startVelocityY = 0.0f;
            startJitter    = 0.0f;
            startColor     = Color.WHITE;
            startSpinSpeed = 0.0f;

            lifetime       = 0.0f;
            startLifetime  = 0.0f;
            sprite         = Option.none();
        }

        private void reset(ParticleParams params, float emitterX, float emitterY, Random random) {
            x              = emitterX + params.getOffsetRange().sampleX(random.nextFloat());
            y              = emitterY + params.getOffsetRange().sampleY(random.nextFloat());
            angle          = params.getAngleRange().sample(random.nextFloat());

            startSize      = params.getSizeRange().sample(random.nextFloat());
            startVelocityX = params.getVelocityRange().sampleX(random.nextFloat());
            startVelocityY = params.getVelocityRange().sampleY(random.nextFloat());
            startJitter    = params.getJitterRange().sample(random.nextFloat());
            startColor     = params.getColorRange().sample(random.nextFloat());
            startSpinSpeed = params.getSpinSpeedRange().sample(random.nextFloat());

            startLifetime  = params.getLifetimeRange().sample(random.nextFloat());

            if (params.hasMinimumSpeed()) {
                final Vector2f velocityVector = new Vector2f(startVelocityX, startVelocityY);
                final float minSpeed = params.getMinimumSpeed();
                final float oldSpeed = velocityVector.length();
                if (oldSpeed < minSpeed) {
                    if (oldSpeed > 0.0f) {
                        velocityVector.normalise();
                        velocityVector.scale(minSpeed);
                    } else {
                        velocityVector.set(1.0f, 0.0f);
                        velocityVector.scale(minSpeed);
                    }
                    startVelocityX = velocityVector.x;
                    startVelocityY = velocityVector.y;
                }
            }

            if (params.hasMaximumSpeed()) {
                final Vector2f velocityVector = new Vector2f(startVelocityX, startVelocityY);
                final float maxSpeed = params.getMaximumSpeed();
                final float oldSpeed = velocityVector.length();
                if (oldSpeed > maxSpeed) {
                    if (oldSpeed > 0.0f) {
                        velocityVector.normalise();
                        velocityVector.scale(maxSpeed);
                    } else {
                        velocityVector.set(1.0f, 0.0f);
                        velocityVector.scale(maxSpeed);
                    }
                    startVelocityX = velocityVector.x;
                    startVelocityY = velocityVector.y;
                }
            }

            final List<String> spriteNames = params.getSpriteNames();
            if (spriteNames.isEmpty()) {
                sprite = Option.none();
            } else {
                final int spriteIndex = random.nextInt(spriteNames.size());
                sprite = Option.of(SavableSprite.create(spriteNames.get(spriteIndex)));
            }

            size      = startSize;
            velocityX = startVelocityX;
            velocityY = startVelocityY;
            jitter    = startJitter;
            color     = startColor;
            spinSpeed = startSpinSpeed;
            lifetime  = startLifetime;
        }

        private boolean isExpired() { return lifetime <= 0.0f; }

        private void advance(ParticleParams params, float amount, List<Attractor> attractors, List<Repulsor> repulsors, Random random) {
            if (isExpired()) { return; }

            lifetime -= amount;
            if (isExpired()) { return; }

            final float t = 1.0f - Utilities.clamp01(startLifetime > 0.0f ? lifetime / startLifetime : 0.0f);

            size      = startSize * params.getSizeRampRange().sample(params.getSizeRampCurve().sample(t));
            velocityX = startVelocityX * params.getVelocityRampRange().sampleX(params.getVelocityRampCurve().sample(t));
            velocityY = startVelocityY * params.getVelocityRampRange().sampleY(params.getVelocityRampCurve().sample(t));
            jitter    = startJitter * params.getJitterRampRange().sample(params.getJitterRampCurve().sample(t));
            color     = Utilities.multiply(startColor, params.getColorRampRange().sample(params.getColorRampCurve().sample(t)));
            spinSpeed = startSpinSpeed * params.getSpinSpeedRampRange().sample(params.getSpinSpeedRampCurve().sample(t));

            for (Attractor attractor : attractors) {
                applyField(attractor.x, attractor.y, attractor.strength, attractor.range, amount, true);
            }
            for (Repulsor repulsor : repulsors) {
                applyField(repulsor.x, repulsor.y, repulsor.strength, repulsor.range, amount, false);
            }

            x += velocityX * amount;
            y += velocityY * amount;

            if (jitter != 0.0f) {
                final float jitterAngleRad = random.nextFloat() * (float) (Math.PI * 2.0);
                final float jitterMagnitude = jitter * amount;
                x += (float) Math.cos(jitterAngleRad) * jitterMagnitude;
                y += (float) Math.sin(jitterAngleRad) * jitterMagnitude;
            }

            angle += spinSpeed * amount;

            if (params.hasMinimumSpeed()) {
                final Vector2f velocityVector = new Vector2f(velocityX, velocityY);
                final float minSpeed = params.getMinimumSpeed();
                final float oldSpeed = velocityVector.length();
                if (oldSpeed < minSpeed) {
                    if (oldSpeed > 0.0f) {
                        velocityVector.normalise();
                        velocityVector.scale(minSpeed);
                    } else {
                        velocityVector.set(1.0f, 0.0f);
                        velocityVector.scale(minSpeed);
                    }
                    velocityX = velocityVector.x;
                    velocityY = velocityVector.y;
                }
            }

            if (params.hasMaximumSpeed()) {
                final Vector2f velocityVector = new Vector2f(velocityX, velocityY);
                final float maxSpeed = params.getMaximumSpeed();
                final float oldSpeed = velocityVector.length();
                if (oldSpeed > maxSpeed) {
                    if (oldSpeed > 0.0f) {
                        velocityVector.normalise();
                        velocityVector.scale(maxSpeed);
                    } else {
                        velocityVector.set(1.0f, 0.0f);
                        velocityVector.scale(maxSpeed);
                    }
                    velocityX = velocityVector.x;
                    velocityY = velocityVector.y;
                }
            }
        }

        private static final float STRENGTH_MULTIPLIER = 500;

        private void applyField(float fieldX, float fieldY, float strength, float range, float amount, boolean attract) {
            strength *= STRENGTH_MULTIPLIER;
            final float dx = fieldX - x;
            final float dy = fieldY - y;
            final float distSq = dx * dx + dy * dy;
            if (distSq < 1e-6f || distSq > range * range) { return; }

            final float dist = (float) Math.sqrt(distSq);
            final float falloff = 1.0f - ((dist / range) * (dist / range));
            final float force = strength * falloff * amount * (attract ? 1.0f : -1.0f);

            velocityX += (dx / dist) * force;
            velocityY += (dy / dist) * force;
        }

        private void render(boolean additiveBlending) {
            if (isExpired()) { return; }

            sprite.match(
                    (s) -> renderWithSpriteOverride(s, additiveBlending),
                    () -> {}
            );
        }

        private void renderWithSpriteOverride(SpriteAPI sprite, boolean additiveBlending) {
            if (isExpired()) { return; }

            sprite.setSize(size, size);
            sprite.setCenter(size * 0.5f, size * 0.5f);
            sprite.setAngle(angle);
            sprite.setColor(color);
            if (additiveBlending) {
                sprite.setAdditiveBlend();
            } else {
                sprite.setNormalBlend();
            }
            sprite.renderAtCenter(x, y);
        }
    }

    private static class Attractor {
        float x; float y; float strength; float range;

        public Attractor( float x, float y, float strength, float range) {
            this.x = x;
            this.y = y;
            this.strength = strength;
            this.range = range;
        }
    }

    private static class Repulsor {
        float x; float y; float strength; float range;

        public Repulsor( float x, float y, float strength, float range) {
            this.x = x;
            this.y = y;
            this.strength = strength;
            this.range = range;
        }
    }

    ParticleParams         params;
    Particle[]             particles;
    Map<String, Attractor> attractors        = new HashMap<>();
    Map<String, Repulsor>  repulsors         = new HashMap<>();
    float                  emitterX          = 0.0f;
    float                  emitterY          = 0.0f;
    Random                 random            = new Random();
    boolean                emitting          = false;
    float                  timeSinceLastEmit = 0.0f;
    int                    emitCursor        = 0;

    public CPUParticleSystem(ParticleParams params) {
        this.params = params;
        particles   = new Particle[params.getParticleCount()];
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle();
        }
    }

    public static CPUParticleSystem create(ParticleParams params) {
        return new CPUParticleSystem(params);
    }

    @Override
    public void setParams(ParticleParams params) {
        this.params = params;

        final int oldCount  = particles.length;
        final int newCount  = params.getParticleCount();
        final int safeCount = Math.min(oldCount, newCount);

        final Particle[] newParticles = new Particle[newCount];
        int i = 0;
        for (; i < safeCount; i++) {
            newParticles[i] = particles[i];
        }
        for(; i < newCount; i++) {
            newParticles[i] = new Particle();
        }

        particles = newParticles;
        timeSinceLastEmit = getAverageLifetime();
        if (emitCursor >= particles.length) {
            emitCursor = 0;
        }
    }

    @Override
    public ParticleParams getParams() {
        return params;
    }

    @Override
    public void setEmitterLocation(float x, float y) {
        emitterX = x;
        emitterY = y;
    }

    @Override
    public void setEmitterLocation(Vector2f location) {
        setEmitterLocation(location.x, location.y);
    }

    @Override
    public float getEmitterX() {
        return emitterX;
    }

    @Override
    public float getEmitterY() {
        return emitterY;
    }

    @Override
    public Vector2f getEmitterLocation() {
        return new Vector2f(emitterX, emitterY);
    }

    @Override
    public void setEmitting(boolean emitting) {
        if (emitting && !this.emitting) {
            timeSinceLastEmit = getTimeToEmit();
        }
        this.emitting = emitting;
    }

    @Override
    public boolean isEmitting() {
        return emitting;
    }

    @Override
    public void addAttractor(String id, float x, float y, float strength, float range) {
        attractors.put(id, new Attractor(x, y, strength, range));
    }

    @Override
    public void addAttractor(String id, Vector2f location, float strength, float range) {
        addAttractor(id, location.x, location.y, strength, range);
    }

    @Override
    public void removeAttractor(String id) {
        attractors.remove(id);
    }

    @Override
    public void clearAttractors() {
        attractors.clear();
    }

    @Override
    public void addRepulsor(String id, float x, float y, float strength, float range) {
        repulsors.put(id, new Repulsor(x, y, strength, range));
    }

    @Override
    public void addRepulsor(String id, Vector2f location, float strength, float range) {
        addRepulsor(id, location.x, location.y, strength, range);
    }

    @Override
    public void removeRepulsor(String id) {
        repulsors.remove(id);
    }

    @Override
    public void clearRepulsors() {
        repulsors.clear();
    }

    @Override
    public void emitBurst(float countRatio) {
        countRatio = Utilities.clamp01(countRatio);
        final int totalToEmit = (int)Math.ceil(particles.length * countRatio);

        int emitted = 0;
        for (Particle particle : particles) {
            if (particle.isExpired()) {
                particle.reset(params, emitterX, emitterY, random);
                emitted += 1;
                if (emitted >= totalToEmit) {
                    break;
                }
            }
        }
    }

    @Override
    public void emitOne() {
        for (Particle particle : particles) {
            if (particle.isExpired()) {
                particle.reset(params, emitterX, emitterY, random);
                break;
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (particles.length == 0) { return; }

        if (emitting) {
            timeSinceLastEmit += amount;
        } else {
            timeSinceLastEmit = 0.0f;
        }

        final List<Attractor> attractorList = attractors.values().stream().toList();
        final List<Repulsor> repulsorList   = repulsors.values().stream().toList();
        final float timeToEmit              = getTimeToEmit();

        if (emitting && timeToEmit > 0.0f) {
            final int maxEmitsThisFrame = Math.max(1, (int) Math.ceil(amount / timeToEmit) + 1);

            int emittedThisFrame = 0;
            while (timeSinceLastEmit >= timeToEmit && emittedThisFrame < maxEmitsThisFrame) {
                timeSinceLastEmit -= timeToEmit;
                emittedThisFrame++;

                Particle toSpawn = null;
                for (int tries = 0; tries < particles.length; tries++) {
                    final Particle candidate = particles[emitCursor];
                    emitCursor = (emitCursor + 1) % particles.length;
                    if (candidate.isExpired()) {
                        toSpawn = candidate;
                        break;
                    }
                }
                if (toSpawn != null) {
                    toSpawn.reset(params, emitterX, emitterY, random);
                }
            }

            if (timeSinceLastEmit >= timeToEmit) {
                timeSinceLastEmit = 0.0f;
            }
        }

        for (Particle particle : particles) {
            particle.advance(params, amount, attractorList, repulsorList, random);
        }
    }

    @Override
    public void render() {
        final boolean additiveBlending = params.isAdditiveBlending();
        for (Particle particle : particles) {
            particle.render(additiveBlending);
        }
    }

    @Override
    public void renderWithSpriteOverride(String spriteName) {
        final SpriteAPI sprite = Global.getSettings().getSprite(spriteName);
        if (sprite == null) { return; }

        final boolean additiveBlending = params.isAdditiveBlending();
        for (Particle particle : particles) {
            particle.renderWithSpriteOverride(sprite, additiveBlending);
        }
    }

    private float getAverageLifetime() {
        return (params.getLifetimeRange().getStart() + params.getLifetimeRange().getEnd()) * 0.5f;
    }

    private float getTimeToEmit() {
        if (particles.length == 0) {
            return 1.0f;
        }
        return Math.max(params.getLifetimeRange().getStart(), params.getLifetimeRange().getEnd()) / particles.length;
    }
}
