package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.MethodRef;

import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.HashSet;
import java.util.Set;

/** Resolves the RC8 host contract from type metadata without constructing tabs. */
public final class CampaignHostContract {
    public static final String VERSION = CampaignCompatibility.GAME_VERSION;
    public static final String PROFILE = CampaignCompatibility.HOST_PROFILE;
    public final Class<?> base, closed, witness;

    private CampaignHostContract(Class<?> base, Class<?> closed, Class<?> witness) {
        this.base = base;
        this.closed = closed;
        this.witness = witness;
    }

    public static CampaignHostContract resolve(Class<?> coreType, String version) throws Exception {
        require(VERSION.equals(version), "UNSUPPORTED_GAME_VERSION");
        require(UIPanelAPI.class.isAssignableFrom(coreType), "CORE_NOT_PANEL");
        MethodRef show = CampaignUiAccess.showMethod(coreType);
        MethodRef current = CampaignUiAccess.method(coreType, "getCurrentTab");
        MethodRef dismiss = CampaignUiAccess.method(coreType, "dismiss", int.class);
        require(
                show != null
                        && current != null
                        && dismiss != null
                        && dismiss.getReturnType() == void.class,
                "INCOMPLETE_CORE_CONTRACT");
        Class<?> base = show.getParameterTypes()[1];
        require(current.getReturnType() == base, "CURRENT_TAB_BASE_MISMATCH");
        // In RC8, Core implements a callback nested in Fleet's tab class.
        // That enclosing tab exposes tabClosed, allowing discovery before any tab is open.
        Set<Class<?>> interfaces = new HashSet<Class<?>>();
        for (Class<?> type = coreType; type != null; type = type.getSuperclass()) {
            for (Class<?> implementedInterface : type.getInterfaces()) {
                collect(implementedInterface, interfaces);
            }
        }
        CampaignHostContract result = null;
        for (Class<?> implementedInterface : interfaces) {
            Class<?> tab = implementedInterface.getDeclaringClass();
            if (tab == null || tab == coreType || !base.isAssignableFrom(tab)) {
                continue;
            }
            Class<?> contract = CampaignUiAccess.closingContract(tab);
            if (contract == null) {
                continue;
            }
            require(
                    tab.getClassLoader() == base.getClassLoader()
                            && contract.getClassLoader() == base.getClassLoader(),
                    "CROSS_LOADER_CONTRACT");
            require(result == null || result.closed == contract, "AMBIGUOUS_CLOSING_CONTRACT");
            result = new CampaignHostContract(base, contract, tab);
        }
        require(result != null, "NO_ENCLOSING_TAB_CONTRACT");
        return result;
    }

    private static void collect(Class<?> type, Set<Class<?>> found) {
        if (!found.add(type)) {
            return;
        }
        for (Class<?> parent : type.getInterfaces()) {
            collect(parent, found);
        }
    }

    private static void require(boolean condition, String reason) {
        if (!condition) {
            throw new IllegalStateException(PROFILE + ": " + reason);
        }
    }
}
