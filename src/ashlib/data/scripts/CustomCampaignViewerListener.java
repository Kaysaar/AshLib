package ashlib.data.scripts;

import ashlib.data.plugins.ui.CustomCampaignFleetViewer;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.CampaignFleetMemberView;
import com.fs.starfarer.campaign.fleet.FleetMember;

public interface CustomCampaignViewerListener {
    public CampaignFleetMemberView generateCampaignFleetViewer(CampaignFleet fleet, FleetMember member);
}
