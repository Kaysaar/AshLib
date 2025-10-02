package ashlib.data.plugins.rendering;

import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.repositories.WeaponMissileInfoRepo;
import com.fs.graphics.Sprite;
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

    ArrayList<String> spritesToRedner;
    CustomPanelAPI anchor;
    WeaponSpecAPI specWeapon;
    String idOfMissileSprite = null;
    float scale = 1f;
    public Color overlayColor;
    Vector2f originalCenterOfMissle;
    public void setAnchor(CustomPanelAPI anchor) {
        this.anchor = anchor;
    }
    float angle;
    float iconSize;
    public void setOverlayColor(Color overlayColor) {
        this.overlayColor = overlayColor;
    }

    public WeaponSpriteRenderer(WeaponSpecAPI spec, float iconSize, float angle) {
        this.specWeapon =spec;
        spritesToRedner = new ArrayList<>();
        spritesToRedner.add(spec.getTurretUnderSpriteName());
        spritesToRedner.add(spec.getTurretSpriteName());
        SpriteAPI baseSprite = Global.getSettings().getSprite(spec.getTurretSpriteName());
        if(spec instanceof ProjectileWeaponSpecAPI){
            spritesToRedner.add(((ProjectileWeaponSpecAPI) spec).getTurretGunSpriteName());
        }
        this.angle = angle;
        this.iconSize = iconSize;
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
            for (String string : spritesToRedner) {
                if(!AshMisc.isStringValid(string))continue;
                SpriteAPI sprite = Global.getSettings().getSprite(string);
                sprite.setAngle(angle);
                float originalWidth = sprite.getWidth();
                float originalHeight = sprite.getHeight();
                float newWidth, newHeight;
                float aspectRatio = originalWidth / originalHeight;
                newHeight = iconSize;
                newWidth = iconSize * aspectRatio;
                sprite.setNormalBlend();
                sprite.setSize(newWidth, newHeight);


                if(overlayColor!=null){
                    sprite.setColor(overlayColor);
                }
                sprite.setAlphaMult(alphaMult);
                sprite.renderAtCenter(anchor.getPosition().getCenterX(), anchor.getPosition().getCenterY());
            }

        }
        if(AshMisc.isStringValid(idOfMissileSprite)){

            SpriteAPI sprite = Global.getSettings().getSprite(idOfMissileSprite);
            if(overlayColor!=null){
                sprite.setColor(overlayColor);
            }
            sprite.setAlphaMult(alphaMult);
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
