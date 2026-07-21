package ashlib.shmo.api.campaign;

import ashlib.shmo.api.general.Option;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lwjgl.util.vector.Vector2f;

public interface StarSiphonEffect {
    StarSiphonParams getParams();
    void setParams(StarSiphonParams params);
    void activate();
    void deactivate();
    void setActive(boolean active);
    void setTimeScale(float timeScale);
    void setCollectionOffset(float x, float y);
    Vector2f getCollectionOffset();
    float getCollectionOffsetX();
    float getCollectionOffsetY();
    float getTimeScale();
    boolean isActive();
    boolean isBusy();
    void setStar(PlanetAPI star);
    void setConsumer(SectorEntityToken consumer);
    Option<PlanetAPI> getStar();
    Option<SectorEntityToken> getConsumer();
    void advance(float amount);
    void renderFlare();
    void renderBackground();
    void renderMidground();
    void renderForeground();
    void renderFlash();
    void renderParticles();
}
