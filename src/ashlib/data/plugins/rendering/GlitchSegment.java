package ashlib.data.plugins.rendering;

public class GlitchSegment {
    public float x;
    public float y;
    public float width;
    public float height;
    public float life;
    public float maxLife;
    public float velocityX;

    public GlitchSegment(float x, float y, float width, float height, float life, float velocityX) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.life = life;
        this.maxLife = life;
        this.velocityX = velocityX;
    }

    public void update(float amount) {
        x += velocityX * amount;
        life -= amount;
    }

    public boolean isAlive() {
        return life > 0;
    }

    public float getAlpha() {
        return Math.max(life / maxLife, 0);
    }
}
