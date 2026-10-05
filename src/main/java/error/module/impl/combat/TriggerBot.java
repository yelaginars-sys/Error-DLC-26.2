package error.module.impl.combat;

import lombok.Getter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.Blocks;
import error.util.player.SprintReset;
import error.event.EventTarget;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.friend.FriendManager;

import java.util.Random;

/**
 */
@Getter
public class TriggerBot extends Module {
    public static TriggerBot INSTANCE;

    private final CheckBox onlycrit = checkbox("Only Crits", false);
    private final CheckBox spaceOnly = checkbox("Only Space", false).visible(onlycrit::getValue);
    private final ModeSetting sprintReset = mode("Sprint Rest", "Legit", "None", "Legit");
    private final CheckBox shieldcheck = checkbox("Checks Shield", true);
    private final CheckBox selfshieldcheck = checkbox("Attack shield", false);
    private final CheckBox randomDelay = checkbox("Random Fall Distance", false);
    private final CheckBox pauseEating = checkbox("Pause while Eating", true);

    private int ticksToWait = 0;
    private int currentTick = 0;
    private final Random random = new Random();

    public TriggerBot() {
        super("TriggerBot", "Сам бьет при наведении на цель", Category.COMBAT);
        INSTANCE = this;
    }

    public LivingEntity getTarget() {
        if (mc != null && mc.crosshairPickEntity instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public void onEnable() {
        currentTick = 0;
        ticksToWait = randomDelay.getValue() ? random.nextInt(4) : 0;
    }

    @Override
    public void onDisable() {
        currentTick = 0;
        ticksToWait = 0;
        SprintReset.reset();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || mc.gameMode == null || screen() != null) return;
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        if (!(mc.crosshairPickEntity instanceof LivingEntity target)) return;
        if (!isValidTarget(target)) return;

        if (player().getAttackStrengthScale(0.5f) > 0.9f) {
            if (player().fallDistance > 1.0f && !player().isFallFlying()) return;
            if (!canCrit()) return;

            if (randomDelay.getValue()) {
                if (currentTick < ticksToWait) {
                    currentTick++;
                    return;
                }
            }

            if (SprintReset.isAttackBlocked(sprintReset.getValue())) {
                return;
            }

            mc.gameMode.attack(player(), target);
            player().swing(InteractionHand.MAIN_HAND);
            player().resetAttackStrengthTicker();

            if (randomDelay.getValue()) {
                ticksToWait = random.nextInt(4);
                currentTick = 0;
            }
        }
    }

    @EventTarget
    public void onInput(PlayerInputEvent event) {
        if (!inGame() || player() == null) return;

        boolean shouldReset = sprintReset.getValue().equalsIgnoreCase("Legit") && shouldResetSprinting();
        SprintReset.handleInput(event, shouldReset);
    }

    private boolean shouldResetSprinting() {
        if (player() == null || !player().isSprinting() || player().isInWater()) return false;
        if (!(mc.crosshairPickEntity instanceof LivingEntity target)) return false;
        if (!isValidTarget(target)) return false;

        return player().getAttackStrengthScale(0.5f) >= 0.90f && canCrit();
    }

    private boolean isValidTarget(LivingEntity target) {
        if (!target.isAlive() || target.isDeadOrDying()) return false;

        if (target instanceof Player targetPlayer && FriendManager.getInstance().isFriend(targetPlayer)) {
            return false;
        }

        if (player().isUsingItem() && pauseEating.getValue()) {
            return false;
        }

        if (shieldcheck.getValue() && target.isUsingItem() && target.getUseItem().getItem() instanceof ShieldItem) {
            return false;
        }

        if (!selfshieldcheck.getValue() && player().isUsingItem() && player().getUseItem().getItem() instanceof ShieldItem) {
            return false;
        }

        return true;
    }

    private boolean canCrit() {
        boolean reasonForSkipCrit = !onlycrit.getValue()
                || player().getAbilities().flying
                || player().hasEffect(MobEffects.LEVITATION)
                || player().hasEffect(MobEffects.BLINDNESS)
                || player().level().getBlockState(player().blockPosition()).is(Blocks.LADDER)
                || player().isFallFlying()
                || player().isInWater()
                || player().isUnderWater();

        if (player().getAttackStrengthScale(0.5f) < (player().onGround() ? 1.0f : 0.9f)) {
            return false;
        }

        boolean onGround = player().onGround();

        if (!mc.options.keyJump.isDown() && onGround && spaceOnly.getValue()) {
            return true;
        }

        if (player().isInLava()) {
            return true;
        }

        if (!reasonForSkipCrit) {
            return !player().onGround() && player().fallDistance > 0.0f;
        }

        return true;
    }
}