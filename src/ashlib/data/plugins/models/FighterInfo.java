package ashlib.data.plugins.models;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class FighterInfo {
    LinkedHashMap<String, Integer> weaponMap;
    String fighterWingID;

    public FighterInfo(String id, LinkedHashMap<String, Integer> weaponMap) {
        this.weaponMap = weaponMap;
        this.fighterWingID = id;
    }

    public LinkedHashMap<String, Integer> getWeaponMap() {
        return weaponMap;
    }

    public String getFighterWingID() {
        return fighterWingID;
    }
}