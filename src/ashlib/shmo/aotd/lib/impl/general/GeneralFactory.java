package ashlib.shmo.aotd.lib.impl.general;

import org.lwjgl.util.vector.Vector2f;
import ashlib.shmo.aotd.lib.api.general.ParticleParams;
import ashlib.shmo.aotd.lib.api.general.ParticleSystem;

public class GeneralFactory {
    public ParticleSystem createParticleSystem() {
        return CPUParticleSystem.create(ParticleParams.create());
    }

    public ParticleSystem createParticleSystem(ParticleParams params) {
        return CPUParticleSystem.create(params);
    }

    public ParticleSystem createParticleSystemAtLocation(ParticleParams params, float x, float y) {
        ParticleSystem particleSystem = createParticleSystem(params);
        particleSystem.setEmitterLocation(x, y);
        return particleSystem;
    }

    public ParticleSystem createParticleSystemAtLocation(ParticleParams params, Vector2f location) {
        return createParticleSystemAtLocation(params, location.x, location.y);
    }
}
