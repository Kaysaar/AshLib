package ashlib.data.plugins.info;

import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class AshInfoTooltipGenerator {

    //Note ! Spec can be either Hullspec, WeaponSpec, or FighterWingSpec!
    public static void generateTooltip(TooltipMakerAPI tooltip,Object spec){
        float width = 400f;
        if(spec instanceof ShipHullSpecAPI){
            width= 990f;
            final CustomPanelAPI panelAPIs = ShipInfoGenerator.getShipImage((ShipHullSpecAPI) spec, 250, null).one;
            ShipInfoGenerator.generate(tooltip, AshMisc.getFleetMemberFromSpec((ShipHullSpecAPI) spec),null,panelAPIs,width);
            return;

        }
        if(spec instanceof WeaponSpecAPI){
            WeaponInfoGenerator.generate(tooltip, (WeaponSpecAPI) spec,width);
            return;

        }
        if(spec instanceof FighterWingSpecAPI){
            FighterInfoGenerator.generate(tooltip, (FighterWingSpecAPI) spec,width);
            return;

        }

    }

}
