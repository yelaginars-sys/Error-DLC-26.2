package error.module.impl.combat;

import lombok.Getter;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import error.event.EventTarget;
import error.event.list.LookEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import error.friend.FriendManager;

import java.util.Comparator;
import java.util.stream.StreamSupport;

/**
 */
@Getter
public class AimAssistant extends Module {
    public static AimAssistant INSTANCE;

    private final MultiModeSetting targets = multiMode("Targets", "Players", "Mobs", "Animals");
    private final CheckBox wallCheck = checkbox("Check Wall", true);
    private final SliderSetting speed = slider("Speed", 5.0f, 1.0f, 10.0f, 0.25f);
    private final CheckBox onlyWeapon = checkbox("Only Weapon", true);

    private LivingEntity target;
    private Vec3 aimVector;

    public AimAssistant() {
        super("AimAssistant", "Доводит прицел до цели", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        this.target = null;
        this.aimVector = null;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || mc.level == null) return;

        if (event.getPhase() == PlayerTickEvent.Phase.PRE) {
            LivingEntity found = findTarget();
            if (found != this.target) {
                this.aimVector = null;
            }
            this.target = found;
        }
    }

    @EventTarget
    public void onLook(LookEvent event) {
        if (!inGame() || player() == null || mc.level == null || this.target == null) return;

        if (!isValidTarget(this.target) || player().isUsingItem()) {
            return;
        }

        if (onlyWeapon.getValue() && !isHoldingWeapon()) {
            return;
        }

        Vec3 eye = player().getEyePosition();

        if (wallCheck.getValue() && !player().hasLineOfSight(this.target)) {
            return;
        }

        Vec3 targetPos = getAimPosition(this.target);
        if (targetPos == null) return;

        this.aimVector = (this.aimVector == null) ? targetPos : this.aimVector.lerp(targetPos, 0.2);

        Vec3 dir = this.aimVector.subtract(eye);
        float yaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))));

        float deltaYaw = Mth.wrapDegrees(yaw - player().getYRot());
        float deltaPitch = pitch - player().getXRot();

        if (Math.abs(deltaPitch) <= 13.0f && Math.abs(deltaYaw) < 8.0f && player().hasLineOfSight(this.target)) {
            deltaPitch = 0.0f;
        }

        float frame = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float ease = Mth.clamp(((float) Math.hypot(deltaYaw, deltaPitch)) / 4.0f, 0.0f, 1.0f);
        float stepSpeed = speed.getValue() * frame * ease;

        if (stepSpeed <= 0.0f) return;

        float step = Math.min(1.0f, stepSpeed / Math.max(Math.abs(deltaYaw), Math.abs(deltaPitch) * 2.0f));

        player().setYRot(player().getYRot() + (deltaYaw * step));
        if (deltaPitch != 0.0f) {
            player().setXRot(Mth.clamp(player().getXRot() + (deltaPitch * step), -90.0f, 90.0f));
        }
    }

    private Vec3 getAimPosition(LivingEntity entity) {
        if (player() == null) return null;
        Vec3 eye = player().getEyePosition();
        Vec3 center = entity.getBoundingBox().getCenter();
        if (eye.distanceTo(center) > 4.5) return null;
        return center;
    }

    private LivingEntity findTarget() {
        if (player() == null || mc.level == null) return null;

        Vec3 eye = player().getEyePosition();
        Vec3 look = Vec3.directionFromRotation(player().getXRot(), player().getYRot());

        return StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(this::isValidTarget)
                .filter(entity -> !wallCheck.getValue() || player().hasLineOfSight(entity))
                .min(Comparator.comparingDouble(entity ->
                        Math.acos(Mth.clamp(look.dot(entity.getBoundingBox().getCenter().subtract(eye).normalize()), -1.0d, 1.0d))))
                .orElse(null);
    }

    private boolean isHoldingWeapon() {
        if (player() == null) return false;
        ItemStack stack = player().getMainHandItem();
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.MACE);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (player() == null || entity == null || !entity.isAlive() || entity.isRemoved() || entity == player()) {
            return false;
        }

        double maxDist = 4.0 + (player().getDeltaMovement().length() * 3.0);
        if (player().distanceToSqr(entity) > maxDist * maxDist) {
            return false;
        }

        if (entity instanceof Player p) {
            if (FriendManager.getInstance().isFriend(p)) return false;
            return targets.isEnabled("Players");
        }
        if (entity instanceof Enemy) {
            return targets.isEnabled("Mobs");
        }
        if (entity instanceof Animal || entity instanceof WaterAnimal || entity instanceof AmbientCreature) {
            return targets.isEnabled("Animals");
        }

        return false;
    }
}