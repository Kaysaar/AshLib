package ashlib.data.plugins.ui.models.resizable;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;

import java.awt.*;

public class ImageViewer extends ResizableComponent {

    public SpriteAPI spriteOfImage;
    public Color colorOverlay;
    public float alphaMult = 1f;

    public ImageViewer(float width, float height, String imagePath) {
        componentPanel = Global.getSettings().createCustom(width, height, this);
        spriteOfImage = Global.getSettings().getSprite(imagePath);
    }

    public void setAlphaMult(float alphaMult) {
        this.alphaMult = alphaMult;
    }

    public void setColorOverlay(Color colorOverlay) {
        this.colorOverlay = colorOverlay;
    }

    protected void prepareSprite(float alphaMult) {
        if (colorOverlay != null) {
            spriteOfImage.setColor(colorOverlay);
        }

        spriteOfImage.setAlphaMult(alphaMult * this.alphaMult);

        spriteOfImage.setSize(
                componentPanel.getPosition().getWidth(),
                componentPanel.getPosition().getHeight()
        );
    }

    @Override
    public void render(float alphaMult) {
        super.render(alphaMult);

        prepareSprite(alphaMult);

        spriteOfImage.renderAtCenter(
                componentPanel.getPosition().getCenterX(),
                componentPanel.getPosition().getCenterY()
        );
    }
}