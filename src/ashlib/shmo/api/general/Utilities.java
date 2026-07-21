package ashlib.shmo.api.general;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public final class Utilities {
    @FunctionalInterface
    public interface Constructor<T> {
        T construct();
    }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    public static float inverseLerp(float value, float a, float b) { return (value - a) / (b - a); }

    public static Vector2f lerp(Vector2f a, Vector2f b, float t) {
        return new Vector2f(lerp(a.x, b.x, t), lerp(a.y, b.y, t));
    }

    public static Color lerp(Color a, Color b, float t) {
        final float aRed = (float)a.getRed() / 255.0f;
        final float aGreen = (float)a.getGreen() / 255.0f;
        final float aBlue = (float)a.getBlue() / 255.0f;
        final float aAlpha = (float)a.getAlpha() / 255.0f;

        final float bRed = (float)b.getRed() / 255.0f;
        final float bGreen = (float)b.getGreen() / 255.0f;
        final float bBlue = (float)b.getBlue() / 255.0f;
        final float bAlpha = (float)b.getAlpha() / 255.0f;

        final float cRed = lerp(aRed, bRed, t);
        final float cGreen = lerp(aGreen, bGreen, t);
        final float cBlue = lerp(aBlue, bBlue, t);
        final float cAlpha = lerp(aAlpha, bAlpha, t);

        return new Color(
                (int) (cRed * 255),
                (int) (cGreen * 255),
                (int) (cBlue * 255),
                (int) (cAlpha * 255)
        );
    }

    public static float smoothstep(float value) {
        final float t = Math.max(0f, Math.min(1f, value));
        return t * t * (3f - 2f * t);
    }

    public static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    public static float clampN11(float v) {
        return Math.max(-1.0f, Math.min(1.0f, v));
    }


    public static Color multiply(Color a, Color b) {
        return new Color(
                (a.getRed()   * b.getRed())   / 255,
                (a.getGreen() * b.getGreen()) / 255,
                (a.getBlue()  * b.getBlue())  / 255,
                (a.getAlpha() * b.getAlpha()) / 255
        );
    }

    public static <K, T> T getOrInsert(Map<K, T> map, K key, Constructor<T> constructor) {
        return Option.of(map.get(key)).match(
                (v) -> v,
                () -> {
                    var value = constructor.construct();
                    map.put(key, value);
                    return value;
                }
        );
    }


    private static long firstTimeStamp = -1;
    public static long getFirstTimeStamp() {
        if (firstTimeStamp == -1) {
            firstTimeStamp = Global.getSector().getClock().getTimestamp();
        }
        return firstTimeStamp;
    }

    public static float getTotalTimeElapsed() {
        return Global.getSector().getClock().convertToSeconds(Global.getSector().getClock().getElapsedDaysSince(getFirstTimeStamp()));
    }

    public static float[][] generatePerlinNoise(float[][] random, int octaves, float persistence) {
        int height = random.length;
        int width = random[0].length;
        float[][] result = new float[height][width];

        int usableOctaves = maxSeamlessOctaves(width, height, octaves);

        float maxAmplitude = 0f;
        float amplitude = 1f;

        for (int o = usableOctaves - 1; o >= 0; o--) {
            int frequency = 1 << o;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    result[y][x] += sampleSmooth(random, x, y, frequency, width, height) * amplitude;
                }
            }
            maxAmplitude += amplitude;
            amplitude *= persistence;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] /= maxAmplitude;
            }
        }

        return result;
    }

    private static int maxSeamlessOctaves(int width, int height, int requested) {
        int usable = 0;
        while (usable < requested) {
            int frequency = 1 << usable;
            if (width % frequency != 0 || height % frequency != 0) {
                break;
            }
            usable++;
        }
        return Math.max(usable, 1);
    }

    public static float[][] normalizeToUnitRange(float[][] noise) {
        int height = noise.length;
        int width = noise[0].length;

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;

        for (float[] floats : noise) {
            for (int x = 0; x < width; x++) {
                float v = floats[x];
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }

        float[][] result = new float[height][width];
        float range = max - min;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = (range == 0f) ? 0f : (noise[y][x] - min) / range;
            }
        }

        return result;
    }

    private static float sampleSmooth(float[][] random, int x, int y, int frequency, int width, int height) {
        float sampleX = (float) x / frequency;
        float sampleY = (float) y / frequency;

        int x0 = (int) sampleX;
        int y0 = (int) sampleY;
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        float fracX = sampleX - x0;
        float fracY = sampleY - y0;

        float v00 = getValue(random, x0 * frequency, y0 * frequency, width, height);
        float v10 = getValue(random, x1 * frequency, y0 * frequency, width, height);
        float v01 = getValue(random, x0 * frequency, y1 * frequency, width, height);
        float v11 = getValue(random, x1 * frequency, y1 * frequency, width, height);

        float smoothX = smoothstep(fracX);
        float smoothY = smoothstep(fracY);

        float top = lerp(v00, v10, smoothX);
        float bottom = lerp(v01, v11, smoothX);

        return lerp(top, bottom, smoothY);
    }

    private static float getValue(float[][] array, int x, int y, int width, int height) {
        x = Math.floorMod(x, width);
        y = Math.floorMod(y, height);
        return array[y][x];
    }

    private static float[][] noise = null;
    public static float sampleNoise(float x, float y) {
        final int width  = 256;
        final int height = 256;

        Random random = new Random();
        if (noise == null) {
            noise = new float[width][height];
            for (float[] array : noise) {
                for (int i = 0; i < height; i++) {
                    array[i] = random.nextFloat();
                }
            }
            noise = generatePerlinNoise(noise, 4, 0.25f);
            noise = normalizeToUnitRange(noise);
        }

        final float u = ((x % 1.0f) + 1.0f) % 1.0f;
        final float v = ((y % 1.0f) + 1.0f) % 1.0f;

        final float gx = u * width;
        final float gy = v * height;

        final int x0 = (int) gx;
        final int y0 = (int) gy;
        return sampleSmooth(noise, x0, y0, 1, width, height);
    }

    private static class PinchVertex {
        float x, y, width, t;
        PinchVertex(float x, float y, float width, float t) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.t = t;
        }
    }

    public static void renderPinchedShape(
            SpriteAPI texture,
            ViewportAPI viewport,
            float startX,
            float startY,
            float startWidth,
            float endX,
            float endY,
            float endWidth,
            float pinchExponent,
            float normalizedPinchOffset,
            float textureScrollOffset,
            Color tint,
            float alphaMult,
            boolean additiveBlend,
            float turbulenceAmount,
            float turbulenceOffset,
            float fadeFraction
    ) {
        textureScrollOffset = 1.0f - (textureScrollOffset % 1.0f);
        turbulenceOffset    = 1.0f - (turbulenceOffset % 1.0f);

        if (startX == endX && startY == endY) {
            return;
        }

        final float clampedFadeFraction = Math.max(0f, Math.min(0.5f, fadeFraction));

        Vector2f forward = new Vector2f(endX - startX, endY - startY);
        forward.normalise();
        Vector2f up = Misc.rotateAroundOrigin(forward,  90f);

        final int pointCount = 256;
        final List<PinchVertex> points = new ArrayList<>();
        for (int i = 0; i < pointCount; i++) {
            final float normalizedOffset = (float) i / (float) (pointCount - 1);

            float samplePosition = normalizedOffset + turbulenceOffset;
            final float pNoise = (0.5f - sampleNoise(
                    samplePosition,
                    0
            )) * turbulenceAmount;

            final float wNoise = (0.5f - sampleNoise(
                    0,
                    samplePosition
            )) * turbulenceAmount;

            if (i == 0) {
                points.add(new PinchVertex(startX, startY, startWidth, 0f));
            } else if (i == pointCount - 1) {
                points.add(new PinchVertex(endX, endY, endWidth, 1f));
            } else if (i == 1) {
                final float x = lerp(startX, endX, normalizedOffset);
                final float y = lerp(startY, endY, normalizedOffset);

                float width;
                if (normalizedOffset <= normalizedPinchOffset) {
                    final float distanceToPinch = inverseLerp(normalizedOffset, 0.0f, normalizedPinchOffset);
                    width = lerp(startWidth, endWidth, 1.0f - ((float) Math.pow(1.0f - distanceToPinch, pinchExponent)));
                } else {
                    width = endWidth;
                }

                points.add(new PinchVertex(x, y, width, normalizedOffset));
            } else {
                float x = lerp(startX, endX, normalizedOffset);
                float y = lerp(startY, endY, normalizedOffset);
                x += up.x * pNoise;
                y += up.y * pNoise;

                float width;
                if (normalizedOffset <= normalizedPinchOffset) {
                    final float distanceToPinch = inverseLerp(normalizedOffset, 0.0f, normalizedPinchOffset);
                    width = lerp(startWidth, endWidth, 1.0f - ((float) Math.pow(1.0f - distanceToPinch, pinchExponent)));
                } else {
                    width = endWidth;
                }
                width += wNoise;

                points.add(new PinchVertex(x, y, width, normalizedOffset));
            }
        }

        float dx = endX - startX;
        float dy = endY - startY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.0001f) len = 0.0001f;
        final float nx = -dy / len;
        final float ny =  dx / len;

        final float alpha = viewport.getAlphaMult() * alphaMult;
        final int r = tint.getRed();
        final int g = tint.getGreen();
        final int b = tint.getBlue();
        final int baseA = (int)(tint.getAlpha() * alpha);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_CURRENT_BIT);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        if (additiveBlend) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        } else {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        texture.bindTexture();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glBegin(GL11.GL_QUADS);

        for (int i = 0; i < points.size() - 1; i++) {
            final PinchVertex p0 = points.get(i);
            final PinchVertex p1 = points.get(i + 1);

            final float hw0 = p0.width * 0.5f;
            final float hw1 = p1.width * 0.5f;

            final float v0 = p0.t + textureScrollOffset;
            final float v1 = p1.t + textureScrollOffset;

            final float p0lx = p0.x + nx * hw0, p0ly = p0.y + ny * hw0;
            final float p0rx = p0.x - nx * hw0, p0ry = p0.y - ny * hw0;
            final float p1lx = p1.x + nx * hw1, p1ly = p1.y + ny * hw1;
            final float p1rx = p1.x - nx * hw1, p1ry = p1.y - ny * hw1;

            final int a0 = (int)(baseA * computeFadeMultiplier(p0.t, clampedFadeFraction));
            final int a1 = (int)(baseA * computeFadeMultiplier(p1.t, clampedFadeFraction));

            GL11.glColor4ub((byte) r, (byte) g, (byte) b, (byte) a0);
            GL11.glTexCoord2f(v0, 0f); GL11.glVertex2f(p0lx, p0ly);
            GL11.glTexCoord2f(v0, 1f); GL11.glVertex2f(p0rx, p0ry);

            GL11.glColor4ub((byte) r, (byte) g, (byte) b, (byte) a1);
            GL11.glTexCoord2f(v1, 1f); GL11.glVertex2f(p1rx, p1ry);
            GL11.glTexCoord2f(v1, 0f); GL11.glVertex2f(p1lx, p1ly);
        }

        GL11.glEnd();
        GL11.glPopAttrib();
    }

    public static void renderPinchedShape(
            SpriteAPI texture,
            ViewportAPI viewport,
            Vector2f startLocation,
            float startWidth,
            Vector2f endLocation,
            float endWidth,
            float pinchExponent,
            float normalizedPinchOffset,
            float textureScrollOffset,
            Color tint,
            float alphaMult,
            boolean additiveBlend,
            float turbulenceAmount,
            float turbulenceOffset,
            float fadeFraction
    ) {
        renderPinchedShape(
                texture,
                viewport,
                startLocation.x,
                startLocation.y,
                startWidth,
                endLocation.x,
                endLocation.y,
                endWidth,
                pinchExponent,
                normalizedPinchOffset,
                textureScrollOffset,
                tint,
                alphaMult,
                additiveBlend,
                turbulenceAmount,
                turbulenceOffset,
                fadeFraction
        );
    }

    private static class ArcVertex {
        float ix, iy, ox, oy, t;
        ArcVertex(float ix, float iy, float ox, float oy, float t) {
            this.ix = ix;
            this.iy = iy;
            this.ox = ox;
            this.oy = oy;
            this.t = t;
        }
    }

    public static void renderArc(
            SpriteAPI texture,
            ViewportAPI viewport,
            float centerX,
            float centerY,
            float innerRadius,
            float outerRadius,
            float centerAngleDeg,
            float arcLengthDeg,
            float textureScrollOffset,
            Color tint,
            float alphaMult,
            boolean additiveBlend,
            float turbulenceAmount,
            float turbulenceOffset,
            float fadeFraction,
            float taperFraction
    ) {
        if (arcLengthDeg == 0f) {
            return;
        }

        textureScrollOffset = 1.0f - (textureScrollOffset % 1.0f);
        turbulenceOffset    = 1.0f - (turbulenceOffset % 1.0f);

        final float clampedFadeFraction = Math.max(0f, Math.min(0.5f, fadeFraction));
        final float clampedTaperFraction = Math.max(0f, Math.min(0.5f, taperFraction));

        final float startAngleDeg = centerAngleDeg - arcLengthDeg * 0.5f;
        final float endAngleDeg   = centerAngleDeg + arcLengthDeg * 0.5f;

        final float fullWidth  = outerRadius - innerRadius;

        final int pointCount = 256;
        final List<ArcVertex> points = new ArrayList<>();
        for (int i = 0; i < pointCount; i++) {
            final float t = (float) i / (float) (pointCount - 1);
            final float angleDeg = lerp(startAngleDeg, endAngleDeg, t);
            final float angleRad = (float) Math.toRadians(angleDeg);
            final float radialX = (float) Math.cos(angleRad);
            final float radialY = (float) Math.sin(angleRad);

            float radiusNoise = 0f;
            float widthNoise = 0f;
            if (i != 0 && i != pointCount - 1) {
                final float samplePosition = t + turbulenceOffset;

                radiusNoise = (0.5f - sampleNoise(
                        samplePosition,
                        0
                )) * turbulenceAmount;

                widthNoise = (0.5f - sampleNoise(
                        0,
                        samplePosition
                )) * turbulenceAmount;
            }

            final float taper = (clampedTaperFraction <= 0f)
                    ? 1f
                    : computePinchFactor(t, clampedTaperFraction);

            final float width = Math.max(0f, fullWidth * taper + widthNoise);

            final float innerR = innerRadius + radiusNoise;
            final float outerR = innerR + width;

            final float innerX = centerX + radialX * innerR;
            final float innerY = centerY + radialY * innerR;
            final float outerX = centerX + radialX * outerR;
            final float outerY = centerY + radialY * outerR;

            points.add(new ArcVertex(innerX, innerY, outerX, outerY, t));
        }
        final float alpha = viewport.getAlphaMult() * alphaMult;
        final int r = tint.getRed();
        final int g = tint.getGreen();
        final int b = tint.getBlue();
        final int baseA = (int) (tint.getAlpha() * alpha);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_CURRENT_BIT);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        if (additiveBlend) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        } else {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        texture.bindTexture();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        for (final ArcVertex p : points) {
            final float u = p.t + textureScrollOffset;
            final int a = (int) (baseA * computeFadeMultiplier(p.t, clampedFadeFraction));

            GL11.glColor4ub((byte) r, (byte) g, (byte) b, (byte) a);
            GL11.glTexCoord2f(u, 1f); GL11.glVertex2f(p.ox, p.oy);
            GL11.glTexCoord2f(u, 0f); GL11.glVertex2f(p.ix, p.iy);
        }

        GL11.glEnd();
        GL11.glPopAttrib();
    }

    private static float computePinchFactor(float t, float pinchFraction) {
        float factor = 1f;

        if (t < pinchFraction) {
            factor = t / pinchFraction;
        } else if (t > 1f - pinchFraction) {
            factor = (1f - t) / pinchFraction;
        }

        factor = Math.max(0f, Math.min(1f, factor));

        return smoothstep(factor);
    }

    private static float computeFadeMultiplier(float t, float fadeFraction) {
        float mult = 1f;

        if (fadeFraction > 0f) {
            mult = Math.min(mult, smoothstep(t / fadeFraction));
            mult = Math.min(mult, smoothstep((1f - t) / fadeFraction));
        }

        return mult;
    }

}
