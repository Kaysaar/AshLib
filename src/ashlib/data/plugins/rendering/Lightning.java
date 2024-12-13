package ashlib.data.plugins.rendering;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Lightning {
    private Color color;
    private float width;
    private float lifetime;
    private final Random random = new Random();
    private float initialDisplacement;

    public Lightning(Color color, float width, float lifetime, float displacement) {
        this.color = color;
        this.width = width;
        this.lifetime = lifetime;
        this.initialDisplacement = displacement;
    }

    private static class LightningArc {
        List<Vector2f> points;
        float life;

        LightningArc(List<Vector2f> points, float life) {
            this.points = points;
            this.life = life;
        }

        void update(float amount) {
            life -= amount;
        }

        boolean isAlive() {
            return life > 0;
        }
    }

    private final List<LightningArc> arcs = new ArrayList<>();

    private List<Vector2f> generateLightningPath(float startX, float startY, float endX, float endY,int pointsNumber) {
        List<Vector2f> points = new ArrayList<>();
        points.add(new Vector2f(startX, startY));

        float midX = (startX + endX) / 2;
        float midY = (startY + endY) / 2;
        float displacement = initialDisplacement; // Reset displacement for each path

        for (int i = 0; i < pointsNumber; i++) { // Reduced number of segments for less zigzag
            midX += (random.nextFloat() - 0.5f) * displacement;
            midY += (random.nextFloat() - 0.5f) * displacement;
            points.add(new Vector2f(midX, midY));
            displacement /= 1.5f;
        }

        points.add(new Vector2f(endX, endY));
        return points;
    }

    public void spawnLightningArc(float x, float y, float length, float angleMin, float angleMax,int points) {
        float angle = angleMin + random.nextFloat() * (angleMax - angleMin);
        float endX = x + (float) Math.cos(Math.toRadians(angle)) * length;
        float endY = y + (float) Math.sin(Math.toRadians(angle)) * length;

        List<Vector2f> path = generateLightningPath(x, y, endX, endY,points);
        arcs.add(new LightningArc(path, lifetime));
    }

    public void render(float alphaMult, SpriteAPI lightningSprite) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        lightningSprite.bindTexture();

        Iterator<LightningArc> iterator = arcs.iterator();
        while (iterator.hasNext()) {
            LightningArc arc = iterator.next();
            if (!arc.isAlive()) {
                iterator.remove();
                continue;
            }
            arc.update(0.008f);

            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, (arc.life / lifetime) + alphaMult);

            GL11.glBegin(GL11.GL_QUADS);
            for (int i = 0; i < arc.points.size() - 1; i++) {
                Vector2f p1 = arc.points.get(i);
                Vector2f p2 = arc.points.get(i + 1);

                Vector2f direction = Vector2f.sub(p2, p1, null);
                if (direction.length() > 0) { // Check for zero length
                    direction.normalise();
                    Vector2f perpendicular = new Vector2f(-direction.y, direction.x);
                    perpendicular.scale(width / 2);

                    Vector2f p1a = Vector2f.add(p1, perpendicular, null);
                    Vector2f p1b = Vector2f.sub(p1, perpendicular, null);
                    Vector2f p2a = Vector2f.add(p2, perpendicular, null);
                    Vector2f p2b = Vector2f.sub(p2, perpendicular, null);

                    GL11.glTexCoord2f(0, 0);
                    GL11.glVertex2f(p1a.x, p1a.y);
                    GL11.glTexCoord2f(1, 0);
                    GL11.glVertex2f(p2a.x, p2a.y);
                    GL11.glTexCoord2f(1, 1);
                    GL11.glVertex2f(p2b.x, p2b.y);
                    GL11.glTexCoord2f(0, 1);
                    GL11.glVertex2f(p1b.x, p1b.y);
                }
            }
            GL11.glEnd();
        }

        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
}
