package ashlib.shmo.api.campaign;

import com.fs.starfarer.api.campaign.SectorEntityToken;

public interface StarSiphonManager {
    StarSiphonEffect getSiphonForEntity(SectorEntityToken entity);
    void removeSiphonForEntity(SectorEntityToken entity);
    StarSiphonParams getParamsForCustomEntityType(String type);
    void setParamsForCustomEntityType(String type, StarSiphonParams params);
    StarSiphonParams getDefaultParams();
    void setDefaultParams(StarSiphonParams params);
    void resetDefaultParams();
}
