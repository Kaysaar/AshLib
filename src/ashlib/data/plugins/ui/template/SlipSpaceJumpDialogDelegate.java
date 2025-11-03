package ashlib.data.plugins.ui.template;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

public class SlipSpaceJumpDialogDelegate implements CustomVisualDialogDelegate {
    protected DialogCallbacks callbacks;
    protected SlipSpaceDialog plugin;
    protected InteractionDialogAPI dialog;
    protected StarSystemAPI location;
    public SlipSpaceJumpDialogDelegate(InteractionDialogAPI dialog,StarSystemAPI location) {
        plugin = new SlipSpaceDialog();
        this.location = location;
        this.dialog = dialog;
    }
    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.callbacks = callbacks;
        plugin.init(panel, callbacks, dialog,location);
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return plugin;
    }

    @Override
    public float getNoiseAlpha() {
        return 0.5f;
    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void reportDismissed(int option) {

    }
}
