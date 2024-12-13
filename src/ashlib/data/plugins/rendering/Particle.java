package ashlib.data.plugins.rendering;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class Particle {
    public Vector2f position;
    public Vector2f velocity;
    public float life;
    public float maxLife;

    public Color color;  // Color of the particle
    public transient SpriteAPI particleSprite;
    public Particle(Vector2f position, Vector2f velocity, float life, Color color,SpriteAPI particleSprite) {
        this.position = position;
        this.velocity = velocity;
        this.life = life;
        this.maxLife= life;
        this.color = color;
        this.particleSprite = particleSprite;
    }

    public void update(float amount) {
        position.x += velocity.x * amount;
        position.y += velocity.y * amount;
        life -= amount;
    }

    public boolean isAlive() {
        return life > 0;
    }
    public float getAlpha(float divider){
        float lifezeroone = maxLife*0.2f;
        float lifePassed = maxLife - life;
        if(lifePassed<=lifezeroone){
            float progress = lifePassed/lifezeroone;
            if(progress>=1)progress=1f;
            return (life/divider)*progress;
        }
        return life/divider;
    }
}