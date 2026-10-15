package ashlib.data.plugins.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AshlibExampleEvent extends AshlibBaseEvent {

    public AshlibExampleEvent() {
        super("ashlib_random_trade_event");
    }

    @Override
    public void configureEvent() {
        maxAmount = 1;
        minimumDaysBetweenFirings = 0f;
    }

    @Override
    public boolean canEvenFireEvent() {
        return !getValidCommodities().isEmpty();
    }

    @Override
    public void configureEventBeforeFiringUp() {
        List<CommoditySpecAPI> commodities = getValidCommodities();

        if (commodities.isEmpty()) {
            possibleTargetOfEvent = null;
            return;
        }

        possibleTargetOfEvent = commodities.get(
                new Random().nextInt(commodities.size())
        );
    }

    @Override
    public boolean canFireAfterConfiguration() {
        return possibleTargetOfEvent instanceof CommoditySpecAPI;
    }

    @Override
    public void createTooltipForEvent(TooltipMakerAPI tooltip) {
        CommoditySpecAPI commodity = getTargetCommodity();

        if (commodity == null) {
            return;
        }

        tooltip.addPara(
                "Reports from across the Sector indicate an unexpected surge in demand for %s. "
                        + "What began as a series of isolated shortages has rapidly developed into a much broader shift in regional trade.",
                5f,
                Misc.getHighlightColor(),
                commodity.getName()
        );

        tooltip.addPara(
                "Merchants are redirecting cargo routes, industrial concerns are competing for increasingly limited shipments, "
                        + "and local authorities have begun securing additional reserves wherever they can. "
                        + "Even markets normally well supplied with the commodity are seeing buyers willing to pay considerably more to guarantee future deliveries.",
                10f
        );

        tooltip.addPara(
                "Analysts expect the disturbance to affect trade throughout the Sector, creating new opportunities for suppliers "
                        + "capable of responding quickly to the sudden increase in consumption.",
                10f
        );

        tooltip.addPara(
                "Sector-wide demand for %s has increased by %s.",
                10f,
                Misc.getPositiveHighlightColor(),
                commodity.getName(),
                "25%"
        );
    }

    @Override
    public String getImageName() {
        return Global.getSettings().getSpriteName(
                "illustrations",
                "crew_leaving"
        );
    }

    @Override
    public float getImageSourceWidth() {
        return 480f;
    }

    @Override
    public float getImageSourceHeight() {
        return 300f;
    }

    @Override
    public float getImageDisplayWidth() {
        return 480f;
    }

    @Override
    public float getImageDisplayHeight() {
        return 150f;
    }

    @Override
    public float getImageCropX() {
        return 0f;
    }

    @Override
    public float getImageCropY() {
        return 75f;
    }

    @Override
    public float getImageCropWidth() {
        return 480f;
    }

    @Override
    public float getImageCropHeight() {
        return 150f;
    }

    @Override
    public String getEventName() {
        return "Sector Trade Boom";
    }

    @Override
    public void executeEvent() {
        CommoditySpecAPI commodity = getTargetCommodity();

        if (commodity == null) {
            return;
        }

        String commodityId = commodity.getId();
        String modifierId = "ashlib_trade_event_" + commodityId;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            for (Industry industry : market.getIndustries()) {
                if (industry.getDemand(commodityId).getQuantity().getModifiedValue() <= 0f) {
                    continue;
                }

                industry.getDemand(commodityId)
                        .getQuantity()
                        .modifyMult(
                                modifierId,
                                1.25f,
                                "Sector-wide trade boom"
                        );
            }
        }
    }

    protected CommoditySpecAPI getTargetCommodity() {
        if (possibleTargetOfEvent instanceof CommoditySpecAPI) {
            return (CommoditySpecAPI) possibleTargetOfEvent;
        }

        return null;
    }

    protected List<CommoditySpecAPI> getValidCommodities() {
        List<CommoditySpecAPI> commodities = new ArrayList<>();

        for (CommoditySpecAPI commodity : Global.getSettings().getAllCommoditySpecs()) {
            if (commodity.hasTag("nonecon")) {
                continue;
            }

            if (!hasSectorDemand(commodity.getId())) {
                continue;
            }

            commodities.add(commodity);
        }

        return commodities;
    }

    protected boolean hasSectorDemand(String commodityId) {
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            for (Industry industry : market.getIndustries()) {
                if (industry.getDemand(commodityId).getQuantity().getModifiedValue() > 0f) {
                    return true;
                }
            }
        }

        return false;
    }
}