package ashlib.data.plugins.rendering;

public class AlphaUtil {
    private float currentAlpha;
    private boolean reverse = false;
    private float speed;
    private float minAlpha;
    private float maxAlpha;
    private boolean heartbeatMode;
    private int heartbeatPhase = 0;
    private float pauseDuration;
    private float pauseTimer = 0f;

    public AlphaUtil(float initialAlpha, float minAlphaValue, float maxAlphaValue, float alphaSpeed) {
        this(initialAlpha, minAlphaValue, maxAlphaValue, alphaSpeed, false, 0f);
    }

    public AlphaUtil(float initialAlpha, float minAlphaValue, float maxAlphaValue, float alphaSpeed, boolean heartbeatMode, float pauseDuration) {
        this.currentAlpha = initialAlpha;
        this.minAlpha = minAlphaValue;
        this.maxAlpha = maxAlphaValue;
        this.speed = alphaSpeed;
        this.heartbeatMode = heartbeatMode;
        this.pauseDuration = pauseDuration;
    }

    public float getAlphaMult() {
        if (!heartbeatMode) {
            // Normal pulsing logic
            if (!reverse) {
                currentAlpha += speed;
            }
            if (currentAlpha >= maxAlpha) {
                reverse = true;
            }
            if (reverse) {
                currentAlpha -= speed;
            }
            if (currentAlpha <= minAlpha) {
                reverse = false;
            }
        } else {
            // Heartbeat mode logic with pause
            switch (heartbeatPhase) {
                case 0:
                    pauseTimer += speed;
                    if (pauseTimer >= pauseDuration) {
                        heartbeatPhase = 1;
                        pauseTimer = 0f;
                    }
                    break;
                case 1:
                    currentAlpha += speed * 4f;
                    if (currentAlpha >= maxAlpha) {
                        heartbeatPhase = 2;
                    }
                    break;
                case 2:
                    currentAlpha -= speed *  4;
                    if (currentAlpha <= minAlpha) {
                        heartbeatPhase = 3;
                    }
                    break;
                case 3:
                    currentAlpha += speed *  4; // Rapidly grow (second pulse)
                    if (currentAlpha >= maxAlpha) {
                        heartbeatPhase = 4;
                    }
                    break;
                case 4:
                    currentAlpha -= speed *  4;
                    if (currentAlpha <= minAlpha) {
                        heartbeatPhase = 0;
                    }
                    break;
            }
        }
        return currentAlpha;
    }
}
