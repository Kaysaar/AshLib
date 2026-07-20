package ashlib.shmo.aotd.lib.impl.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import ashlib.shmo.aotd.lib.api.ShmoGlobal;
import ashlib.shmo.aotd.lib.api.campaign.CampaignData;
import ashlib.shmo.aotd.lib.api.campaign.StarSiphonEffect;
import ashlib.shmo.aotd.lib.api.campaign.StarSiphonParams;
import ashlib.shmo.aotd.lib.api.campaign.StarSiphonManager;
import ashlib.shmo.aotd.lib.api.general.Option;
import ashlib.shmo.aotd.lib.api.general.SoundParams;
import ashlib.shmo.aotd.lib.api.ids.ShmoGraphics;
import ashlib.shmo.aotd.lib.api.ids.ShmoSounds;

import java.awt.*;
import java.util.*;
import java.util.List;

class StarSiphonManagerImpl extends BaseCustomEntityPlugin implements StarSiphonManager {
    private static final String SIPHON_ENTITIES_KEY    = "siphonEntities";
    private static final String ENTITY_TYPE_PARAMS_KEY = "entityTypeParams";

    CampaignData data = new MapCampaignData();

    private Map<SectorEntityToken, StarSiphonEffect> getSiphonEntities() {
        return data.getOrConstruct(SIPHON_ENTITIES_KEY, HashMap::new);
    }

    private Map<String, StarSiphonParams> getEntityTypeParams() {
        return data.getOrConstruct(ENTITY_TYPE_PARAMS_KEY, HashMap::new);
    }

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        Option.of(Global.getSector().getPlayerFleet()).match(
                (playerFleet) -> {
                    if (entity.getContainingLocation() != playerFleet.getContainingLocation() && playerFleet.getContainingLocation() != null) {
                        entity.getContainingLocation().removeEntity(entity);
                        playerFleet.getContainingLocation().addEntity(entity);
                    }
                    entity.setLocation(playerFleet.getLocation().x, playerFleet.getLocation().y);
                },
                () -> {}
        );

        final Map<SectorEntityToken, StarSiphonEffect> siphonEntities = getSiphonEntities();
        final List<SectorEntityToken> toRemove = new ArrayList<>();

        for (var entry : siphonEntities.entrySet()) {
            SectorEntityToken entity = entry.getKey();
            StarSiphonEffect siphon = entry.getValue();
            if (entity.isExpired() && !siphon.isBusy()) {
                siphon.setConsumer(null);
                toRemove.add(entity);
                continue;
            } else if (entity.isExpired() && siphon.isBusy()) {
                siphon.setConsumer(null);
                siphon.deactivate();
            }
            siphon.advance(amount);
        }

        for (SectorEntityToken entity : toRemove) {
            siphonEntities.remove(entity);
        }
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        super.render(layer, viewport);

        final Map<SectorEntityToken, StarSiphonEffect> siphonEntities = getSiphonEntities();
        for (var entry : siphonEntities.entrySet()) {
            StarSiphonEffect siphon = entry.getValue();
            Option<PlanetAPI> starOption = siphon.getStar();
            Option<SectorEntityToken> entityOption = siphon.getConsumer();

            if (starOption.isSome()) {
                PlanetAPI star = starOption.unwrap();
                if (!star.isInCurrentLocation()) {
                    continue;
                }
            } else {
                continue;
            }

            if (entityOption.isSome()) {
                SectorEntityToken entity = entityOption.unwrap();
                if (!entity.isInCurrentLocation()) {
                    continue;
                } else if (!entity.isVisibleToPlayerFleet()) {
                    continue;
                }
            }

            if (layer == CampaignEngineLayers.TERRAIN_1) {
                siphon.renderFlare();
                siphon.renderBackground();
                siphon.renderMidground();
            } else {
                siphon.renderForeground();
                siphon.renderParticles();
                siphon.renderFlash();
            }
        }
    }

    @Override
    public float getRenderRange() {
        return 100_000f;
    }

    @Override
    public StarSiphonEffect getSiphonForEntity(SectorEntityToken entity) {
        return Option.of(getSiphonEntities().get(entity)).match(
                (siphon) -> siphon,
                () -> {
                    StarSiphonEffect siphon = ShmoGlobal.getCampaignFactory()
                            .createStarSiphonEffect(getParamsForCustomEntityType(entity.getCustomEntityType()));
                    siphon.setConsumer(entity);
                    getSiphonEntities().put(entity, siphon);
                    return siphon;
                }
        );
    }

    @Override
    public void removeSiphonForEntity(SectorEntityToken entity) {
        getSiphonEntities().remove(entity);
    }

    @Override
    public StarSiphonParams getParamsForCustomEntityType(String type) {
        if (type == null || type.isEmpty()) {
            return getDefaultParams();
        }
        return getEntityTypeParams().getOrDefault(type, getDefaultParams());
    }

    @Override
    public void setParamsForCustomEntityType(String type, StarSiphonParams params) {
        if (type == null || type.isEmpty()) {
            return;
        }
        getEntityTypeParams().put(type, params);
    }

    @Override
    public StarSiphonParams getDefaultParams() {
        final SettingsAPI settings = Global.getSettings();
        final Color backgroundTint = Color.BLUE;
        final Color midgroundTint  = new Color(240, 240, 150, 200);
        final Color foregroundTint = new Color(255, 250, 250, 200);
        final Color flareTint      = new Color(200, 210, 200, 190);
        final Color flashTint      = Color.WHITE;
        final Color particleTint   = Color.WHITE;

        return StarSiphonParams.create()
                .withSprites(
                        settings.getSpriteName(ShmoGraphics.CAMPAIGN, ShmoGraphics.Campaign.FX_SIPHON_BACKGROUND),
                        settings.getSpriteName(ShmoGraphics.CAMPAIGN, ShmoGraphics.Campaign.FX_SIPHON_MIDGROUND),
                        settings.getSpriteName(ShmoGraphics.CAMPAIGN, ShmoGraphics.Campaign.FX_SIPHON_FOREGROUND),
                        settings.getSpriteName(ShmoGraphics.CAMPAIGN, ShmoGraphics.Campaign.FX_FLARE),
                        settings.getSpriteName(ShmoGraphics.CAMPAIGN, ShmoGraphics.Campaign.FX_FLASH)
                )
                .withParticleSprites(
                        List.of(
                                settings.getSpriteName(ShmoGraphics.CAMPAIGN, ShmoGraphics.Campaign.PARTICLE_CONFETTI_CIRCLE),
                                settings.getSpriteName(ShmoGraphics.CAMPAIGN, ShmoGraphics.Campaign.PARTICLE_CONFETTI_HEXAGON),
                                settings.getSpriteName(ShmoGraphics.CAMPAIGN, ShmoGraphics.Campaign.PARTICLE_CONFETTI_RECTANGLE),
                                settings.getSpriteName(ShmoGraphics.CAMPAIGN, ShmoGraphics.Campaign.PARTICLE_CONFETTI_TRIANGLE),
                                settings.getSpriteName(ShmoGraphics.CAMPAIGN, ShmoGraphics.Campaign.PARTICLE_CONFETTI_SQUARE),
                                settings.getSpriteName(ShmoGraphics.CAMPAIGN, ShmoGraphics.Campaign.PARTICLE_CONFETTI_HALFCIRCLE)
                        )
                )
                .withBackgroundTint(backgroundTint)
                .withMidgroundTint(midgroundTint)
                .withForegroundTint(foregroundTint)
                .withFlareTint(flareTint)
                .withFlashTint(flashTint)
                .withParticleTint(particleTint)
                .withSounds(
                        SoundParams.of(ShmoSounds.Campaign.STAR_SIPHON_IN),
                        SoundParams.of(ShmoSounds.Campaign.STAR_SIPHON_OUT),
                        SoundParams.of(ShmoSounds.Campaign.STAR_SIPHON_LOOP)
                );
    }
}
