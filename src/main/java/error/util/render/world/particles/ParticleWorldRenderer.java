package error.util.render.world.particles;

import error.module.impl.render.WorldParticles;

/**
 * Рендерер 3D эффектов кубиков и светлячков
 */
public final class ParticleWorldRenderer {
    private final CubeParticleRenderer cubeRenderer = new CubeParticleRenderer();
    private final FireflyRenderer fireflyRenderer = new FireflyRenderer();

    public void render(WorldParticles module, float tickDelta) {
        if (module == null || !module.isState()) {
            release();
            return;
        }

        if (module.mode.getValue().equals("Кубики")) {
            this.fireflyRenderer.release();
            this.cubeRenderer.render(module, tickDelta);
        } else if (module.mode.getValue().equals("Светлячки")) {
            this.cubeRenderer.release();
            this.fireflyRenderer.render(module);
        }
    }

    public void release() {
        this.cubeRenderer.release();
        this.fireflyRenderer.release();
    }
}