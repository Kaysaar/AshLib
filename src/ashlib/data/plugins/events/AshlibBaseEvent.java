package ashlib.data.plugins.events;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public abstract class AshlibBaseEvent {
    public String uniqueEventTypeID;
    public int firedAmount = 0;
    public int maxAmount = 1;
    public float lastDaysSinceEventFired = 0f;
    public float minimumDaysBetweenFirings = 0f;
    public transient Object possibleTargetOfEvent;

    public AshlibBaseEvent(String uniqueEventTypeID) {
        this.uniqueEventTypeID = uniqueEventTypeID;
    }

    public boolean isAutomaticallyScheduled() {
        return true;
    }

    public boolean shouldFireImmediately() {
        return false;
    }

    public float getImmediateEventDelayDays() {
        return 0f;
    }

    public boolean isMinorEvent() {
        return false;
    }

    /** Null uses the normal economy-tick scheduler; named triggers have their own budget. */
    public String getMinorEventTrigger() {
        return null;
    }

    /**
     * Return a detached snapshot of this occurrence, containing saveable values.
     * Do not return the reusable event or a mutable target shared with later firings.
     */
    public Object createEventData() {
        return null;
    }

    /** A new intel instance is created for every successful major or minor event. */
    public AshlibBaseEventIntel createIntelPlugin() {
        return new AshlibBaseEventIntel(getUniqueEventTypeID(), getEventName(), createEventData());
    }

    /** Grouped reports may already be registered in intel. */
    public boolean shouldPublishIntel(AshlibBaseEventIntel intel) {
        return true;
    }

    public void configureEvent() {
    }

    public boolean canEvenFireEvent() {
        return true;
    }

    public boolean didEventMetBaseCriteria() {
        if (maxAmount >= 0 && firedAmount >= maxAmount) return false;
        return firedAmount == 0 || lastDaysSinceEventFired >= minimumDaysBetweenFirings;
    }

    public void configureEventBeforeFiringUp() {
    }

    public boolean canFireAfterConfiguration() {
        return true;
    }

    public void executeEvent() {
    }

    /** Allows an effect to update its own occurrence record as its lifecycle progresses. */
    public void executeEvent(AshlibBaseEventIntel intel) {
        executeEvent();
    }

    public void createTooltipForEvent(TooltipMakerAPI tooltip) {
    }

    public void createTitleForEvent(TooltipMakerAPI tooltip) {
    }

    public void createOnHoverForConfirmButton(ButtonAPI button, TooltipMakerAPI tooltip) {
    }

    public String getImageName() {
        return null;
    }

    public float getImageSourceWidth() {
        return 480f;
    }

    public float getImageSourceHeight() {
        return 300f;
    }

    public float getImageCropX() {
        return 0f;
    }

    public float getImageCropY() {
        return 75f;
    }

    public float getImageCropWidth() {
        return 480;
    }

    public float getImageCropHeight() {
        return 150f;
    }

    public float getImageDisplayWidth() {
        return 480f;
    }

    public float getImageDisplayHeight() {
        return 150F;
    }

    public String getConfirmButtonText() {
        return "Confirm";
    }

    public String getUniqueEventTypeID() {
        return uniqueEventTypeID;
    }

    public int getFiredAmount() {
        return firedAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public String getEventName() {
        return null;
    }

    public boolean doesHaveOnHoverTooltip() {
        return false;
    }

    public float getLastDaysSinceEventFired() {
        return lastDaysSinceEventFired;
    }

    public Object getPossibleTargetOfEvent() {
        return possibleTargetOfEvent;
    }

    public boolean hasFiredAtLeastOnce() {
        return firedAmount > 0;
    }

    public boolean reachedMaximumAmount() {
        return maxAmount >= 0 && firedAmount >= maxAmount;
    }

    void advanceDays(float days) {
        if (firedAmount > 0) lastDaysSinceEventFired += days;
    }

    void markAsFired() {
        firedAmount++;
        lastDaysSinceEventFired = 0f;
    }

    void clearTarget() {
        possibleTargetOfEvent = null;
    }
}
