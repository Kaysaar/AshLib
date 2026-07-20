package ashlib.shmo.aotd.lib.impl.campaign;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.campaign.BaseCampaignEntity;
import com.fs.starfarer.combat.CombatViewport;
import ashlib.shmo.aotd.lib.api.campaign.CampaignData;

import java.util.EnumSet;
import java.util.Objects;

public class ShmoEntityToken extends BaseCampaignEntity implements SectorEntityToken {
    private static final String PLUGIN_KEY = "plugin";
    private static final String LAYERS_KEY = "layers";

    private final CampaignData data = new MapCampaignData();

    public ShmoEntityToken(String id) {
        super(id);
    }

    @Override
    public float getRadius() {
        return 100;
    }

    @Override
    public boolean isStar() {
        return false;
    }

    @Override
    public boolean hasTag(String s) {
        return super.hasTag(s);
    }

    public void setCustomPlugin(CustomCampaignEntityPlugin plugin, Object params) {
        if (Objects.equals(getCustomPlugin(), plugin)) {
            return;
        }
        data.set(PLUGIN_KEY, plugin);
        plugin.init(this, params);
    }

    @Override
    public CustomCampaignEntityPlugin getCustomPlugin() {
        return data.<CustomCampaignEntityPlugin>getAs(PLUGIN_KEY).match((p) -> p, () -> null);
    }

    @Override
    public void advance(float v) {
        super.advance(v);
        CustomCampaignEntityPlugin plugin = getCustomPlugin();
        if (plugin != null) {
            plugin.advance(v);
        }
    }

    @Override
    public void render(CampaignEngineLayers layer, CombatViewport viewport) {
        super.render(layer, viewport);
        CustomCampaignEntityPlugin plugin = getCustomPlugin();
        if (plugin != null) {
            if (viewport.isNearViewport(this.getLocation(), plugin.getRenderRange())) {
                plugin.render(layer, viewport);
            }
        }
    }

    @Override
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return data.getOrConstruct(LAYERS_KEY, () -> EnumSet.of(CampaignEngineLayers.FLEETS));
    }

    public void setActiveLayers(EnumSet<CampaignEngineLayers> layers) {
        data.set(LAYERS_KEY, layers);
    }
}
