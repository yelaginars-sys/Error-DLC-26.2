package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import error.util.RotationHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AimBot extends Module {
    public static AimBot INSTANCE;

    public final SliderSetting distance = slider("Дистанция", 55.0F, 3.0F, 140.0F, 1.0F);
    public final SliderSetting speed = slider("Скорость наводки", 360.0F, 30.0F, 360.0F, 5.0F);
    public final ModeSetting priority = mode("Приоритет", "Оптимальный", "Оптимальный", "Дистанция", "Здоровье", "Угол поворота");
    public final MultiModeSetting weapons = multiMode("Оружие", List.of("Лук", "Трезубец"), "Лук", "Трезубец");
    public final MultiModeSetting targets = multiMode("Цели", List.of("Голые"), "Голые", "Друзья");
    public final CheckBox onlyVisible = checkbox("Только видимые", false);

    private LivingEntity target;

    public AimBot() {
        super("AimBot", "Автоматически наводит лук и трезубец на противников с баллистикой", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        this.target = null;
        RotationHandler.clear();
        super.onDisable();
    }

    private record ProjectileProperties(double speed, double gravity) {}

    private ProjectileProperties getWeaponProperties() {
        if (!inGame() || player() == null || !player().isUsingItem()) return null;
        ItemStack item = player().getUseItem();
        if (item.isEmpty()) return null;

        if (item.getItem() instanceof BowItem) {
            if (!weapons.isEnabled("Лук")) return null;
            int useDuration = player().getTicksUsingItem();
            float pull = BowItem.getPowerForTime(useDuration);
            pull = Math.max(pull, 0.1F);
            return new ProjectileProperties(3.0 * pull, 0.05);
        } else if (item.getItem() instanceof TridentItem) {
            if (!weapons.isEnabled("Трезубец")) return null;
            return new ProjectileProperties(2.5, 0.05);
        }

        return null;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || level() == null) {
            this.target = null;
            return;
        }
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        ProjectileProperties props = getWeaponProperties();
        if (props == null) {
            if (this.target != null) {
                this.target = null;
                RotationHandler.clear();
            }
            return;
        }

        if (this.target == null || !isValidTarget(this.target)) {
            findTarget();
        }

        if (this.target != null) {
            float[] rotations = calculateBallisticRotation(this.target, props);
            if (rotations != null) {
                float targetYaw = rotations[0];
                float targetPitch = rotations[1];

                if (speed.getValue() < 360.0F) {
                    float currentYaw = player().getYRot();
                    float currentPitch = player().getXRot();
                    float maxStep = speed.getValue() / 20.0F;

                    float deltaYaw = Mth.wrapDegrees(targetYaw - currentYaw);
                    float deltaPitch = targetPitch - currentPitch;

                    targetYaw = currentYaw + Mth.clamp(deltaYaw, -maxStep, maxStep);
                    targetPitch = currentPitch + Mth.clamp(deltaPitch, -maxStep, maxStep);
                }

                RotationHandler.setRotation(targetYaw, targetPitch);
            }
        }
    }

    private void findTarget() {
        if (player() == null || level() == null) {
            this.target = null;
            return;
        }

        List<Player> list = new ArrayList<>();
        for (Entity e : level().entitiesForRendering()) {
            if (e instanceof Player p && isValidTarget(p)) {
                list.add(p);
            }
        }

        if (list.isEmpty()) {
            this.target = null;
            return;
        }

        if (list.size() == 1) {
            this.target = list.get(0);
            return;
        }

        switch (priority.getValue()) {
            case "Дистанция" -> list.sort(Comparator.comparingDouble(p -> player().distanceTo(p)));
            case "Здоровье" -> list.sort(Comparator.comparingDouble(this::getEffectiveHealth));
            case "Угол поворота" -> list.sort(Comparator.comparingDouble(this::getAngleToTarget));
            default -> list.sort(Comparator.comparingDouble(this::getOptimalScore));
        }

        this.target = list.get(0);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (player() == null || level() == null) return false;
        if (!(entity instanceof Player p)) return false;
        if (p == player()) return false;
        if (!p.isAlive() || p.isInvulnerable()) return false;
        if (player().distanceTo(p) > distance.getValue()) return false;
        if (onlyVisible.getValue() && !player().hasLineOfSight(p)) return false;

        boolean isFriend = FriendManager.getInstance().isFriend(p.getName().getString());
        if (isFriend && !targets.isEnabled("Друзья")) return false;

        boolean isNaked = p.getArmorValue() == 0;
        if (isNaked && !targets.isEnabled("Голые")) return false;

        return true;
    }

    private double getEffectiveHealth(LivingEntity entity) {
        double hp = entity.getHealth() + entity.getAbsorptionAmount();
        double armor = entity.getAttributeValue(Attributes.ARMOR);
        return hp * (1.0 + armor / 25.0);
    }

    private double getAngleToTarget(LivingEntity entity) {
        if (player() == null) return 0.0;
        Vec3 eyePos = player().getEyePosition();
        Vec3 targetPos = entity.position().add(0.0, entity.getBbHeight() / 2.0, 0.0);
        Vec3 diff = targetPos.subtract(eyePos);
        double yaw = Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0;
        double pitch = -Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));
        double dYaw = Mth.wrapDegrees((float) (yaw - player().getYRot()));
        double dPitch = pitch - player().getXRot();
        return Math.sqrt(dYaw * dYaw + dPitch * dPitch);
    }

    private double getOptimalScore(LivingEntity entity) {
        if (player() == null) return Double.MAX_VALUE;
        double score = player().distanceTo(entity) * 3.0;
        score += getAngleToTarget(entity) * 2.0;
        score += getEffectiveHealth(entity) * 1.5;
        if (player().hasLineOfSight(entity)) score -= 2.0;
        return score;
    }

    private float[] calculateBallisticRotation(LivingEntity target, ProjectileProperties props) {
        if (player() == null || props == null) return null;

        Vec3 eyePos = player().getEyePosition();
        double targetAimHeight = Math.clamp(target.getBbHeight() * 0.7, 0.35, 1.6);
        Vec3 targetCenter = target.position().add(0, targetAimHeight, 0);

        Vec3 targetVel = target.getDeltaMovement();
        Vec3 playerVel = player().getDeltaMovement();

        double travelTime = Math.clamp(eyePos.distanceTo(targetCenter) / Math.max(props.speed(), 0.01), 0.0, 3.0);
        float yaw = player().getYRot();
        float pitch = player().getXRot();

        for (int i = 0; i < 4; i++) {
            Vec3 predictedPos = targetCenter.add(targetVel.scale(travelTime));
            Vec3 diff = predictedPos.subtract(eyePos).subtract(playerVel.scale(travelTime));
            double flatDist = Math.hypot(diff.x, diff.z);
            if (flatDist < 1.0E-4) break;

            double calculatedPitch = solveBallisticPitch(flatDist, diff.y, props.speed(), props.gravity());
            if (Double.isNaN(calculatedPitch)) {
                calculatedPitch = -Math.toDegrees(Math.atan2(diff.y, flatDist));
            }

            yaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
            pitch = (float) Mth.clamp(calculatedPitch, -89.0, 89.0);

            double horizontalSpeed = Math.abs(props.speed() * Math.cos(Math.toRadians(calculatedPitch)));
            if (horizontalSpeed < 1.0E-4) break;

            travelTime = Math.clamp(flatDist / horizontalSpeed, 0.0, 3.0);
        }

        return new float[]{yaw, pitch};
    }

    private double solveBallisticPitch(double x, double y, double v, double g) {
        if (x <= 0.0 || v <= 0.0 || g <= 0.0) return Double.NaN;
        double v2 = v * v;
        double inside = v2 * v2 - g * (g * x * x + 2.0 * y * v2);
        if (inside < 0.0) return Double.NaN;
        double sqrt = Math.sqrt(inside);
        double angleRad = Math.atan((v2 - sqrt) / (g * x));
        return -Math.toDegrees(angleRad);
    }
}
