package ashlib.shmo.aotd.lib.api.general;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;

import java.awt.*;

public final class SavableSprite implements SpriteAPI {
    private String              spriteName;
    private transient SpriteAPI cachedSprite = null;

    private float centerX   = 0.0f;
    private float centerY   = 0.0f;
    private float width     = 0.0f;
    private float height    = 0.0f;
    private float texX      = 0.0f;
    private float texY      = 0.0f;
    private float texWidth  = 0.0f;
    private float texHeight = 0.0f;
    private Color color     = Color.WHITE;
    private float alpha     = 1.0f;
    private float angle     = 0.0f;

    public SavableSprite() {
        spriteName = "";
    }

    public SavableSprite(String spriteName) {
        changeSprite(spriteName);
    }

    public static SavableSprite create() { return new SavableSprite(); }

    public static SavableSprite create(String spriteName) {
        return new SavableSprite(spriteName);
    }

    public static SavableSprite create(String spriteCategory, String spriteId) {
        return new SavableSprite(Global.getSettings().getSpriteName(spriteCategory, spriteId));
    }

    public void changeSprite(String spriteName) {
        this.spriteName = spriteName;
        if (spriteName.isEmpty()) { return; }

        cachedSprite = Global.getSettings().getSprite(spriteName);

        if (isInvalid()) { return; }

        centerX   = cachedSprite.getCenterX();
        centerY   = cachedSprite.getCenterY();
        width     = cachedSprite.getWidth();
        height    = cachedSprite.getHeight();
        texX      = cachedSprite.getTexX();
        texY      = cachedSprite.getTexY();
        texWidth  = cachedSprite.getTexWidth();
        texHeight = cachedSprite.getTexHeight();
        color     = cachedSprite.getColor();
        alpha     = cachedSprite.getAlphaMult();
        angle     = cachedSprite.getAngle();
    }

    public void changeSprite(String spriteCategory, String spriteId) {
        changeSprite(Global.getSettings().getSpriteName(spriteCategory, spriteId));
    }

    public void tryFetchSprite() {
        if (isValid()) { return; }
        if (spriteName.isEmpty()) { return; }

        cachedSprite = Global.getSettings().getSprite(spriteName);
        if (isInvalid()) { return; }

        cachedSprite.setCenter(centerX, centerY);
        cachedSprite.setSize(width, height);
        cachedSprite.setTexX(texX);
        cachedSprite.setTexY(texY);
        cachedSprite.setTexWidth(texWidth);
        cachedSprite.setTexHeight(texHeight);
        cachedSprite.setColor(color);
        cachedSprite.setAlphaMult(alpha);
        cachedSprite.setAngle(angle);
    }

    public boolean isValid() {
        return cachedSprite != null;
    }

    public boolean isInvalid() {
        return !isValid();
    }

    @Override
    public void setBlendFunc(int src, int dest) {
        tryFetchSprite();
        if (isValid()) {
            cachedSprite.setBlendFunc(src, dest);
        }
    }

    @Override
    public void setNormalBlend() {
        tryFetchSprite();
        if (isValid()) {
            cachedSprite.setNormalBlend();
        }
    }

    @Override
    public void setAdditiveBlend() {
        tryFetchSprite();
        if (isValid()) {
            cachedSprite.setAdditiveBlend();
        }
    }

    @Override
    public void setCenter(float x, float y) {
        setCenterX(x);
        setCenterY(y);
    }

    @Override
    public void setSize(float width, float height) {
        setWidth(width);
        setHeight(height);
    }

    @Override
    public float getAngle() {
        return angle;
    }

    @Override
    public void setAngle(float angle) {
        this.angle = angle;
        if (isValid()) {
            cachedSprite.setAngle(angle);
        } else {
            tryFetchSprite();
        }
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
        if (isValid()) {
            cachedSprite.setColor(color);
        } else {
            tryFetchSprite();
        }
    }

    @Override
    public void setHeight(float height) {
        this.height = height;
        if (isValid()) {
            cachedSprite.setHeight(height);
        } else {
            tryFetchSprite();
        }
    }

    @Override
    public void setWidth(float width) {
        this.width = width;
        if (isValid()) {
            cachedSprite.setWidth(width);
        } else {
            tryFetchSprite();
        }
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public void bindTexture() {
        tryFetchSprite();
        if (isValid()) {
            cachedSprite.bindTexture();
        }
    }

    @Override
    public int getTextureId() {
        tryFetchSprite();
        if (isValid()) {
            return cachedSprite.getTextureId();
        } else {
            return 0;
        }
    }

    @Override
    public void renderAtCenter(float x, float y) {
        tryFetchSprite();
        if (isInvalid()) { return; }
        cachedSprite.renderAtCenter(x, y);
    }

    @Override
    public void render(float x, float y) {
        tryFetchSprite();
        if (isInvalid()) { return; }
        cachedSprite.render(x, y);
    }

    @Override
    public void renderRegionAtCenter(float x, float y, float tx, float ty, float tw, float th) {
        tryFetchSprite();
        if (isInvalid()) { return; }
        cachedSprite.renderRegionAtCenter(x, y, tx, ty, tw, th);
    }

    @Override
    public void renderRegion(float x, float y, float tx, float ty, float tw, float th) {
        tryFetchSprite();
        if (isInvalid()) { return; }
        cachedSprite.renderRegion(x, y, tx, ty, tw, th);
    }

    @Override
    public float getCenterX() {
        return centerX;
    }

    @Override
    public float getCenterY() {
        return centerY;
    }

    @Override
    public float getAlphaMult() {
        return alpha;
    }

    @Override
    public void setAlphaMult(float alphaMult) {
        alpha = alphaMult;
        if (isValid()) {
            cachedSprite.setAlphaMult(alphaMult);
        } else {
            tryFetchSprite();
        }
    }

    @Override
    public float getTextureWidth() {
        return texWidth;
    }

    @Override
    public float getTextureHeight() {
        return texHeight;
    }

    @Override
    public void setCenterY(float cy) {
        centerY = cy;
        if (isValid()) {
            cachedSprite.setCenterY(cy);
        } else {
            tryFetchSprite();
        }
    }

    @Override
    public void setCenterX(float cx) {
        centerX = cx;
        if (isValid()) {
            cachedSprite.setCenterX(cx);
        } else {
            tryFetchSprite();
        }
    }

    @Override
    public Color getAverageColor() {
        tryFetchSprite();
        if (isValid()) {
            return cachedSprite.getAverageColor();
        } else {
            return Color.WHITE;
        }
    }

    @Override
    public void setTexX(float texX) {
        this.texX = texX;
        if (isValid()) {
            cachedSprite.setTexX(texX);
        } else {
            tryFetchSprite();
        }
    }

    @Override
    public void setTexY(float texY) {
        this.texY = texY;
        if (isValid()) {
            cachedSprite.setTexY(texY);
        } else {
            tryFetchSprite();
        }
    }

    @Override
    public void setTexWidth(float texWidth) {
        this.texWidth = texWidth;
        if (isValid()) {
            cachedSprite.setTexWidth(texWidth);
        } else {
            tryFetchSprite();
        }
    }

    @Override
    public void setTexHeight(float texHeight) {
        this.texHeight = texHeight;
        if (isValid()) {
            cachedSprite.setTexHeight(texHeight);
        } else {
            tryFetchSprite();
        }
    }

    @Override
    public void renderWithCorners(float blX, float blY, float tlX, float tlY, float trX, float trY, float brX, float brY) {
        tryFetchSprite();
        if (isInvalid()) { return; }
        cachedSprite.renderWithCorners(blX, blY, tlX, tlY, trX, trY, brX, brY);
    }

    @Override
    public Color getAverageBrightColor() {
        tryFetchSprite();
        if (isValid()) {
            return cachedSprite.getAverageBrightColor();
        } else {
            return Color.WHITE;
        }
    }

    @Override
    public void renderNoBind(float x, float y) {
        tryFetchSprite();
        if (isInvalid()) { return; }
        cachedSprite.renderNoBind(x, y);
    }

    @Override
    public void renderAtCenterNoBind(float x, float y) {
        tryFetchSprite();
        if (isInvalid()) { return; }
        cachedSprite.renderAtCenterNoBind(x, y);
    }

    @Override
    public int getBlendDest() {
        tryFetchSprite();
        if (isValid()) {
            return cachedSprite.getBlendDest();
        } else {
            return 0;
        }
    }

    @Override
    public int getBlendSrc() {
        if (isValid()) {
            return cachedSprite.getBlendSrc();
        } else {
            return 0;
        }
    }

    @Override
    public float getTexX() {
        return texX;
    }

    @Override
    public float getTexY() {
        return texY;
    }

    @Override
    public float getTexWidth() {
        return texWidth;
    }

    @Override
    public float getTexHeight() {
        return texHeight;
    }
}
