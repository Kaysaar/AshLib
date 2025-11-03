package ashlib.data.plugins.ui.models.resizable.map;

import ashlib.data.plugins.ui.models.resizable.ResizableComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class MapGridRenderer extends ResizableComponent {
    private final ResizableComponent topLeft;
    private final ResizableComponent topRight;
    private final ResizableComponent bottomLeft;
    private final ResizableComponent bottomRight;
    private final ResizableComponent center;
    /** Base grid size at scale = 1 */
    public float gridSize = 3000f;

    transient SpriteAPI spriteToRender = Global.getSettings().getSprite("rendering", "GlitchSquare");

    public MapGridRenderer(ResizableComponent topLeft, ResizableComponent topRight,
                           ResizableComponent bottomLeft, ResizableComponent bottomRight,ResizableComponent center) {
        this.componentPanel = Global.getSettings().createCustom(1,1,this);
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
        this.center = center;
    }
    public Vector2f getSizeOfMapCurrently(){
        float startX = topLeft.getComponentPanel().getPosition().getX();
        float endX = topRight.getComponentPanel().getPosition().getX();
        float topY = topLeft.getComponentPanel().getPosition().getCenterY();
        float bottomY = bottomLeft.getComponentPanel().getPosition().getCenterY();
        // Ensure correct Y order
        if (bottomY > topY) {
            float tmp = bottomY;
            bottomY = topY;
            topY = tmp;
        }

        float width = endX - startX;
        float height = topY - bottomY;
        return new Vector2f(width, height);

    }
    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);

        // Grid spacing adjusted by current scale
        float scaledGridSize = gridSize * scale;

        // Configure sprite appearance
        Color color = new Color(54, 107, 119, 180);
        spriteToRender.setColor(color);
        spriteToRender.setAlphaMult(alphaMult * 0.6f);
        spriteToRender.setNormalBlend();

        // Determine grid bounds
        float startX = topLeft.getComponentPanel().getPosition().getX();
        float endX = topRight.getComponentPanel().getPosition().getX();
        float topY = topLeft.getComponentPanel().getPosition().getCenterY();
        float bottomY = bottomLeft.getComponentPanel().getPosition().getCenterY();
        float centerX = center.getComponentPanel().getPosition().getCenterX();
        float centerY = center.getComponentPanel().getPosition().getCenterY();

        // Ensure correct Y order
        if (bottomY > topY) {
            float tmp = bottomY;
            bottomY = topY;
            topY = tmp;
        }

        float width = endX - startX;
        float height = topY - bottomY;

        // --- Horizontal grid lines (vary Y; full width) ---
        spriteToRender.setSize(width, 1f);

        // Center horizontal line
        if (centerY <= topY && centerY >= bottomY) {
            spriteToRender.render(startX, centerY);
        }

        // Upwards from centerY to topY
        for (float y = centerY + scaledGridSize; y <= topY; y += scaledGridSize) {
            spriteToRender.render(startX, y);
        }
        // Downwards from centerY to bottomY
        for (float y = centerY - scaledGridSize; y >= bottomY; y -= scaledGridSize) {
            spriteToRender.render(startX, y);
        }

        // --- Vertical grid lines (vary X; full height) ---
        spriteToRender.setSize(1f, height);

        // Center vertical line
        if (centerX >= startX && centerX <= endX) {
            spriteToRender.render(centerX, bottomY);
        }

        // Rightwards from centerX to endX
        for (float x = centerX + scaledGridSize; x <= endX; x += scaledGridSize) {
            spriteToRender.render(x, bottomY);
        }
        // Leftwards from centerX to startX
        for (float x = centerX - scaledGridSize; x >= startX; x -= scaledGridSize) {
            spriteToRender.render(x, bottomY);
        }
    }

}
