package ashlib.data.plugins;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import ashlib.data.plugins.repositories.FighterInfoRepo;
import ashlib.data.plugins.repositories.WeaponMissileInfoRepo;

public class AshWeaponRepoPlugin extends BaseEveryFrameCombatPlugin {
    @Override
    public void init(CombatEngineAPI engine) {

        if(Global.getCurrentState()== GameState.TITLE) {
            if (WeaponMissileInfoRepo.weapontoMissleMap.isEmpty()) {
                WeaponMissileInfoRepo.initMap();
            }
        if(FighterInfoRepo.fighterRepo.isEmpty()){
            FighterInfoRepo.initializeRepo();
        }
        }


    }
}
