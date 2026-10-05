package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import lombok.Getter;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Getter
public class AimAssistant extends Module {
    public static AimAssistant INSTANCE;

    private final MultiModeSetting targets = multiMode(
            "Цели для наведения",
            List.of("Игроки", "Друзья"),
            "Игроки", "Животные", "Мобы", "Друзья"
    );
    private final CheckBox wallCheck = checkbox("Наводить за стеной", false);
    private final SliderSetting threshold = slider("Порог", 5.0F, 1.0F, 5.0F, 0.25F);
    private final CheckBox onlyWeapon = checkbox("Только с оружием", true);

    private LivingEntity currentTarget;
    private Vec3 vec3d = null;
    private long timestamp = 0L;

    public AimAssistant() {
        super("AimAssistant", "Доводит прицел до цели", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.currentTarget = null;
        this.vec3d = null;
    }

    @Override
    public void onDisable() {
        this.currentTarget = null;
        this.vec3d = null;
        super.onDisable();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;
        if (mc.player == null || mc.level == null) {
            this.currentTarget = null;
            this.vec3d = null;
            this.timestamp = 0L;
            return;
        }

        LivingEntity var2 = null;
        if (TriggerBot.INSTANCE != null && TriggerBot.INSTANCE.isEnabled()) {
            var2 = TriggerBot.INSTANCE.getTarget();
        }

        if (var2 == null) {
            var2 = this.computeLivingEntity();
        }

        if (var2 != this.currentTarget) {
            this.vec3d = null;
        }

        this.currentTarget = var2;

        if (this.checkCondition(this.currentTarget) && !mc.player.isUsingItem()) {
            if (!this.onlyWeapon.getValue() || this.isHoldingWeapon()) {
                Vec3 eye = mc.player.getEyePosition();
                Vec3 targetVec = this.computeVec3d(eye, this.currentTarget, 3.0, this.wallCheck.getValue());
                if (targetVec.equals(Vec3.ZERO)) {
                    return;
                }

                float dt = this.computefloat();
                double lerpFactor = 1.0 - Math.exp(-dt / 0.06);
                this.vec3d = this.vec3d == null ? targetVec : this.vec3d.lerp(targetVec, lerpFactor);
                float rotYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(this.vec3d.z, this.vec3d.x)) - 90.0);
                float rotPitch = (float) (-Math.toDegrees(Math.atan2(this.vec3d.y, Math.hypot(this.vec3d.x, this.vec3d.z))));
                float deltaYaw = Mth.wrapDegrees(rotYaw - mc.player.getYRot());
                float deltaPitch = rotPitch - mc.player.getXRot();

                if (Math.abs(deltaPitch) <= 13.0F
                        && Math.abs(deltaYaw) < 8.0F
                        && this.checkCondition3(mc.player.getYRot(), mc.player.getXRot(), 3.0, this.currentTarget, this.wallCheck.getValue())) {
                    deltaPitch = 0.0F;
                }

                if (Math.abs(deltaYaw) > 45.0F || Math.abs(deltaPitch) > 30.0F) {
                    return;
                }

                float totalDelta = (float) Math.hypot(deltaYaw, deltaPitch);
                if (totalDelta < 0.35F) {
                    return;
                }

                float speedVal = this.threshold.getValue() * 3.5F;
                float yawFactor = 1.0F - (float) Math.exp(-speedVal * dt);
                float pitchFactor = 1.0F - (float) Math.exp(-speedVal * 0.65F * dt);
                mc.player.setYRot(mc.player.getYRot() + deltaYaw * yawFactor);
                if (deltaPitch != 0.0F) {
                    mc.player.setXRot(Mth.clamp(mc.player.getXRot() + deltaPitch * pitchFactor, -90.0F, 90.0F));
                }
            }
        }
    }

    private float computefloat() {
        long now = System.nanoTime();
        if (this.timestamp == 0L) {
            this.timestamp = now;
            return 0.016666668F;
        } else {
            float dt = (float) (now - this.timestamp) / 1.0E9F;
            this.timestamp = now;
            return Mth.clamp(dt, 0.001F, 0.1F);
        }
    }

    private LivingEntity computeLivingEntity() {
        if (mc.player == null || mc.level == null) return null;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = Vec3.directionFromRotation(mc.player.getXRot(), mc.player.getYRot());
        LivingEntity best = null;
        double bestAngle = Double.MAX_VALUE;

        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof LivingEntity entity && checkCondition(entity) && (wallCheck.getValue() || checkCondition4(eye, entity, 4.0))) {
                Vec3 center = entity.getBoundingBox().getCenter();
                Vec3 toTarget = center.subtract(eye).normalize();
                double dot = Mth.clamp(look.dot(toTarget), -1.0, 1.0);
                double angle = Math.acos(dot);
                if (angle < bestAngle) {
                    bestAngle = angle;
                    best = entity;
                }
            }
        }
        return best;
    }

    private boolean checkCondition(LivingEntity entity) {
        if (entity == null || entity == mc.player) {
            return false;
        } else if (entity.isAlive() && !entity.isRemoved()) {
            double maxDist = 4.0 + mc.player.getDeltaMovement().length() * 3.0;
            return resolveDouble(entity) > maxDist * maxDist ? false : checkCondition2(entity);
        } else {
            return false;
        }
    }

    private boolean checkCondition2(LivingEntity entity) {
        if (entity instanceof Player player) {
            return this.targets.isEnabled("Игроки")
                    && (this.targets.isEnabled("Друзья") || !FriendManager.getInstance().isFriend(player.getName().getString()));
        } else if (entity instanceof Monster) {
            return this.targets.isEnabled("Мобы");
        } else if (entity instanceof Animal) {
            return this.targets.isEnabled("Животные");
        }
        return false;
    }

    private boolean isHoldingWeapon() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandItem();
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.MACE);
    }

    private double resolveDouble(net.minecraft.world.entity.Entity entity) {
        Vec3 eye = mc.player.getEyePosition();
        AABB box = entity.getBoundingBox();
        double cx = Mth.clamp(eye.x, box.minX, box.maxX);
        double cy = Mth.clamp(eye.y, box.minY, box.maxY);
        double cz = Mth.clamp(eye.z, box.minZ, box.maxZ);
        double dx = cx - eye.x;
        double dy = cy - eye.y;
        double dz = cz - eye.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean checkCondition3(float yaw, float pitch, double distance, net.minecraft.world.entity.Entity entity, boolean ignoreWalls) {
        if (mc.player != null && mc.level != null) {
            Vec3 eye = mc.player.getEyePosition();
            Vec3 dir = Vec3.directionFromRotation(pitch, yaw).scale(distance);
            Optional<Vec3> hit = entity.getBoundingBox().contains(eye) ? Optional.of(eye) : entity.getBoundingBox().clip(eye, eye.add(dir));
            return hit.isEmpty()
                    ? false
                    : ignoreWalls
                    || mc.level.clip(new ClipContext(eye, hit.get(), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS;
        } else {
            return false;
        }
    }

    private boolean checkCondition4(Vec3 from, LivingEntity entity, double reach) {
        AABB box = entity.getBoundingBox();
        double[] steps = new double[]{0.0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0};
        int last = steps.length - 1;
        double reachSq = reach * reach;

        for (int x = 0; x <= last; x++) {
            for (int y = 0; y <= last; y++) {
                for (int z = 0; z <= last; z++) {
                    if (x <= 0 || x >= last || y <= 0 || y >= last || z <= 0 || z >= last) {
                        Vec3 sample = new Vec3(
                                Mth.lerp(steps[x], box.minX, box.maxX),
                                Mth.lerp(steps[y], box.minY, box.maxY),
                                Mth.lerp(steps[z], box.minZ, box.maxZ)
                        );
                        double distSq = from.distanceToSqr(sample);
                        if (!(distSq > reachSq)) {
                            Vec3 rayTarget = sample.add(from.subtract(sample).scale(0.05 / Math.sqrt(distSq)));
                            if (mc.level.clip(new ClipContext(from, rayTarget, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private Vec3 computeVec3d(Vec3 eye, LivingEntity target, double reach, boolean throughWalls) {
        AABB box = target.getBoundingBox();
        double midX = (box.minX + box.maxX) * 0.5;
        double midZ = (box.minZ + box.maxZ) * 0.5;
        Vec3 eyePos = target.position().add(0.0, target.getEyeHeight(), 0.0);
        double dist = eye.distanceTo(eyePos);
        double targetY = Mth.lerp(Mth.clamp((float) (dist / 3.0), 0.0F, 1.0F), box.minY, Mth.clamp(eye.y, box.minY, box.maxY));
        Vec3 centerPoint = new Vec3(midX, targetY, midZ);
        List<Vec3> points = new ArrayList<>();
        points.add(centerPoint);
        double[] steps = new double[]{0.0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0};
        int last = steps.length - 1;

        for (int x = 0; x < steps.length; x++) {
            for (int y = 0; y < steps.length; y++) {
                for (int z = 0; z < steps.length; z++) {
                    if (x == 0 || x == last || y == 0 || y == last || z == 0 || z == last) {
                        points.add(
                                new Vec3(
                                        Mth.lerp(steps[x], box.minX, box.maxX),
                                        Mth.lerp(steps[y], box.minY, box.maxY),
                                        Mth.lerp(steps[z], box.minZ, box.maxZ)
                                )
                        );
                    }
                }
            }
        }

        for (double ext : new double[]{0.0, 0.2}) {
            List<Vec3> validPoints = new ArrayList<>();

            for (Vec3 pt : points) {
                Vec3 diff = pt.subtract(eye);
                double length = diff.length();
                if (length <= reach + ext) {
                    float reachDist = (float) (reach + ext);
                    if (this.checkCondition3(
                            (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0),
                            (float) (-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)))),
                            reachDist,
                            target,
                            false
                    )) {
                        validPoints.add(pt);
                    }
                }
            }

            if (!validPoints.isEmpty()) {
                Vec3 avg = validPoints.stream().reduce(Vec3.ZERO, Vec3::add).scale(1.0 / validPoints.size());
                return validPoints.stream().min(Comparator.comparingDouble(pt -> pt.distanceToSqr(avg))).get().subtract(eye);
            }

            if (throughWalls) {
                List<Vec3> wallPoints = new ArrayList<>();

                for (Vec3 pt : points) {
                    Vec3 diff = pt.subtract(eye);
                    double length = diff.length();
                    if (length <= reach + ext) {
                        float reachDist = (float) (reach + ext);
                        if (this.checkCondition3(
                                (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0),
                                (float) (-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)))),
                                reachDist,
                                target,
                                true
                        )) {
                            wallPoints.add(pt);
                        }
                    }
                }

                if (!wallPoints.isEmpty()) {
                    Vec3 avg = wallPoints.stream().reduce(Vec3.ZERO, Vec3::add).scale(1.0 / wallPoints.size());
                    return wallPoints.stream().min(Comparator.comparingDouble(pt -> pt.distanceToSqr(avg))).get().subtract(eye);
                }
            }
        }

        return Vec3.ZERO;
    }
}