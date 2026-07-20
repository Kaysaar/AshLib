package ashlib.shmo.aotd.lib.example.campaign;

import ashlib.shmo.aotd.lib.api.ShmoGlobal;
import ashlib.shmo.aotd.lib.api.campaign.StarSiphonEffect;
import ashlib.shmo.aotd.lib.api.campaign.StarSiphonManager;
import ashlib.shmo.aotd.lib.api.general.Option;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class ExampleStarSiphonPlugin extends BaseCustomEntityPlugin {
    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        final StarSiphonManager plugin = ShmoGlobal.getStarSiphonManager();

        Option.of(Global.getSector().getPlayerFleet()).match(
                (playerFleet) -> {
                    final StarSiphonEffect starSiphonEffect = plugin.getSiphonForEntity(playerFleet);

                    final Vector2f playerLocation = playerFleet.getLocation();
                    final float playerRadius      = playerFleet.getRadius();

                    entity.getContainingLocation().removeEntity(entity);
                    playerFleet.getContainingLocation().addEntity(entity);
                    entity.setLocation(playerLocation.x, playerLocation.y);

                    if (entity.isInHyperspace()) {
                        starSiphonEffect.deactivate();
                        return;
                    }

                    if (entity.getContainingLocation() instanceof StarSystemAPI starSystem) {
                        final PlanetAPI star        = starSystem.getStar();
                        final Vector2f starLocation = star.getLocation();
                        final float starRadius      = star.getRadius();
                        if (Misc.getDistance(playerLocation, starLocation) - (playerRadius + starRadius) < 2000f) {
                            starSiphonEffect.setConsumer(playerFleet);
                            starSiphonEffect.setStar(star);
                            starSiphonEffect.activate();
                        } else {
                            starSiphonEffect.deactivate();
                        }
                    } else {
                        starSiphonEffect.deactivate();
                    }
                },
                () -> {}
        );
    }
}
