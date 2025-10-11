package ashlib.data.plugins.ui.models.resizable;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ImageViewer extends ResizableComponent {
        public SpriteAPI spriteOfImage;
        public Color colorOverlay;
        public float alphaMult =1f;
        public void setAlphaMult(float alphaMult) {
            this.alphaMult = alphaMult;
        }
        public ImageViewer(float width, float height,String imagePath) {
            componentPanel = Global.getSettings().createCustom(width, height, this);
            spriteOfImage = Global.getSettings().getSprite(imagePath);

        }

        public void setColorOverlay(Color colorOverlay) {
            this.colorOverlay = colorOverlay;
        }

        @Override
        public void render(float alphaMult) {
            super.render(alphaMult);

            if(colorOverlay != null) {
                spriteOfImage.setColor(colorOverlay);
            }

            spriteOfImage.setAlphaMult(alphaMult*this.alphaMult);
            spriteOfImage.setSize(componentPanel.getPosition().getWidth(), componentPanel.getPosition().getHeight());
            spriteOfImage.renderAtCenter(componentPanel.getPosition().getCenterX(), componentPanel.getPosition().getCenterY());
        }
    }


