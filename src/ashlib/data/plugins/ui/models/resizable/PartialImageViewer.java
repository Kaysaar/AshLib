package ashlib.data.plugins.ui.models.resizable;

import java.awt.Color;

public class PartialImageViewer extends ImageViewer {

    protected float sourceWidth;
    protected float sourceHeight;

    protected float regionX = 0f;
    protected float regionY = 0f;
    protected float regionWidth = 1f;
    protected float regionHeight = 1f;

    public PartialImageViewer(
            float width,
            float height,
            String imagePath,
            float sourceWidth,
            float sourceHeight
    ) {
        super(width, height, imagePath);

        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    public void setRegion(
            float x,
            float y,
            float width,
            float height
    ) {
        if (sourceWidth <= 0f || sourceHeight <= 0f) {
            setRegionNormalized(0f, 0f, 0f, 0f);
            return;
        }
        regionX = x / sourceWidth;
        regionY = y / sourceHeight;
        regionWidth = width / sourceWidth;
        regionHeight = height / sourceHeight;
    }

    public void setRegionNormalized(
            float x,
            float y,
            float width,
            float height
    ) {
        regionX = x;
        regionY = y;
        regionWidth = width;
        regionHeight = height;
    }

    @Override
    public void render(float alphaMult) {
        if (spriteOfImage == null) return;

        float x = Math.max(0f, Math.min(1f, regionX));
        float y = Math.max(0f, Math.min(1f, regionY));
        float width = Math.max(0f, Math.min(1f - x, regionWidth));
        float height = Math.max(0f, Math.min(1f - y, regionHeight));
        float displayWidth = componentPanel.getPosition().getWidth();
        float displayHeight = componentPanel.getPosition().getHeight();
        if (width <= 0f || height <= 0f || displayWidth <= 0f || displayHeight <= 0f) return;

        float oldTexX = spriteOfImage.getTexX();
        float oldTexY = spriteOfImage.getTexY();
        float oldTexWidth = spriteOfImage.getTexWidth();
        float oldTexHeight = spriteOfImage.getTexHeight();
        float oldWidth = spriteOfImage.getWidth();
        float oldHeight = spriteOfImage.getHeight();
        float oldCenterX = spriteOfImage.getCenterX();
        float oldCenterY = spriteOfImage.getCenterY();
        float oldAngle = spriteOfImage.getAngle();
        float oldAlpha = spriteOfImage.getAlphaMult();
        Color oldColor = spriteOfImage.getColor();

        try {
            spriteOfImage.setTexX(oldTexX + oldTexWidth * x);
            spriteOfImage.setTexY(oldTexY + oldTexHeight * (1f - y - height));
            spriteOfImage.setTexWidth(oldTexWidth * width);
            spriteOfImage.setTexHeight(oldTexHeight * height);
            spriteOfImage.setSize(displayWidth, displayHeight);
            spriteOfImage.setCenter(displayWidth / 2f, displayHeight / 2f);
            spriteOfImage.setAngle(0f);
            spriteOfImage.setColor(colorOverlay == null ? Color.WHITE : colorOverlay);
            spriteOfImage.setAlphaMult(alphaMult * this.alphaMult);
            spriteOfImage.renderAtCenter(
                    componentPanel.getPosition().getCenterX(),
                    componentPanel.getPosition().getCenterY()
            );
        } finally {
            spriteOfImage.setTexX(oldTexX);
            spriteOfImage.setTexY(oldTexY);
            spriteOfImage.setTexWidth(oldTexWidth);
            spriteOfImage.setTexHeight(oldTexHeight);
            spriteOfImage.setSize(oldWidth, oldHeight);
            spriteOfImage.setCenter(oldCenterX, oldCenterY);
            spriteOfImage.setAngle(oldAngle);
            spriteOfImage.setColor(oldColor);
            spriteOfImage.setAlphaMult(oldAlpha);
        }
    }
}
