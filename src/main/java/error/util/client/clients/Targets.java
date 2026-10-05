package error.util.client.clients;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import error.Client;
import error.friend.FriendManager;
import error.module.impl.combat.AuraModule;
import error.setting.impl.MultiModeSetting;
import net.minecraft.world.phys.Vec3;

import static error.IMinecraft.mc;

/**
 */
public final class Targets {

    @Getter
    private static LivingEntity target;
    @Getter
    @Setter
    private static LivingEntity lastTarget;
    @Getter
    private static boolean targetLocked = false;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public static boolean isNaked(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidTarget(Entity entity, double range, MultiModeSetting filter, float fovLimit) {
        if (mc.player == null || entity == null || entity == mc.player) return false;
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isDeadOrDying() || living.isRemoved()) return false;
        if (mc.player.distanceToSqr(entity) > (range * range)) return false;

        if (living.isInvisible() && !(filter.isEnabled("Invisibles") || filter.isEnabled("Невидимые") || filter.isEnabled("Невидимки"))) {
            return false;
        }

        if (fovLimit < 360.0F) {
            Vec3 eye = mc.player.getEyePosition();
            Vec3 targetEye = living.getEyePosition();
            Vec3 dir = targetEye.subtract(eye).normalize();
            Vec3 look = mc.player.getLookAngle().normalize();
            double dot = look.dot(dir);
            double angleDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
            if (angleDeg > fovLimit * 0.5) {
                return false;
            }
        }

        if (entity instanceof Player player) {
            boolean isFriend = FriendManager.getInstance().isFriend(player);
            boolean friendsEnabled = filter.isEnabled("Friends") || filter.isEnabled("Друзья");

            if (isFriend && !friendsEnabled) {
                return false;
            }

            boolean playersEnabled = filter.isEnabled("Players") || filter.isEnabled("Игроки");
            if (!playersEnabled && !isFriend) return false;

            boolean nakedEnabled = filter.isEnabled("Naked") || filter.isEnabled("Голые");
            if (!nakedEnabled && isNaked(player)) return false;

            return true;
        }

        if ((filter.isEnabled("Villagers") || filter.isEnabled("Жители")) && (entity instanceof Villager || entity instanceof IronGolem)) return true;
        if ((filter.isEnabled("Mobs") || filter.isEnabled("Мобы") || filter.isEnabled("Монстры")) && (entity instanceof Enemy)) return true;
        if ((filter.isEnabled("Animals") || filter.isEnabled("Животные") || filter.isEnabled("Мирные")) &&
                (entity instanceof Animal || entity instanceof WaterAnimal || entity instanceof AmbientCreature)) return true;

        return false;
    }

    public static boolean isValidTarget(Entity entity, double range, MultiModeSetting filter) {
        return isValidTarget(entity, range, filter, 360.0F);
    }

    public static LivingEntity findTarget(double range, MultiModeSetting filter) {
        return findTarget(range, filter, "Distance", 360.0F);
    }

    public static LivingEntity findTarget(double range, MultiModeSetting filter, String sortMode, float fovLimit) {
        if (mc.player == null || mc.level == null) {
            reset();
            return null;
        }

        lastTarget = target;
        double rangeSq = range * range;

        if (target != null && target.isAlive() && !target.isRemoved() && !target.isDeadOrDying()
                && mc.player.distanceToSqr(target) <= rangeSq
                && isValidTarget(target, range, filter, fovLimit)) {
            targetLocked = true;
            return target;
        }

        unlockTarget();

        LivingEntity bestTarget = null;
        double bestVal = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.isRemoved() || living.isDeadOrDying()) continue;

            double distSq = mc.player.distanceToSqr(living);
            if (distSq > rangeSq) continue;
            if (!isValidTarget(living, range, filter, fovLimit)) continue;

            double val;
            if ("Health".equalsIgnoreCase(sortMode)) {
                val = living.getHealth() + living.getAbsorptionAmount();
            } else if ("Armor".equalsIgnoreCase(sortMode)) {
                val = living.getArmorValue();
            } else if ("FOV".equalsIgnoreCase(sortMode)) {
                Vec3 eye = mc.player.getEyePosition();
                Vec3 targetEye = living.getEyePosition();
                Vec3 dir = targetEye.subtract(eye).normalize();
                Vec3 look = mc.player.getLookAngle().normalize();
                val = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, look.dot(dir)))));
            } else {
                val = distSq; // Distance
            }

            if (bestTarget == null || val < bestVal) {
                bestTarget = living;
                bestVal = val;
            }
        }

        target = bestTarget;
        targetLocked = (target != null);

        return target;
    }

    public static LivingEntity findTarget(LivingEntity currentTarget, double range, MultiModeSetting filter) {
        if (currentTarget != null && target == null) {
            target = currentTarget;
        }
        return findTarget(range, filter);
    }

    public static void unlockTarget() {
        target = null;
        targetLocked = false;
    }

    public static void reset() {
        target = null;
        lastTarget = null;
        targetLocked = false;
    }

    public static void lockTarget() {
        if (target != null) {
            targetLocked = true;
        }
    }

    public static LivingEntity getCurrentTarget() {
        if (target != null && target.isAlive() && !target.isRemoved()) {
            return target;
        }

        if (mc.player == null || mc.level == null) return null;

        if (Client.INSTANCE != null && Client.INSTANCE.moduleManager != null) {
            AuraModule aura = (AuraModule) Client.INSTANCE.moduleManager.getAuraModule();
            if (aura != null && aura.isEnabled() && aura.getTarget() != null && aura.getTarget().isAlive()) {
                return aura.getTarget();
            }
        }

        return null;
    }

}