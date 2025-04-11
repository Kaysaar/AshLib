package ashlib.data.plugins.rendering;

import ashlib.data.plugins.repositories.WeaponMissileInfoRepo;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WeaponSpriteRenderer implements CustomUIPanelPlugin {

    ArrayList<SpriteAPI> spritesToRedner;
    CustomPanelAPI anchor;
    WeaponSpecAPI specWeapon;
    String idOfMissileSprite = null;
    float scale = 1f;
    public Color overlayColor;
    Vector2f originalCenterOfMissle;
    public void setAnchor(CustomPanelAPI anchor) {
        this.anchor = anchor;
    }

    public void setOverlayColor(Color overlayColor) {
        this.overlayColor = overlayColor;
    }

    public WeaponSpriteRenderer(WeaponSpecAPI spec, float iconSize, float angle) {
        this.specWeapon =spec;
        spritesToRedner = new ArrayList<>();
        spritesToRedner.add(Global.getSettings().getSprite(spec.getTurretUnderSpriteName()));
        spritesToRedner.add(Global.getSettings().getSprite(spec.getTurretSpriteName()));
        SpriteAPI baseSprite = Global.getSettings().getSprite(spec.getTurretSpriteName());
        if(spec instanceof ProjectileWeaponSpecAPI){
            spritesToRedner.add(Global.getSettings().getSprite(((ProjectileWeaponSpecAPI) spec).getTurretGunSpriteName()));
        }
        for (SpriteAPI spriteAPI : spritesToRedner) {
            spriteAPI.setAngle(angle);
            float originalWidth = spriteAPI.getWidth();
            float originalHeight = spriteAPI.getHeight();
            float newWidth, newHeight;
            float aspectRatio = originalWidth / originalHeight;
            newHeight = iconSize;
            newWidth = iconSize * aspectRatio;

            spriteAPI.setSize(newWidth, newHeight);
        }

        scale = getScale(baseSprite,iconSize);
        idOfMissileSprite = WeaponMissileInfoRepo.weapontoMissleMap.get(spec.getWeaponId());
    }
    public float getScale(SpriteAPI sprite,float iconSize){
        float originalWidth = sprite.getWidth();
        float originalHeight = sprite.getHeight();
        float newWidth, newHeight;
        float aspectRatio = originalWidth / originalHeight;
        newHeight = iconSize;
        newWidth = iconSize * aspectRatio;
        return newWidth/originalWidth;
    }
    @Override
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void renderBelow(float alphaMult) {

    }

    @Override
    public void render(float alphaMult) {
        if (anchor != null) {
            for (SpriteAPI spriteAPI : spritesToRedner) {
                if(overlayColor!=null){
                    spriteAPI.setColor(overlayColor);
                }
                spriteAPI.renderAtCenter(anchor.getPosition().getCenterX(), anchor.getPosition().getCenterY());
            }
        }
        if(idOfMissileSprite!=null){

            SpriteAPI sprite = Global.getSettings().getSprite(idOfMissileSprite);
            if(overlayColor!=null){
                sprite.setColor(overlayColor);
            }
            sprite.setSize(sprite.getWidth()*scale,sprite.getHeight()*scale);
            for (Vector2f turretFireOffset : specWeapon.getTurretFireOffsets()) {
                sprite.renderAtCenter((anchor.getPosition().getCenterX()+(turretFireOffset.getY()*scale)),anchor.getPosition().getCenterY()+(turretFireOffset.x*scale));
            }
        }



    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }


}
