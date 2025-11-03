package ashlib.data.plugins.ui.template;

import ashlib.data.plugins.misc.AshMisc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.MarkerData;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.kaysaar.aotd.vok.plugins.ReflectionUtilis;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Set;

public class SlipSpaceEntityPicker extends BaseCampaignEntityPickerListener {
    SectorEntityToken pickedEntity;
    Vector2f locationInSystem;
    InteractionDialogAPI dialog;

    public SlipSpaceEntityPicker(InteractionDialogAPI dialog) {
        this.dialog = dialog;
    }

    public String getMenuItemNameOverrideFor(SectorEntityToken entity) {
        return null;
    }

    @Override
    public void pickedEntity(SectorEntityToken entity) {
        pickedEntity = entity;
        ReflectionUtilis.invokeMethodWithAutoProjection("dialogDismissed",dialog,null,1);
        dialog.showCustomVisualDialog(1000,600, new SlipSpaceJumpDialogDelegate(dialog,pickedEntity.getStarSystem()));

    }

    @Override
    public void cancelledEntityPicking() {
        dialog.dismiss();
    }

    public String getSelectedTextOverrideFor(SectorEntityToken entity) {
        if (locationInSystem != null) {
            return entity.getName() + " - " + entity.getContainingLocation().getNameWithTypeShort() + " (" + locationInSystem.getX() + "," + locationInSystem.getY() + ")";
        }
        return entity.getName() + " - " + entity.getContainingLocation().getNameWithTypeShort();
    }


    @Override
    public void createInfoText(TooltipMakerAPI info, SectorEntityToken entity) {

    }

    @Override
    public boolean canConfirmSelection(SectorEntityToken entity) {
        return true;
    }

    @Override
    public float getFuelColorAlphaMult() {
        return 0;
    }

    @Override
    public float getFuelRangeMult() {
        return 0;
    }


}
