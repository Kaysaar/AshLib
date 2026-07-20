package ashlib.shmo.aotd.lib.api.campaign;

import ashlib.shmo.aotd.lib.api.general.Option;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public interface StarSiphonEffect {
    StarSiphonParams getParams();
    void setParams(StarSiphonParams params);
    void activate();
    void deactivate();
    void setActive(boolean active);
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
