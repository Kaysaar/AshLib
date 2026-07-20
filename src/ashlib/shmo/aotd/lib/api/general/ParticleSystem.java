package ashlib.shmo.aotd.lib.api.general;

import org.lwjgl.util.vector.Vector2f;

public interface ParticleSystem {
    void setParams(ParticleParams params);
    ParticleParams getParams();

    void setEmitterLocation(float x, float y);
    void setEmitterLocation(Vector2f location);
    float getEmitterX();
    float getEmitterY();
    Vector2f getEmitterLocation();

    void setEmitting(boolean emitting);
    boolean isEmitting();
    void emitBurst(float countRatio);
    void emitOne();

    void addAttractor(String id, float x, float y, float strength, float range);
    void addAttractor(String id, Vector2f location, float strength, float range);
    void removeAttractor(String id);
    void clearAttractors();

    void addRepulsor(String id, float x, float y, float strength, float range);
    void addRepulsor(String id,Vector2f location, float strength, float range);
    void removeRepulsor(String id);
    void clearRepulsors();

    void advance(float amount);
    void render();
    void renderWithSpriteOverride(String spriteName);
}
