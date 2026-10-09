package error.util.render.world;

import error.Client;
import error.util.render.world.module.*;
import error.module.impl.combat.AuraModule;
import error.module.impl.render.*;
import error.util.client.target.TargetMarkers;
import error.util.render.world.particles.ParticleWorldRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 */
public final class MiasmWorlds {
    private static final List<AffectedWorlds> Affected = new ArrayList<>();

    private MiasmWorlds() {
    }

    public static void processed() {
        if (!Affected.isEmpty()) {
            return;
        }

        register(AffectedWorlds.lazy(
                () -> gate(Client.INSTANCE.moduleManager.getAmbience(), Ambience::usesSky),
                AmbienceRenderer::new,
                (feature, renderer, context) -> renderer.renderSky(feature, context.cameraRenderState()),
                AmbienceRenderer::release
        ));
        register(AffectedWorlds.lazy(
                () -> {HitEffect hitEffect = Client.INSTANCE.moduleManager.getModule(HitEffect.class);return (hitEffect != null && hitEffect.isEnabled() && hitEffect.hasActiveHits()) ? hitEffect : null;},
                () -> Client.INSTANCE.moduleManager.getModule(HitEffect.class).getRenderer(),
                (module, renderer, context) -> renderer.render(module, context.cameraRenderState()),
                HitEffectRenderer::release
        ));

        register(AffectedWorlds.lazy(
                () -> {PopEffect popEffect = Client.INSTANCE.moduleManager.getModule(PopEffect.class);return (popEffect != null && popEffect.isEnabled() && popEffect.hasActiveEffects()) ? popEffect : null;},
                () -> Client.INSTANCE.moduleManager.getModule(PopEffect.class).getRenderer(),
                (module, renderer, context) -> renderer.render(module, context.tickDelta()),
                r -> {}
        ));

        register(AffectedWorlds.lazy(
                () -> {
                    BlockOutline outline = Client.INSTANCE.moduleManager.getModule(BlockOutline.class);
                    if (outline != null && outline.isState()) return outline;
                    BlockHighlight highlight = Client.INSTANCE.moduleManager.getModule(BlockHighlight.class);
                    return (highlight != null && highlight.enables()) ? highlight : null;
                },
                BlockHighlightRenderer::new,
                (module, renderer, context) -> renderer.render(module, context.cameraRenderState()),
                BlockHighlightRenderer::release
        ));

        register(AffectedWorlds.lazy(
                () -> {JumpCircles jump = Client.INSTANCE.moduleManager.getModule(JumpCircles.class);return (jump != null && jump.hasActiveCircles()) ? jump : null;},
                JumpCircleRenderer::new,
                (module, renderer, context) -> renderer.render(module, context.cameraRenderState()),
                JumpCircleRenderer::release
        ));

        register(AffectedWorlds.lazy(
                () -> gate(Client.INSTANCE.moduleManager.getAmbience(), Ambience::usesPuddles),
                AmbienceRenderer::new,
                (feature, renderer, context) -> renderer.renderPuddles(feature, context.cameraRenderState()),
                AmbienceRenderer::release
        ));
        register(AffectedWorlds.lazy(
                () -> gate(Client.INSTANCE.moduleManager.getAmbience(), Ambience::usesLightning),
                AmbienceRenderer::new,
                (feature, renderer, context) -> renderer.renderLightning(feature, context.cameraRenderState()),
                AmbienceRenderer::release
        ));

        register(AffectedWorlds.lazy(
                () -> gate(Client.INSTANCE.moduleManager.getAmbience(), Ambience::usesVolumetricFog),
                AmbienceRenderer::new,
                (feature, renderer, context) -> renderer.renderVolumetricFog(feature, context.cameraRenderState()),
                AmbienceRenderer::release
        ));
        register(AffectedWorlds.lazy(
                () -> gate(Client.INSTANCE.moduleManager.getAmbience(), Ambience::usesSaturation),
                AmbienceRenderer::new,
                (feature, renderer, context) -> renderer.renderSaturation(feature),
                AmbienceRenderer::release
        ));

        register(AffectedWorlds.lazy(
                () -> {
                    error.module.impl.render.Atmosphere atmo = error.module.impl.render.Atmosphere.INSTANCE;
                    return (atmo != null && atmo.isEnabled()) ? atmo : null;
                },
                () -> error.util.render.WorldColorRenderer.getInstance(),
                (atmo, renderer, context) -> atmo.renderWorldColor(),
                r -> {}
        ));

        register(AffectedWorlds.lazy(
                () -> {
                    error.module.impl.render.TargetEsp esp = error.module.impl.render.TargetEsp.INSTANCE;
                    boolean espActive = esp != null && esp.isEnabled();
                    boolean hasVisuals = TargetMarkers.INSTANCE.hasActive();
                    return (espActive || hasVisuals) ? (esp != null ? esp : Client.INSTANCE.moduleManager.getModule(error.module.impl.render.TargetEsp.class)) : null;
                },
                () -> TargetMarkers.INSTANCE,
                (esp, markers, context) -> markers.render(esp, context.tickDelta()),
                TargetMarkers::release
        ));

        register(AffectedWorlds.lazy(
                () -> {WorldParticles particles = Client.INSTANCE.moduleManager.getWorldParticles();return (particles != null && particles.isState()) ? particles : null;},
                ParticleWorldRenderer::new,
                (module, renderer, context) -> renderer.render(module, context.tickDelta()),
                ParticleWorldRenderer::release
        ));
        register(AffectedWorlds.lazy(
                () -> {WorldParticles worldParticles = Client.INSTANCE.moduleManager.getModule(WorldParticles.class);return (worldParticles != null && worldParticles.isState()) ? worldParticles : null;},
                ParticleWorldRenderer::new,
                (module, renderer, context) -> renderer.render(module, context.tickDelta()),
                ParticleWorldRenderer::release
        ));
    }

    public static void register(AffectedWorlds effect) {
        Affected.add(effect);
    }

    public static void render(RendererWorldProvider context) {
        if (Affected.isEmpty()) {
            processed();
        }
        for (AffectedWorlds afffect : Affected) {
            if (afffect.active()) {
                afffect.render(context);
            } else {
                afffect.release();
            }
        }
    }

    private static <F> F gate(F module, java.util.function.Predicate<F> enabled) {
        return module != null && enabled.test(module) ? module : null;
    }
}