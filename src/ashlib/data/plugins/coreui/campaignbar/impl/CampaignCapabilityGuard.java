package ashlib.data.plugins.coreui.campaignbar.impl;

/** After a failure, retries only on an explicit request. */
public final class CampaignCapabilityGuard {
    public interface Work {
        void run() throws Exception;
    }

    public interface Failures {
        void report(String operation, Throwable failure);
    }

    private final String name;
    private final Failures failures;
    private boolean blocked;

    public CampaignCapabilityGuard(String name, Failures failures) {
        this.name = name;
        this.failures = failures;
    }

    public boolean run(String operation, boolean explicit, Work work) {
        if (blocked && !explicit) {
            return false;
        }
        try {
            work.run();
            blocked = false;
            return true;
        } catch (Throwable failure) {
            blocked = true;
            failures.report(name + "/" + operation, failure);
            return false;
        }
    }

    public boolean isBlocked() {
        return blocked;
    }
}
