package ashlib.data.plugins.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.Set;
import java.util.UUID;

public class AshlibBaseEventIntel extends BaseIntelPlugin {
    private final String occurrenceId;
    private final String eventTypeId;
    private final String eventName;
    private final Object eventData;
    private final long occurredAt;
    private final String occurredOn;

    public AshlibBaseEventIntel() {
        this("ashlib_event", "Event Report", null);
    }

    public AshlibBaseEventIntel(String eventTypeId, String eventName, Object eventData) {
        this.occurrenceId = UUID.randomUUID().toString();
        this.eventTypeId = eventTypeId;
        this.eventName = eventName == null ? "Event Report" : eventName;
        this.eventData = eventData;
        this.occurredAt = Global.getSector().getClock().getTimestamp();
        this.occurredOn = Global.getSector().getClock().getDateString();
        setPlayerVisibleTimestamp(occurredAt);
    }

    public String getOccurrenceId() {
        return occurrenceId;
    }

    public String getEventTypeId() {
        return eventTypeId;
    }

    public Object getEventData() {
        return eventData;
    }

    public long getOccurredAt() {
        return occurredAt;
    }

    public String getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getName() {
        return eventName;
    }

    @Override
    public boolean hasLargeDescription() {
        return true;
    }

    @Override
    public boolean hasSmallDescription() {
        return false;
    }

    protected Color getEventColor() {
        return Misc.getButtonTextColor();
    }

    protected Color getEventBackgroundColor() {
        return Misc.getDarkPlayerColor();
    }

    protected String getEventHeading() {
        return "Campaign Bulletin";
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getEventColor(), 0f);
        info.addPara(occurredOn, Misc.getGrayColor(), 3f);
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        float contentWidth = Math.max(1f, width - 32f);
        TooltipMakerAPI tooltip = panel.createUIElement(contentWidth, Math.max(1f, height - 32f), true);
        tooltip.addSectionHeading(getEventHeading(), getEventColor(), getEventBackgroundColor(), Alignment.MID, 0f);
        tooltip.addSpacer(12f);
        tooltip.addTitle(getName(), getEventColor());
        tooltip.addPara("Recorded on " + occurredOn, Misc.getGrayColor(), 5f);
        createDescription(tooltip, contentWidth);
        panel.addUIElement(tooltip).inTL(16f, 16f);
    }

    protected void createDescription(TooltipMakerAPI tooltip, float width) {
        tooltip.addPara("This event has taken place. Its report is retained in the campaign records.", 10f);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("Events");
        return tags;
    }
}
