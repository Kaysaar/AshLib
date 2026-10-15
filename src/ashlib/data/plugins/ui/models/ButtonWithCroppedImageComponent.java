package ashlib.data.plugins.ui.models;

import ashlib.data.plugins.ui.models.resizable.ButtonComponent;
import ashlib.data.plugins.ui.models.resizable.PartialImageViewer;
import com.fs.starfarer.api.Global;

public class ButtonWithCroppedImageComponent extends ButtonComponent {

    public PartialImageViewer viewer;

    public ButtonWithCroppedImageComponent(
            float width,
            float height,
            String imageName,
            float sourceWidth,
            float sourceHeight
    ) {
        super(width, height);

        viewer = new PartialImageViewer(
                width,
                height,
                imageName,
                sourceWidth,
                sourceHeight
        );

        addComponent(viewer, 0, 0);
    }

    public ButtonWithCroppedImageComponent(
            float width,
            float height,
            String imageName,
            float sourceWidth,
            float sourceHeight,
            float cropX,
            float cropY,
            float cropWidth,
            float cropHeight
    ) {
        this(
                width,
                height,
                imageName,
                sourceWidth,
                sourceHeight
        );

        setCrop(
                cropX,
                cropY,
                cropWidth,
                cropHeight
        );
    }

    public void setCrop(
            float x,
            float y,
            float width,
            float height
    ) {
        viewer.setRegion(
                x,
                y,
                width,
                height
        );
    }

    public void setCropNormalized(
            float x,
            float y,
            float width,
            float height
    ) {
        viewer.setRegionNormalized(
                x,
                y,
                width,
                height
        );
    }

    public void setViewerSpriteId(String id) {
        viewer.spriteOfImage = Global.getSettings().getSprite(id);
    }

    public PartialImageViewer getViewer() {
        return viewer;
    }
}