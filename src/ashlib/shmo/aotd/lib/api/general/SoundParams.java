package ashlib.shmo.aotd.lib.api.general;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import org.lwjgl.util.vector.Vector2f;

public final class SoundParams {
    private final String soundName;
    private final float  pitch;
    private final float  volume;

    public SoundParams(
            String soundId,
            float  pitch,
            float  volume
    ) {
        this.soundName = soundId;
        this.pitch = pitch;
        this.volume = volume;
    }

    public static SoundParams create() {
        return new SoundParams(
                "",
                1.0f,
                1.0f
        );
    }

    public static SoundParams of(String soundId) {
        return new SoundParams(
                soundId,
                1.0f,
                1.0f
        );
    }

    public static SoundParams of(String soundId, float pitch, float volume) {
        return new SoundParams(
                soundId,
                pitch,
                volume
        );
    }

    public SoundParams withSound(String soundId) {
        return new SoundParams(
                soundId,
                pitch,
                volume
        );
    }

    public SoundParams withVolume(float volume) {
        return new SoundParams(
                soundName,
                pitch,
                volume
        );
    }

    public SoundParams withPitch(float pitch) {
        return new SoundParams(
                soundName,
                pitch,
                volume
        );
    }

    public String getSoundId() {
        return soundName;
    }

    public float getPitch() {
        return pitch;
    }

    public float getVolume() {
        return volume;
    }

    public Option<SoundAPI> playSound(
            Vector2f location,
            Vector2f velocity
    ) {
        if (getSoundId().isEmpty()) {
            return Option.none();
        }

        return Option.of(Global.getSoundPlayer().playSound(
                getSoundId(),
                getPitch(),
                getVolume(),
                location,
                velocity
        ));
    }

    public void playLoop(
            Object playingEntity,
            Vector2f location,
            Vector2f velocity
    ) {
        if (getSoundId().isEmpty()) {
            return;
        }

        Global.getSoundPlayer().playLoop(
                getSoundId(),
                playingEntity,
                getPitch(),
                getVolume(),
                location,
                velocity
        );
    }

    public void playLoop(
            Object playingEntity,
            Vector2f location,
            Vector2f velocity,
            float fadeIn,
            float fadeOut
    ) {
        if (getSoundId().isEmpty()) {
            return;
        }

        Global.getSoundPlayer().playLoop(
                getSoundId(),
                playingEntity,
                getPitch(),
                getVolume(),
                location,
                velocity,
                fadeIn,
                fadeOut
        );
    }

    public Option<SoundAPI> playUISound() {
        if (getSoundId().isEmpty()) {
            return Option.none();
        }

        return Option.of(Global.getSoundPlayer().playUISound(
                getSoundId(),
                getPitch(),
                getVolume()
        ));
    }

    public void playUILoop() {
        if (getSoundId().isEmpty()) {
            return;
        }

        Global.getSoundPlayer().playUILoop(
                getSoundId(),
                getPitch(),
                getVolume()
        );
    }
}
