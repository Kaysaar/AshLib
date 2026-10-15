package ashlib.data.scripts;

import ashlib.data.plugins.ui.CustomCampaignFleetViewer;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.CampaignFleetMemberView;
import com.fs.starfarer.campaign.fleet.FleetMember;

public interface CustomCampaignViewerListener {
    public CampaignFleetMemberView generateCampaignFleetViewer(CampaignFleetAPI fleet, FleetMemberAPI member);
}
