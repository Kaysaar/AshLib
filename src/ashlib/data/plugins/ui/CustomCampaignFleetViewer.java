package ashlib.data.plugins.ui;

import ashlib.data.scripts.CustomCampaignViewerListener;
import ashlib.data.scripts.CustomCampaignViewerListenerManager;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.CampaignFleetMemberView;
import com.fs.starfarer.campaign.fleet.CampaignFleetView;
import com.fs.starfarer.campaign.fleet.FleetMember;

import java.awt.*;

public class CustomCampaignFleetViewer extends CampaignFleetView {

    public CustomCampaignFleetViewer(CampaignFleet fleet) {
        super(fleet);
    }
    public static void replaceFleetView(CampaignFleetAPI fleetToReplace){
        CampaignFleet fleet = (CampaignFleet) fleetToReplace;

        if(fleet==null){
            return;
        }

        CampaignFleetView oldView = fleet.getFleetView();

        if(oldView instanceof CustomCampaignFleetViewer){
            return;
        }

        CustomCampaignFleetViewer newView =
                new CustomCampaignFleetViewer(fleet);

        Object lightColor =
                ReflectionUtilis.getPrivateVariableFromSuperClass(
                        "lightColor",
                        oldView
                );

        if(oldView.getLightSource()!=null){
            newView.setLightSource(
                    oldView.getLightSource(),
                    lightColor instanceof Color
                            ? (Color) lightColor
                            : null
            );
        }

        ReflectionUtilis.setPrivateVariableFromSuperclass(
                "fleetView",
                fleet,
                newView
        );

        /*
         * Populates CollectionView through the overridden createItemView().
         */
        newView.advance(1f);
    }
    public static void replacePlayerFleetView(){
        replaceFleetView(Global.getSector().getPlayerFleet());
    }
    @Override
    public CampaignFleetMemberView createItemView(Object item) {
        if(!(item instanceof FleetMember)){
            return super.createItemView(item);
        }

        FleetMember member = (FleetMember) item;
        CampaignFleetMemberView view = null;
        for (CustomCampaignViewerListener value : CustomCampaignViewerListenerManager.listeners.values()) {
            if(value.generateCampaignFleetViewer(getFleet(),member)!=null){
                view = value.generateCampaignFleetViewer(getFleet(),member);
                break;
            }
        }
        if(view==null){
            view = new CampaignFleetMemberView(
                    getFleet(),
                    member
            );
        }
        return view;
    }



}