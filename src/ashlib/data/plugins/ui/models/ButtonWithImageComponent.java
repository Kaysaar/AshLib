package ashlib.data.plugins.ui.models;

import ashlib.data.plugins.ui.models.resizable.ButtonComponent;
import ashlib.data.plugins.ui.models.resizable.ImageViewer;
import com.fs.starfarer.api.Global;

import java.awt.Color;

public class ButtonWithImageComponent extends ButtonComponent {

    ImageViewer viewer;
    private boolean singlePassBorder;

    public ButtonWithImageComponent(float width, float height, String imageName) {
        super(width, height);
        viewer = new ImageViewer(width, height, imageName);
        addComponent(viewer, 0f, 0f);
    }

    public void setViewerSpriteId(String id) {
        viewer.spriteOfImage = Global.getSettings().getSprite(id);
    }

    public void setImageFrameAppearance(Color borderColor) {
        alphaBG = 0f;
        setColorOfBorder(borderColor);
        setShouldRenderBorders(false);
        singlePassBorder = true;
    }

    @Override
    public void render(float alphaMult) {
        super.render(alphaMult);
        if (singlePassBorder) {
            renderer.render(alphaMult);
        }
    }
}

