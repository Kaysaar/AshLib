package ashlib.data.plugins.coreui.campaignbar.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.awt.Color;

/** Draws arrow icons; native buttons handle input, sound and enabled state. */
final class CampaignArrowIcons extends BaseCustomUIPanelPlugin {
    static final String LEFT = "graphics/ashlib/campaignbar/arrow-left-white.png";
    static final String RIGHT = "graphics/ashlib/campaignbar/arrow-right-white.png";
    static final float WIDTH = 8, HEIGHT = 11, DISABLED_ALPHA = 0.30f;

    interface Glyph {
        void render(float x, float y, float opacity);
    }

    interface Factory {
        UIPanelAPI create(CampaignArrowIcons plugin);
    }

    final UIPanelAPI panel;
    private ButtonAPI left, right;
    private Glyph leftGlyph, rightGlyph;
    private Throwable failure;
    private boolean disposed;

    static CampaignArrowIcons create(ButtonAPI left, ButtonAPI right) throws Exception {
        // Private sprites share texture data; UV crops exclude transparent margins.
        // Crop values are source-image pixels, measured from the top-left corner.
        Glyph l = load(LEFT, 131 /* X */, 223 /* Top */, 800 /* Width */, 1103 /* Height */),
                r = load(RIGHT, 128 /* X */, 226 /* Top */, 806 /* Width */, 1086 /* Height */);
        return new CampaignArrowIcons(
                left,
                right,
                l,
                r,
                new Factory() {
                    public UIPanelAPI create(CampaignArrowIcons plugin) {
                        return Global.getSettings().createCustom(914, 35, plugin);
                    }
                });
    }

    CampaignArrowIcons(ButtonAPI left, ButtonAPI right, Glyph l, Glyph r, Factory factory) {
        if (left == null || right == null || l == null || r == null) {
            throw new IllegalArgumentException("NAV_ARROW_ICON_MISSING");
        }
        this.left = left;
        this.right = right;
        leftGlyph = l;
        rightGlyph = r;
        panel = factory.create(this);
        if (panel == null) {
            throw new IllegalStateException("NAV_ARROW_ICON_SURFACE_MISSING");
        }
    }

    private static Glyph load(String path, int x, int top, int w, int h) throws Exception {
        Global.getSettings().loadTexture(path);
        final SpriteAPI sprite = Global.getSettings().getSprite(path);
        if (sprite == null
                || sprite.getWidth() != 1060
                || sprite.getHeight() != 1484
                || sprite.getTextureId() == 0) {
            throw new IllegalStateException("NAV_ARROW_ICON_ASSET " + path);
        }
        float u = sprite.getTexX(),
                v = sprite.getTexY(),
                uw = sprite.getTexWidth(),
                vh = sprite.getTexHeight();
        sprite.setTexX(u + uw * x / 1060f);
        sprite.setTexY(v + vh * (1484 - top - h) / 1484f);
        sprite.setTexWidth(uw * w / 1060f);
        sprite.setTexHeight(vh * h / 1484f);
        sprite.setSize(WIDTH, HEIGHT);
        sprite.setColor(Color.WHITE);
        sprite.setNormalBlend();
        return new Glyph() {
            public void render(float x, float y, float opacity) {
                sprite.setAlphaMult(opacity);
                sprite.render(x, y);
            }
        };
    }

    @Override
    public void render(float opacity) {
        if (disposed || failure != null || opacity <= 0) {
            return;
        }
        try {
            draw(left, leftGlyph, opacity);
            draw(right, rightGlyph, opacity);
        } catch (Throwable error) {
            failure = error;
        } // Defer rollback to the next update.
    }

    private void draw(ButtonAPI button, Glyph glyph, float opacity) {
        PositionAPI position = button.getPosition();
        glyph.render(
                position.getX() + (position.getWidth() - WIDTH) / 2,
                position.getY() + (position.getHeight() - HEIGHT) / 2,
                opacity * button.getOpacity() * (button.isEnabled() ? 1 : DISABLED_ALPHA));
    }

    Throwable failure() {
        return failure;
    }

    void dispose() {
        disposed = true;
        left = null;
        right = null;
        leftGlyph = null;
        rightGlyph = null;
    }
}
