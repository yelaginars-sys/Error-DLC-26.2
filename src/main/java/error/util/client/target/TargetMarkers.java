package error.util.client.target;

import net.minecraft.world.entity.LivingEntity;
import error.module.impl.combat.AuraModule;
import error.module.impl.render.TargetEsp;
import error.util.client.clients.Theme;
import error.util.render.TargetOrbsRenderer;

public final class TargetMarkers {
    public static final TargetMarkers INSTANCE = new TargetMarkers();

    private final AuraMarkerRenderer marker = new AuraMarkerRenderer();
    private final GhostTargetRenderer ghost = new GhostTargetRenderer();
    private final CircleTargetRenderer circle = new CircleTargetRenderer();
    private final CubesTargetRenderer cubes = new CubesTargetRenderer();
    private final TargetOrbsRenderer orbs = new TargetOrbsRenderer();

    public void render(TargetEsp esp, float tickDelta) {
        if (esp == null || !esp.isEnabled()) {
            this.marker.render(null, tickDelta, 0);
            this.ghost.render(null, tickDelta, 0);
            this.circle.render(null, tickDelta, 0);
            this.cubes.render(null, tickDelta, 0);
            this.orbs.reset();
            return;
        }

        LivingEntity target = esp.getTarget();
        int color = esp.getEspColor();
        String mode = esp.mode.getValue();

        boolean isMarker = "Ромб".equals(mode);
        boolean isCircle = "Кружок".equals(mode);
        boolean isCubes = "Crystal".equals(mode);
        boolean isGhosts = "Призраки".equals(mode);
        boolean isGhosts2 = "Призраки 2".equals(mode);

        this.marker.render(isMarker ? target : null, tickDelta, color);
        this.ghost.render(isGhosts ? target : null, tickDelta, color);
        this.circle.render(isCircle ? target : null, tickDelta, color);
        this.cubes.render(isCubes ? target : null, tickDelta, color);

        if (isGhosts2 && target != null) {
            float speed = esp.rotSpeed.get();
            float baseSize = esp.size.get() * 16.0F;
            float alpha = esp.opacity.get();
            boolean hit = esp.colorOnHit.getValue();
            this.orbs.renderSouls(target, alpha, tickDelta, 4, baseSize, 0.85F * esp.size.get(), speed, false, hit, 0xFFFF3333, "Кастом", color);
        } else {
            this.orbs.reset();
        }
    }

    public void render(AuraModule module, float tickDelta) {
        TargetEsp esp = TargetEsp.INSTANCE;
        if (esp != null && esp.isEnabled()) {
            render(esp, tickDelta);
        } else {
            LivingEntity target = (module != null && module.isState()) ? module.getTarget() : null;
            int color = Theme.getAccentColor();
            this.marker.render(target, tickDelta, color);
        }
    }

    public boolean hasActive() {
        TargetEsp esp = TargetEsp.INSTANCE;
        if (esp != null && esp.isEnabled() && esp.getTarget() != null) return true;
        return this.marker.hasActive() || this.ghost.hasActive() || this.circle.hasActive() || this.cubes.hasActive();
    }

    public void release() {
        this.marker.release();
        this.ghost.reset();
        this.circle.reset();
        this.cubes.release();
        this.orbs.reset();
    }
}