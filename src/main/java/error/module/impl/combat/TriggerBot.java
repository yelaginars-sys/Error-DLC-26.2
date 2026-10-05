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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Random;

@Getter
public class TriggerBot extends Module {
    public static TriggerBot INSTANCE;

    public final CheckBox onlyCrits = checkbox("Только криты", true);
    public final CheckBox sprintReset = checkbox("Сброс спринта", true);
    public final SliderSetting minDelay = slider("Мин. задержка", 25.0F, 0.0F, 500.0F, 5.0F);
    public final SliderSetting maxDelay = slider("Макс. задержка", 90.0F, 0.0F, 500.0F, 5.0F);
    public final MultiModeSetting targets = multiMode("Цели",
            List.of("Игроки", "Животные", "Мобы", "Невидимки", "Голые игроки"),
            "Игроки", "Животные", "Мобы", "Невидимки", "Голые игроки", "Друзья");

    private LivingEntity target;
    private final Random random = new Random();
    private long nextAttackMs;

    public TriggerBot() {
        super("TriggerBot", "Автоматически атакует при наведении на цель", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.target = null;
        this.nextAttackMs = 0L;
    }

    @Override
    public void onDisable() {
        this.target = null;
        this.nextAttackMs = 0L;
        super.onDisable();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            this.target = null;
            this.nextAttackMs = 0L;
            return;
        }

        if (mc.crosshairPickEntity instanceof LivingEntity var2 && this.checkCondition2(var2)) {
            this.target = var2;
            if (this.checkCondition(var2)) {
                long now = System.currentTimeMillis();
                if (this.nextAttackMs == 0L) {
                    this.nextAttackMs = now + this.randomDelay();
                }

                if (now >= this.nextAttackMs) {
                    if (this.sprintReset.getValue() && mc.player.isSprinting()) {
                        mc.player.setSprinting(false);
                    }

                    mc.gameMode.attack(mc.player, var2);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    this.nextAttackMs = System.currentTimeMillis() + this.randomDelay();
                }
            }
        } else {
            this.target = null;
            this.nextAttackMs = 0L;
        }
    }

    private long randomDelay() {
        float min = Math.min(this.minDelay.getValue(), this.maxDelay.getValue());
        float max = Math.max(this.minDelay.getValue(), this.maxDelay.getValue());
        return (long) (min + this.random.nextFloat() * (max - min));
    }

    private boolean checkCondition(LivingEntity entity) {
        if (mc.player == null) {
            return false;
        }
        if (mc.player.getAttackStrengthScale(0.5F) <= 0.93F) {
            return false;
        }
        return !this.onlyCrits.getValue() || canCritical(entity);
    }

    public static boolean canCritical(LivingEntity target) {
        if (mc.player == null) return false;
        return !mc.player.onGround()
                && mc.player.getDeltaMovement().y < 0.0
                && mc.player.fallDistance > 0.0F
                && !mc.player.onClimbable()
                && !mc.player.isInWater()
                && !mc.player.hasEffect(MobEffects.BLINDNESS)
                && !mc.player.isPassenger()
                && !mc.player.isFallFlying();
    }

    private boolean checkCondition2(LivingEntity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive() || entity.getHealth() <= 0.0F) {
            return false;
        }

        if (entity instanceof ArmorStand) {
            return false;
        }

        if (entity.distanceTo(mc.player) > 3.0F) {
            return false;
        }

        if (entity.isInvisible() && !this.targets.isEnabled("Невидимки")) {
            return false;
        }

        if (entity instanceof Player var2) {
            if (FriendManager.getInstance().isFriend(var2.getName().getString()) && !this.targets.isEnabled("Друзья")) {
                return false;
            } else {
                return this.checkCondition3(var2) ? this.targets.isEnabled("Голые игроки") : this.targets.isEnabled("Игроки");
            }
        } else if (entity instanceof Animal) {
            return this.targets.isEnabled("Животные");
        } else if (entity instanceof Monster) {
            return this.targets.isEnabled("Мобы");
        }
        return false;
    }

    private boolean checkCondition3(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty() && !stack.is(Items.ELYTRA)) {
                    return false;
                }
            }
        }
        return true;
    }
}