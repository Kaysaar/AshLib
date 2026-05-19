package ashlib.data.plugins.ui;

import ashlib.data.plugins.ui.models.ExtendedUIPanelPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;

import static com.fs.starfarer.api.util.Misc.isMilitary;

public class EntityWithNameComponent implements ExtendedUIPanelPlugin {
    CustomPanelAPI mainPanel, componentPanel;
    SectorEntityToken token;
    boolean showSize = false;
    boolean factionStars = false;
    public EntityWithNameComponent(SectorEntityToken token, float width, float height) {
        this.token = token;
        mainPanel = Global.getSettings().createCustom(width, height, this);


    }
    public EntityWithNameComponent(SectorEntityToken token, float width, float height,boolean showSize) {
        this.token = token;
        mainPanel = Global.getSettings().createCustom(width, height, this);
        this.showSize = showSize;

    }
    public EntityWithNameComponent(SectorEntityToken token,boolean factionForStars ,float width, float height) {
        this.token = token;
        mainPanel = Global.getSettings().createCustom(width, height, this);
        this.factionStars = factionForStars;

    }

    @Override
    public CustomPanelAPI getMainPanel() {
        return mainPanel;
    }

    @Override
    public void createUI() {
        if (componentPanel != null) {
            mainPanel.removeComponent(componentPanel);
        }
        componentPanel = Global.getSettings().createCustom(mainPanel.getPosition().getWidth(), mainPanel.getPosition().getHeight(), null);
        TooltipMakerAPI tooltip = componentPanel.createUIElement(componentPanel.getPosition().getWidth(), componentPanel.getPosition().getHeight(), false);
        float width = componentPanel.getPosition().getWidth();
        float height = componentPanel.getPosition().getHeight();
        float boxSize = Math.min(width, height);
        EntityRenderer renderer = new EntityRenderer(token, boxSize);
        float startX = (width / 2) - (boxSize / 2);
        float startY = 0;
        Color color = Misc.getTooltipTitleAndLightHighlightColor();
        if (token instanceof PlanetAPI planet) {
            color = planet.getSpec().getIconColor();
            if(factionStars){
                if(planet.isStar()||planet.isBlackHole()){
                    color = getClaimingFaction(planet).getBaseUIColor();
                }
            }

        }
        if (token.getFaction() != null && !token.getFaction().getId().equals(Factions.NEUTRAL)) {
            color = token.getFaction().getBaseUIColor();
        }

        UIComponentAPI comp = tooltip.addCustomDoNotSetPosition(renderer.getMainPanel());
        LabelAPI labelAPI;
        if (token.getMarket() != null && token.getFaction() != null && !token.getFaction().getId().equals(Factions.NEUTRAL)&&showSize) {
            String name = token.getName();
            String sizeSuffix = "(size " + token.getMarket().getSize() + ")";

            LabelAPI suffixLabel = Global.getSettings().createLabel(sizeSuffix, Fonts.DEFAULT_SMALL);
            float suffixWidth = suffixLabel.computeTextWidth(sizeSuffix);

            float panelWidth = componentPanel.getPosition().getWidth()-30;
            float allowedWidthMax = panelWidth - suffixWidth;

            String fittedName = trimToWidthWithEllipsis(name, allowedWidthMax, Fonts.DEFAULT_SMALL);

            labelAPI = tooltip.addPara(fittedName + " %s", boxSize - 2, color, Misc.getGrayColor(), "(size " + token.getMarket().getSize() + ")");

        } else {
            labelAPI = tooltip.addPara(token.getName(), color, boxSize - 2);
        }

        labelAPI.setAlignment(Alignment.MID);
        comp.getPosition().inTL(startX, startY);
        componentPanel.addUIElement(tooltip).inTL(0, 0);
        mainPanel.addComponent(componentPanel).inTL(0, 0);

    }

    @Override
    public void clearUI() {

    }

    @Override
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void renderBelow(float alphaMult) {

    }

    @Override
    public void render(float alphaMult) {

    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }
    private static String trimToWidthWithEllipsis(String text, float maxWidth, String fontId) {
        if (text == null) return "";
        if (maxWidth <= 0) return "...";

        final String ellipsis = "...";

        LabelAPI measurer = Global.getSettings().createLabel("", fontId);

        // Already fits
        measurer.setText(text);
        if (measurer.computeTextWidth(text) <= maxWidth) return text;

        // Even "..." doesn't fit; return it anyway (or "" if you prefer)
        measurer.setText(ellipsis);
        if (measurer.computeTextWidth(ellipsis) > maxWidth) return ellipsis;

        int lo = 0;
        int hi = text.length(); // inclusive upper bound for "take"
        int best = 0;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1; // avoid overflow
            String candidate = text.substring(0, mid) + ellipsis;

            measurer.setText(candidate);
            float w = measurer.computeTextWidth(candidate);

            if (w <= maxWidth) {
                best = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        return text.substring(0, best) + ellipsis;
    }
    public static FactionAPI getClaimingFaction(SectorEntityToken planet) {
        if (planet.getStarSystem() != null) {
            String claimedBy = planet.getStarSystem().getMemoryWithoutUpdate().getString(MemFlags.CLAIMING_FACTION);
            if (claimedBy != null) {
                return Global.getSector().getFaction(claimedBy);
            }
        }

        int max = 0;
        MarketAPI result = null;
        List<MarketAPI> markets = Global.getSector().getEconomy().getMarkets(planet.getContainingLocation());
        for (MarketAPI curr : markets) {
            if (curr.isHidden()) continue;
            int score = curr.getSize();
            for (MarketAPI other : markets) {
                if (other != curr && other.getFaction() == curr.getFaction()) score++;
            }
            if (isMilitary(curr)) score += 10;
            if (score > max) {
                max = score;
                result = curr;
            }
        }
        if (result == null) return null;

        return result.getFaction();
    }
}
