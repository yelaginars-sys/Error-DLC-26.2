package error.module.impl.combat;

import lombok.Getter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import error.util.*;
import error.util.UBoxPoints.TargetPoint;
import error.event.EventTarget;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.*;
import error.util.client.clients.Targets;
import error.util.player.MoveUtility;

import java.util.List;

@Getter
public class AuraModule extends Module {
    public static AuraModule INSTANCE;

    public final ModeSetting rotMode = mode("Ротация", "Funtime", "Funtime", "HolyLegit", "HolyWorld", "SpookyTime", "4pookyTime", "Spooky", "ReallyWorld", "HelixWave", "Artygrief", "Sloth", "Legit", "Linear", "Matrix", "Builder", "Lumen", "Grim", "Snap", "Smooth");
    public final MultiModeSetting targets = multiMode("Таргеты", List.of("Игроки", "Голые", "Невидимки", "Мобы"), "Игроки", "Голые", "Невидимки", "Мирные", "Мобы", "Друзья", "Жители");
    public final SliderSetting attackRange = slider("Дистанция атаки", 3.0f, 0.0f, 6.0f, 0.05f);
    public final SliderSetting aimRange = slider("Дистанция наводки", 3.0f, 0.0f, 6.0f, 0.05f);
    public final SliderSetting distancelytra = slider("Дистанция на элитрах", 50.0f, 10.0f, 100.0f, 1.0f);
    public final CheckBox elytraPredict = checkbox("Предикт элитры", true);
    public final ModeSetting predictType = mode("Тип предикта", "Default", "Default", "Limit").visible(elytraPredict::getValue);
    public final CheckBox randomFallDistance = checkbox("Случайный падающий крит", false);
    public final ModeSetting sprintReset = mode("Сброс спринта", "Legit", "None", "Legit", "Packet");

    public PredictUtils.Type getPredictType() {
        return predictType != null && predictType.getValue().equalsIgnoreCase("Limit") ? PredictUtils.Type.LIMIT : PredictUtils.Type.DEFAULT;
    }
    
    public final CheckBox smartCrits = checkbox("Умные криты", true);
    public final CheckBox onlyCrits = checkbox("Только криты", true);
    public final CheckBox throughWalls = checkbox("Бить через стены", false);
    public final CheckBox bypassRwWalls = checkbox("Обход рв стен", false);
    public final CheckBox lookDownBypass = checkbox("Смотреть вниз", false).visible(bypassRwWalls::getValue);
    public final CheckBox unshield = checkbox("Отжимать щит", false);
    public final CheckBox shieldBreaker = checkbox("Ломать щит", true);
    public final CheckBox pauseEating = checkbox("Не бить когда ешь", true);
    public final CheckBox autoCerberus = checkbox("Авто цербер", false);
    public final SliderSetting swapDelay = slider("Задержка свапа", 4.0f, 2.0f, 10.0f, 1.0f).visible(autoCerberus::getValue);
    public final CheckBox clientLook = checkbox("Наводка от первого лица", false);

    public final ModeSetting moveFix = mode("Коррекция", "Свободная", "Нет", "Свободная", "Сфокусированная", "Полная");
    public final ModeSetting targetSort = mode("Приоритет", "Дистанция", "Дистанция", "Здоровье", "Угол", "Никакой");
    public final SliderSetting fov = slider("FOV", 360.0f, 10.0f, 360.0f, 5.0f);

    private LivingEntity target = null;
    private Vec3 predictedElytraPos = null;

    public LivingEntity getTarget() {
        return this.target;
    }

    public AuraModule() {
        super("Aura", "Автоматическая атака целей", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        target = null;
        predictedElytraPos = null;
        RotationHandler.disengage("Smooth");
        AttackHandler.reset();
        Targets.reset();
    }

    public float getElytraRangeBonus() {
        if (player() != null && player().getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) && player().isFallFlying()) {
            return distancelytra.getValue();
        }
        return 0.0f;
    }

    public boolean isEating() {
        if (player() == null || !player().isUsingItem()) return false;
        ItemUseAnimation anim = player().getUseItem().getUseAnimation();
        return anim == ItemUseAnimation.EAT || anim == ItemUseAnimation.DRINK;
    }

    private void handleWeaponSwitch() {
        if (player() == null || target == null) return;
        int bestSlot = -1;
        double maxDamage = -1.0;

        boolean targetShielding = shieldBreaker.getValue() && target instanceof net.minecraft.world.entity.player.Player p && p.isBlocking();

        for (int i = 0; i < 9; i++) {
            var stack = player().getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (targetShielding && stack.is(net.minecraft.tags.ItemTags.AXES)) {
                bestSlot = i;
                break;
            }

            if (AttackHandler.isWeapon(stack)) {
                double dmg = 1.0;
                if (stack.is(net.minecraft.tags.ItemTags.SWORDS)) dmg = 7.0;
                else if (stack.is(net.minecraft.tags.ItemTags.AXES)) dmg = 9.0;
                else if (stack.is(net.minecraft.world.item.Items.MACE)) dmg = 10.0;

                if (dmg > maxDamage) {
                    maxDamage = dmg;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot != -1 && bestSlot != player().getInventory().getSelectedSlot()) {
            player().getInventory().setSelectedSlot(bestSlot);
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || mc.gameMode == null) return;

        if (event.getPhase() == PlayerTickEvent.Phase.PRE) {
            float maxFindDist = aimRange.getValue() + getElytraRangeBonus();
            target = Targets.findTarget(maxFindDist, targets, targetSort.getValue(), fov.getValue());

            if (target == null) {
                predictedElytraPos = null;
                RotationHandler.disengage("Smooth");
                AttackHandler.reset();
                return;
            }

            if (unshield.getValue() && player().isBlocking()) {
                mc.options.keyUse.setDown(false);
            }

            handleWeaponSwitch();

            TargetPoint targetPoint = UBoxPoints.getBestPoint(target);
            if (targetPoint == null) {
                RotationHandler.disengage("Smooth");
                return;
            }

            Vec3 aimPos = targetPoint.point();
            boolean isVisible = targetPoint.isVisible();

            if (!throughWalls.getValue() && !isVisible) {
                RotationHandler.disengage("Smooth");
                return;
            }

            AuraRotation rotation = RotationRegistry.get(rotMode.getValue());
            boolean canAttackNow = AttackHandler.shouldAttack(target, this) && !(pauseEating.getValue() && isEating());

            rotation.tick(player(), target, aimPos, canAttackNow);
            processAttack(isVisible, rotation);
        }
    }

    private void processAttack(boolean isVisible, AuraRotation rotation) {
        if (target == null) return;

        if (pauseEating.getValue() && isEating()) return;

        if (!AttackHandler.shouldAttack(target, this)) return;

        boolean isIntersecting = player().getBoundingBox().intersects(target.getBoundingBox());

        float checkYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player().getYRot();
        float checkPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player().getXRot();
        boolean isAimed = isIntersecting || RayTraceUtils.isLookingAt(target, attackRange.getValue(), checkYaw, checkPitch);
        if (!isAimed) return;

        boolean canHit = isIntersecting || this.throughWalls.getValue() || isVisible;
        if (!canHit) return;

        AttackHandler.attack(target, this, rotation);
    }

    @EventTarget
    public void onInput(PlayerInputEvent event) {
        if (!inGame() || player() == null) return;

        AttackHandler.handleInput(event, this);

        if (target != null && RotationHandler.isActive()) {
            if (!moveFix.getValue().equalsIgnoreCase("Нет")) {
                MoveUtility.fixMovement(event, RotationHandler.getFreeYaw());
            }
        }
    }

    public boolean hasTargetEsp() { return error.module.impl.render.TargetEsp.INSTANCE != null && error.module.impl.render.TargetEsp.INSTANCE.isEnabled(); }
    public boolean usesCubesTargetEsp() { return hasTargetEsp() && error.module.impl.render.TargetEsp.INSTANCE.mode.is("Кубики"); }
    public boolean usesMarkerTargetEsp() { return hasTargetEsp() && error.module.impl.render.TargetEsp.INSTANCE.mode.is("Маркер"); }
    public boolean usesGhostTargetEsp() { return hasTargetEsp() && error.module.impl.render.TargetEsp.INSTANCE.mode.is("Призраки"); }
    public boolean usesCircleTargetEsp() { return hasTargetEsp() && error.module.impl.render.TargetEsp.INSTANCE.mode.is("Кольцо"); }
}