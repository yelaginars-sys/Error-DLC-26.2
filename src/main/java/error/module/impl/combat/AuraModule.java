package error.module.impl.combat;

import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemUseAnimation;
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

    public final MultiModeSetting targets = multiMode("Таргеты", List.of("Игроки", "Голые", "Невидимки", "Мобы"), "Игроки", "Голые", "Невидимки", "Мирные", "Мобы", "Друзья", "Жители");
    public final ModeSetting targetSort = mode("Приоритет", "Дистанция", "Дистанция", "Здоровье", "Угол", "Никакой");
    public final SliderSetting attackRange = slider("Дистанция атаки", 3.0f, 0.0f, 6.0f, 0.05f);
    public final SliderSetting aimRange = slider("Дистанция наводки", 3.0f, 0.0f, 6.0f, 0.05f);
    public final SliderSetting fov = slider("FOV", 360.0f, 10.0f, 360.0f, 5.0f);

    public final ModeSetting rotMode = mode("Ротация", "Funtime", "Funtime", "HolyLegit", "HolyWorld", "SpookyTime", "4pookyTime", "Spooky", "ReallyWorld", "HelixWave", "Artygrief", "Sloth", "Legit", "Linear", "Matrix", "Builder", "Error", "Grim", "Snap", "Smooth", "Spooky (Error)", "ReallyWorld (Error)", "FunTime (Error)", "AimAssist (Error)", "HolyWorld (Error)", "ML (Error)", "Ares/FT (Error)", "Snap (Error)", "FunTime (exs)", "SpookyTime (exs)", "SlimeWorld (exs)", "ReallyWorld (exs)", "LonyGrief (exs)", "HvH (exs)", "Shard (exs)", "Sloth (exs)", "Legit (exs)");
    public final ModeSetting moveFix = mode("Коррекция", "Свободная", "Нет", "Свободная", "Сфокусированная", "Полная");
    public final ModeSetting sprintReset = mode("Сброс спринта", "Legit", "None", "Legit", "Packet")
            .visible(() -> !rotMode.getValue().endsWith("(Error)") && !rotMode.getValue().endsWith("(exs)"));

    // Error Rotations Settings
    public final SliderSetting enAimAssistPower = slider("Сила AimAssist", 0.5f, 0.05f, 1.0f, 0.05f)
            .visible(() -> rotMode.is("AimAssist (Error)"));
    public final SliderSetting enJerkSmoothing = slider("Сглаживание джерка", 3.0f, 1.0f, 16.0f, 1.0f)
            .visible(() -> rotMode.is("Spooky (Error)") || rotMode.is("HolyWorld (Error)"));
    public final SliderSetting enJerkSpeed = slider("Скорость джерка", 0.5f, 0.05f, 3.0f, 0.05f)
            .visible(() -> rotMode.is("Spooky (Error)") || rotMode.is("HolyWorld (Error)"));
    public final SliderSetting enDovodka = slider("Доводка", 0.35f, 0.0f, 1.0f, 0.05f)
            .visible(() -> rotMode.is("ReallyWorld (Error)") || rotMode.is("HolyWorld (Error)"));
    public final CheckBox enRwThroughWalls = checkbox("Бить через стены RW", true)
            .visible(() -> rotMode.is("ReallyWorld (Error)"));
    public final CheckBox enOnlyHits = checkbox("Только попадания", true)
            .visible(() -> rotMode.is("ML (Error)"));
    public final ModeSetting enSprintBypass = mode("Обход спринта", "Legit", "Legit", "Funtime", "None")
            .visible(() -> rotMode.getValue().endsWith("(Error)") && !rotMode.is("Spooky (Error)"));
    public final CheckBox enNoAttackContainer = checkbox("Не бить если открыт контейнер", false)
            .visible(() -> rotMode.is("Ares/FT (Error)"));

    // Exclusive (exs) Rotations Settings
    public final CheckBox exsSpookyBypass = checkbox("Spooky обход (exs)", true)
            .visible(() -> rotMode.is("SpookyTime (exs)"));
    public final SliderSetting exsSlimeSmooth = slider("Сглаживание (exs)", 0.6f, 0.1f, 1.0f, 0.05f)
            .visible(() -> rotMode.is("SlimeWorld (exs)"));
    public final SliderSetting exsLonyAccel = slider("Ускорение (exs)", 0.12f, 0.05f, 0.50f, 0.01f)
            .visible(() -> rotMode.is("LonyGrief (exs)"));
    public final CheckBox exsRwSnap = checkbox("Snap атака (exs)", true)
            .visible(() -> rotMode.is("ReallyWorld (exs)"));
    public final SliderSetting exsSlothFov = slider("FOV обзора (exs)", 60.0f, 10.0f, 180.0f, 5.0f)
            .visible(() -> rotMode.is("Sloth (exs)"));
    public final CheckBox exsShardMissHits = checkbox("Случайный промах (exs)", true)
            .visible(() -> rotMode.is("Shard (exs)"));

    public final CheckBox smartCrits = checkbox("Умные криты", true);
    public final CheckBox onlyCrits = checkbox("Только криты", true);
    public final CheckBox randomFallDistance = checkbox("Случайный падающий крит", false);

    public final CheckBox throughWalls = checkbox("Бить через стены", false);
    public final CheckBox bypassRwWalls = checkbox("Обход рв стен", false)
            .visible(() -> rotMode.is("ReallyWorld") || rotMode.is("ReallyWorld (En)"));
    public final CheckBox lookDownBypass = checkbox("Смотреть вниз", false)
            .visible(() -> bypassRwWalls.getValue() && (rotMode.is("ReallyWorld") || rotMode.is("ReallyWorld (En)")));

    public final CheckBox unshield = checkbox("Отжимать щит", false);
    public final CheckBox shieldBreaker = checkbox("Ломать щит", true);
    public final CheckBox pauseEating = checkbox("Не бить когда ешь", true);
    public final CheckBox autoCerberus = checkbox("Авто цербер", false)
            .visible(() -> rotMode.is("Funtime") || rotMode.is("FunTime (En)") || rotMode.is("Ares/FT (En)"));
    public final SliderSetting swapDelay = slider("Задержка свапа", 4.0f, 2.0f, 10.0f, 1.0f)
            .visible(() -> autoCerberus.getValue() && (rotMode.is("Funtime") || rotMode.is("FunTime (En)") || rotMode.is("Ares/FT (En)")));
    public final CheckBox clientLook = checkbox("Наводка от первого лица", false);

    private LivingEntity target = null;

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
        RotationHandler.disengage("Smooth");
        AttackHandler.reset();
        Targets.reset();
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
            float maxFindDist = aimRange.getValue();
            target = Targets.findTarget(maxFindDist, targets, targetSort.getValue(), fov.getValue());

            if (target == null) {
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

        if (rotMode.is("Ares/FT (Error)") && enNoAttackContainer.getValue() && screen() != null && !(screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;

        if (!AttackHandler.shouldAttack(target, this)) return;

        boolean isIntersecting = player().getBoundingBox().intersects(target.getBoundingBox());

        float checkYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player().getYRot();
        float checkPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player().getXRot();
        boolean isAimed = isIntersecting || RayTraceUtils.isLookingAt(target, attackRange.getValue(), checkYaw, checkPitch);
        if (!isAimed) return;

        boolean canHit = isIntersecting || this.throughWalls.getValue() || (rotMode.is("ReallyWorld (Error)") && enRwThroughWalls.getValue()) || isVisible;
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
    public boolean usesCubesTargetEsp() { return hasTargetEsp() && error.module.impl.render.TargetEsp.INSTANCE.mode.is("Crystal"); }
    public boolean usesMarkerTargetEsp() { return hasTargetEsp() && error.module.impl.render.TargetEsp.INSTANCE.mode.is("Ромб"); }
    public boolean usesGhostTargetEsp() { return hasTargetEsp() && (error.module.impl.render.TargetEsp.INSTANCE.mode.is("Призраки") || error.module.impl.render.TargetEsp.INSTANCE.mode.is("Призраки 2")); }
    public boolean usesCircleTargetEsp() { return hasTargetEsp() && error.module.impl.render.TargetEsp.INSTANCE.mode.is("Кружок"); }
}