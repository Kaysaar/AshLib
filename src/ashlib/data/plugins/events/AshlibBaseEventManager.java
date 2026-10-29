package ashlib.data.plugins.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Objects;

public class AshlibBaseEventManager implements EconomyTickListener, EveryFrameScript {
    private static final Logger log = Global.getLogger(AshlibBaseEventManager.class);

    public LinkedHashMap<String, AshlibBaseEvent> events = new LinkedHashMap<>();

    // Scheduling settings and limits apply to random major events only.
    public int monthsPassedThisYear = 0;
    public int eventsFiredThisYear = 0;
    public int immediateEventsFiredThisMonth = 0;
    public int immediateEventsFiredThisYear = 0;
    public int maxEventsPerYear = 4;
    public float baseMonthlyEventChance = 0.10f;
    public float monthlyEventChanceIncrease = 0.075f;
    public float eventTickChance = 0.35f;
    public boolean eventCanFireThisMonth = false;

    // Minor events roll every economy tick, independently of the major gate.
    public int minorEventsFiredThisMonth = 0;
    public int minorEventsFiredThisYear = 0;
    public int maxMinorEventsPerMonth = 3;
    public int maxMinorEventsPerYear = 36;
    public float minorEventTickChance = 0.65f;

    public String forcedEventThisMonth = null;
    public String devModeTestEventId = "ashlib_example_event";

    public transient boolean eventPopupOpen = false;
    public transient Random random;
    private transient AshlibBaseEvent popupEvent;
    private transient boolean popupEventImmediate;
    private transient boolean frameScriptRegistered;
    private LinkedHashMap<String, Long> immediateEventEligibleSince = new LinkedHashMap<>();
    private LinkedHashSet<String> requestedImmediateEvents = new LinkedHashSet<>();

    private LinkedHashMap<String, MinorEventTrigger> minorEventTriggers = new LinkedHashMap<>();
    public int triggeredMinorEventsFiredThisMonth;
    public int triggeredMinorEventsFiredThisYear;

    /** Saved delays measured from the caller's actual allocation/other milestone. */
    private static class MinorEventTrigger {
        String triggerId;
        long scheduledAt;
        float delayDays;
        int attempts;
        float chance;
    }

    public void scheduleMinorEventTrigger(String triggerId, float delayDays, int attempts, float chance) {
        if (minorEventTriggers == null) minorEventTriggers = new LinkedHashMap<>();
        MinorEventTrigger trigger = new MinorEventTrigger();
        trigger.triggerId = triggerId;
        trigger.scheduledAt = Global.getSector().getClock().getTimestamp();
        trigger.delayDays = Math.max(0f, delayDays);
        trigger.attempts = Math.max(0, attempts);
        trigger.chance = chance;
        minorEventTriggers.put(triggerId + ":" + trigger.delayDays, trigger);
    }

    /** Call only from a safe campaign-thread point, after economy workers have finished. */
    public void advanceMinorEventTriggers() {
        if (minorEventTriggers == null || minorEventTriggers.isEmpty()) return;
        for (String key : new ArrayList<>(minorEventTriggers.keySet())) {
            MinorEventTrigger trigger = minorEventTriggers.get(key);
            if (Global.getSector().getClock().getElapsedDaysSince(trigger.scheduledAt) < trigger.delayDays) continue;
            minorEventTriggers.remove(key);
            for (int i = 0; i < trigger.attempts; i++) {
                if (!rollChance(trigger.chance)) continue;
                List<AshlibBaseEvent> valid = collectValidEvents(true, trigger.triggerId);
                if (valid.isEmpty()) break;
                Collections.shuffle(valid, getRandom());
                for (AshlibBaseEvent event : valid) {
                    if (activateEvent(event)) break;
                }
            }
        }
    }

    public static AshlibBaseEventManager getInstance() {
        if (!Global.getSector().getListenerManager().hasListenerOfClass(AshlibBaseEventManager.class)) {
            // Keep scheduling counters and event cooldowns when the campaign is saved.
            Global.getSector().getListenerManager().addListener(new AshlibBaseEventManager(), false);
        }
        AshlibBaseEventManager manager = Global.getSector().getListenerManager()
                .getListeners(AshlibBaseEventManager.class).iterator().next();
        if (!manager.frameScriptRegistered) {
            Global.getSector().addTransientScript(manager);
            manager.frameScriptRegistered = true;
        }
        return manager;
    }

    protected Object readResolve() {
        if (events == null) events = new LinkedHashMap<>();
        if (immediateEventEligibleSince == null) immediateEventEligibleSince = new LinkedHashMap<>();
        if (requestedImmediateEvents == null) requestedImmediateEvents = new LinkedHashSet<>();
        immediateEventEligibleSince.keySet().retainAll(events.keySet());
        requestedImmediateEvents.retainAll(events.keySet());
        eventPopupOpen = false;
        frameScriptRegistered = false;
        popupEvent = null;
        popupEventImmediate = false;
        random = null;
        for (AshlibBaseEvent event : events.values()) {
            event.clearTarget();
        }
        return this;
    }

    public LinkedHashMap<String, AshlibBaseEvent> getEvents() {
        return events;
    }

    public AshlibBaseEvent getEvent(String eventId) {
        return events.get(eventId);
    }

    public AshlibBaseEvent addEvent(AshlibBaseEvent event) {
        if (event == null) return null;
        String id = event.getUniqueEventTypeID();
        if (id == null || id.isEmpty()) return null;

        AshlibBaseEvent existing = events.get(id);
        if (existing != null && existing == popupEvent) return existing;
        if (existing != null && existing.getClass().equals(event.getClass())) {
            existing.configureEvent();
            existing.clearTarget();
            return existing;
        }

        event.configureEvent();
        if (existing != null) {
            event.firedAmount = existing.firedAmount;
            event.lastDaysSinceEventFired = existing.lastDaysSinceEventFired;
        }
        events.put(id, event);
        return event;
    }

    public void removeEvent(String eventId) {
        AshlibBaseEvent event = events.get(eventId);
        if (event != null && event == popupEvent) return;
        events.remove(eventId);
        immediateEventEligibleSince.remove(eventId);
        requestedImmediateEvents.remove(eventId);
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null || Global.getSector().isPaused()) return;
        float days = Global.getSector().getClock().convertToDays(amount);
        for (AshlibBaseEvent event : new ArrayList<>(events.values())) {
            if (!event.isMinorEvent() && event.shouldFireImmediately()) {
                event.advanceDays(days);
            }
        }
        checkImmediateMajorEvents();
    }

    public boolean checkImmediateMajorEvents() {
        boolean opened = false;
        for (AshlibBaseEvent event : new ArrayList<>(events.values())) {
            String id = event.getUniqueEventTypeID();
            try {
                if (event == popupEvent) continue;
                boolean automatic = event.isAutomaticallyScheduled() && event.shouldFireImmediately();
                if (event.isMinorEvent() || (!automatic && !requestedImmediateEvents.contains(id))) {
                    immediateEventEligibleSince.remove(id);
                    continue;
                }
                if (activateEvent(event, true)) opened = true;
            } catch (Exception ex) {
                immediateEventEligibleSince.remove(id);
                log.error("Failed checking immediate event " + id, ex);
            }
        }
        return opened;
    }

    private boolean isImmediateEventReady(AshlibBaseEvent event) {
        String id = event.getUniqueEventTypeID();
        if (!event.didEventMetBaseCriteria() || !event.canEvenFireEvent()) {
            immediateEventEligibleSince.remove(id);
            if (event.reachedMaximumAmount()) requestedImmediateEvents.remove(id);
            return false;
        }
        float delay = Math.max(0f, event.getImmediateEventDelayDays());
        if (delay == 0f) return true;
        Long since = immediateEventEligibleSince.get(id);
        if (since == null) {
            since = Global.getSector().getClock().getTimestamp();
            immediateEventEligibleSince.put(id, since);
        }
        return Global.getSector().getClock().getElapsedDaysSince(since) >= delay;
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        if (iterIndex == -1) return;

        float days = Global.getSettings().getFloat("economyIntervalnGameDays");
        for (AshlibBaseEvent event : new ArrayList<>(events.values())) {
            if (event.isMinorEvent() || !event.shouldFireImmediately()) event.advanceDays(days);
        }

        boolean forcedMinorFired = false;
        if (forcedEventThisMonth != null) {
            AshlibBaseEvent event = events.get(forcedEventThisMonth);
            if (event == null || !event.didEventMetBaseCriteria()) {
                forcedEventThisMonth = null;
            } else if (activateEvent(event.getUniqueEventTypeID())) {
                forcedMinorFired = event.isMinorEvent();
                forcedEventThisMonth = null;
            }
        }

        if (!forcedMinorFired
                && belowLimit(minorEventsFiredThisMonth, maxMinorEventsPerMonth)
                && belowLimit(minorEventsFiredThisYear, maxMinorEventsPerYear)
                && rollChance(minorEventTickChance)) {
            fireRandomEvent(true);
        }

        AshlibBaseEvent forced = events.get(forcedEventThisMonth);
        if ((forced == null || forced.isMinorEvent())
                && eventCanFireThisMonth
                && belowLimit(eventsFiredThisYear, maxEventsPerYear)
                && !eventPopupOpen
                && canOpenEvent()
                && rollChance(eventTickChance)) {
            fireRandomEvent(false);
        }
    }

    @Override
    public void reportEconomyMonthEnd() {
        minorEventsFiredThisMonth = 0;
        triggeredMinorEventsFiredThisMonth = 0;
        immediateEventsFiredThisMonth = 0;
        monthsPassedThisYear++;
        if (monthsPassedThisYear >= 12) {
            monthsPassedThisYear = 0;
            eventsFiredThisYear = 0;
            immediateEventsFiredThisYear = 0;
            minorEventsFiredThisYear = 0;
            triggeredMinorEventsFiredThisYear = 0;
        }

        eventCanFireThisMonth = belowLimit(eventsFiredThisYear, maxEventsPerYear)
                && rollChance(getCurrentMonthlyEventChance());
        forcedEventThisMonth = null;
        if (Global.getSettings().isDevMode()) {
            AshlibBaseEvent event = events.get(devModeTestEventId);
            if (event != null && event.didEventMetBaseCriteria()) {
                forcedEventThisMonth = devModeTestEventId;
            }
        }
    }

    public float getCurrentMonthlyEventChance() {
        return Math.max(0f, Math.min(1f,
                baseMonthlyEventChance + monthlyEventChanceIncrease * monthsPassedThisYear));
    }

    public List<AshlibBaseEvent> getValidEvents() {
        return collectValidEvents(null);
    }

    public List<AshlibBaseEvent> getValidEvents(boolean minor) {
        return collectValidEvents(minor);
    }

    private List<AshlibBaseEvent> collectValidEvents(Boolean minor) {
        return collectValidEvents(minor, null);
    }

    private List<AshlibBaseEvent> collectValidEvents(Boolean minor, String triggerId) {
        List<AshlibBaseEvent> valid = new ArrayList<>();
        for (AshlibBaseEvent event : new ArrayList<>(events.values())) {
            try {
                if (!event.isAutomaticallyScheduled()) continue;
                if (!event.isMinorEvent() && (event.shouldFireImmediately() || requestedImmediateEvents.contains(event.getUniqueEventTypeID()))) continue;
                if (minor != null && event.isMinorEvent() != minor) continue;
                if (!Objects.equals(triggerId, event.getMinorEventTrigger())) continue;
                if (event.didEventMetBaseCriteria() && event.canEvenFireEvent()) valid.add(event);
            } catch (Exception ex) {
                log.error("Failed checking event " + event.getUniqueEventTypeID(), ex);
            }
        }
        return valid;
    }

    private void fireRandomEvent(boolean minor) {
        List<AshlibBaseEvent> valid = getValidEvents(minor);
        Collections.shuffle(valid, getRandom());
        for (AshlibBaseEvent event : valid) {
            if (activateEvent(event)) return;
        }
    }


    public boolean fireMajorEventImmediately(String eventId) {
        AshlibBaseEvent event = events.get(eventId);
        if (event == null || event.isMinorEvent()) return false;
        requestedImmediateEvents.add(eventId);
        return activateEvent(event, true);
    }

    // Manual/dev activation bypasses schedule chances and caps, but still counts a successful firing.
    public boolean activateEvent(String eventId) {
        return activateEvent(events.get(eventId));
    }

    protected boolean activateEvent(AshlibBaseEvent event) {
        if (event == null) return false;
        return activateEvent(event, !event.isMinorEvent() && (event.shouldFireImmediately() || requestedImmediateEvents.contains(event.getUniqueEventTypeID())));
    }

    protected boolean activateEvent(AshlibBaseEvent event, boolean immediate) {
        if (event == null) return false;
        boolean minor = event.isMinorEvent();
        if (event == popupEvent) return false;

        try {
            if (immediate && !minor) {
                if (!isImmediateEventReady(event)) return false;
            } else if (!event.didEventMetBaseCriteria() || !event.canEvenFireEvent()) {
                return false;
            }
            if (!minor && (eventPopupOpen || !canOpenEvent())) return false;
            event.clearTarget();
            event.configureEventBeforeFiringUp();
            if (!event.canFireAfterConfiguration()) {
                immediateEventEligibleSince.remove(event.getUniqueEventTypeID());
                event.clearTarget();
                return false;
            }

            if (minor) {
                completeEvent(event, false);
                event.clearTarget();
            } else {
                popupEvent = event;
                popupEventImmediate = immediate;
                eventPopupOpen = true;
                new AshlibBaseEventPopUp(event).show();
            }
            return true;
        } catch (Exception ex) {
            if (popupEvent == event) {
                popupEvent = null;
                popupEventImmediate = false;
                eventPopupOpen = false;
            }
            event.clearTarget();
            immediateEventEligibleSince.remove(event.getUniqueEventTypeID());
            log.error("Failed activating event " + event.getUniqueEventTypeID(), ex);
            return false;
        }
    }

    public void confirmEvent(AshlibBaseEvent event) {
        // Ignore stale buttons and repeated confirmation. Minor events never use this path.
        if (event == null || event != popupEvent || !eventPopupOpen) return;
        try {
            if (event.canFireAfterConfiguration()) {
                completeEvent(event, popupEventImmediate);
            } else {
                immediateEventEligibleSince.remove(event.getUniqueEventTypeID());
            }
        } catch (Exception ex) {
            log.error("Failed confirming event " + event.getUniqueEventTypeID(), ex);
        } finally {
            event.clearTarget();
            popupEvent = null;
            popupEventImmediate = false;
            eventPopupOpen = false;
        }
    }

    private void completeEvent(AshlibBaseEvent event, boolean immediate) {
        // Snapshot the occurrence while its target still exists. Do not store the reusable event.
        AshlibBaseEventIntel intel = event.createIntelPlugin();
        if (intel == null) {
            intel = new AshlibBaseEventIntel(event.getUniqueEventTypeID(), event.getEventName(), event.createEventData());
        }

        event.executeEvent(intel);
        event.markAsFired();
        immediateEventEligibleSince.remove(event.getUniqueEventTypeID());
        requestedImmediateEvents.remove(event.getUniqueEventTypeID());
        if (event.isMinorEvent() && event.getMinorEventTrigger() != null) {
            triggeredMinorEventsFiredThisMonth++;
            triggeredMinorEventsFiredThisYear++;
        } else if (event.isMinorEvent()) {
            minorEventsFiredThisMonth++;
            minorEventsFiredThisYear++;
        } else if (immediate) {
            immediateEventsFiredThisMonth++;
            immediateEventsFiredThisYear++;
        } else {
            eventsFiredThisYear++;
            eventCanFireThisMonth = false;
        }

        // A notification failure must not make an already-applied event fire again.
        try {
            if (event.shouldPublishIntel(intel)) {
                Global.getSector().getIntelManager().addIntel(intel, !event.isMinorEvent());
            }
        } catch (Exception ex) {
            log.error("Event applied, but failed to publish intel " + intel.getOccurrenceId(), ex);
        }
    }

    protected boolean canOpenEvent() {
        return Global.getSector() != null
                && Global.getSector().getCampaignUI() != null
                && Global.getSector().getCampaignUI().getCurrentInteractionDialog() == null;
    }

    private boolean belowLimit(int count, int limit) {
        return limit < 0 || count < limit;
    }

    private boolean rollChance(float chance) {
        return chance > 0f && (chance >= 1f || getRandom().nextFloat() < chance);
    }

    protected Random getRandom() {
        if (random == null) random = new Random();
        return random;
    }
}
