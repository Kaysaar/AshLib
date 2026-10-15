package ashlib.data.plugins.ui.models.resizable;

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
        prepareSprite(alphaMult);

        spriteOfImage.setSize(
                sourceWidth,
                sourceHeight
        );

        spriteOfImage.renderRegionAtCenter(
                componentPanel.getPosition().getCenterX(),
                componentPanel.getPosition().getCenterY(),
                regionX,
                regionY,
                regionWidth,
                regionHeight
        );
    }
}