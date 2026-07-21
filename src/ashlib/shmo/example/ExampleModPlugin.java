package ashlib.shmo.example;

import ashlib.shmo.api.general.Option;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;


public class ExampleModPlugin extends BaseModPlugin {
    private static final String EXAMPLE_STAR_SIPHON_INSTANCE_ID = "shmolib_example_star_siphon_instance";

    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);
        if (newGame) { return; }

        Option.of(Global.getSector().getEntityById(EXAMPLE_STAR_SIPHON_INSTANCE_ID)).match(
                (e) -> {},
                this::initializeExampleStarSiphonInstance
        );
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        super.onNewGameAfterTimePass();
        initializeExampleStarSiphonInstance();
    }

    private void initializeExampleStarSiphonInstance() {
        ExampleCustomEntities.createExampleStarSiphon(
                EXAMPLE_STAR_SIPHON_INSTANCE_ID,
                Global.getSector().getHyperspace()
        );
    }
}
