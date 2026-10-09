package ashlib.data.plugins.coreui.campaignbar.impl;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Full exception trees without StringWriter/other java.io classes restricted to scripts. */
public final class CampaignDiagnostics {
    private CampaignDiagnostics() {}

    public static String trace(Throwable error) {
        StringBuilder out = new StringBuilder();
        append(
                out,
                error,
                "",
                Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>()));
        return out.toString();
    }

    private static void append(
            StringBuilder out, Throwable error, String prefix, Set<Throwable> seen) {
        if (error == null) {
            return;
        }
        out.append(prefix)
                .append(error.getClass().getName())
                .append(": ")
                .append(error.getMessage())
                .append('\n');
        if (!seen.add(error)) {
            out.append("[exception cycle]\n");
            return;
        }
        for (StackTraceElement frame : error.getStackTrace()) {
            out.append("\tat ").append(frame).append('\n');
        }
        for (Throwable suppressed : error.getSuppressed()) {
            append(out, suppressed, "Suppressed: ", seen);
        }
        append(out, error.getCause(), "Caused by: ", seen);
    }
}
