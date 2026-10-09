package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.AttackEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Criticals module.
 * Always deals critical hits with Grim old, Slow/Web bypasses and weapon fast-swap.
 */
public class Criticals extends Module {
    public static Criticals INSTANCE;

    public final ModeSetting mode = mode("Режим", "Grim old", "Grim old", "Slow/Web");
    public final CheckBox fastSwap = checkbox("Быстрый свап", false);
    public final ModeSetting weaponType = mode("Оружие", "Меч", "Меч", "Топор").visible(fastSwap::getValue);

    private int prevSlot = -1;

    public Criticals() {
        super("Criticals", "Всегда наносит критические удары", Category.COMBAT);
        INSTANCE = this;
    }

    private void sendPositionOffset(double yOffset) {
        if (player() == null || mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                player().getX(),
                player().getY() + yOffset,
                player().getZ(),
                player().getYRot(),
                player().getXRot(),
                false,
                false
        ));
    }

    private boolean isServer(String domain) {
        if (mc.getCurrentServer() == null || mc.getCurrentServer().ip == null) return false;
        return mc.getCurrentServer().ip.toLowerCase().contains(domain.toLowerCase());
    }

    @EventTarget(priority = -200)
    public void onAttack(final AttackEvent event) {
        if (event.isCancelled() || !inGame() || player() == null || mc.level == null) return;

        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity)) return;

        // Fast weapon swap logic
        if (fastSwap.getValue() && canFastSwap()) {
            int bestSlot = findBestWeaponSlot();
            int currentSlot = player().getInventory().getSelectedSlot();
            if (bestSlot >= 0 && bestSlot != currentSlot) {
                prevSlot = currentSlot;
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(bestSlot));
                }
            }
        }

        // Critical packet offsets
        if (mode.is("Grim old")) {
            if (!isServer("mineblaze")) {
                if (!player().onGround() && !player().isFallFlying() && !player().isInWater()) {
                    player().fallDistance = 0.01F;
                    sendPositionOffset(-1.0E-6);
                }
            } else {
                if (!player().isInWater()) {
                    player().fallDistance = 0.01F;
                    sendPositionOffset(-1.0E-6);
                }
            }
        } else if (mode.is("Slow/Web")) {
            if (!player().onGround() && !player().isFallFlying() && !player().isInWater() && isInCobweb()) {
                player().fallDistance = 0.08F;
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                            player().getX(),
                            player().getY() - 1.0E-6,
                            player().getZ(),
                            false,
                            false
                    ));
                }
            }
        }

        // Swap back after attack if fast swap was used
        if (prevSlot != -1) {
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(prevSlot));
            }
            prevSlot = -1;
        }
    }

    private boolean canFastSwap() {
        if (player() == null) return false;
        if (player().getAttackStrengthScale(0.5F) <= 0.9F) return false;
        if (player().onClimbable() || player().isInWater()) return false;
        if (player().hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) return false;
        if (player().isPassenger() || player().isSprinting()) return false;
        return !player().onGround() || player().fallDistance > 0.0F;
    }

    private int findBestWeaponSlot() {
        int bestSlot = -1;
        float maxDamage = -1.0F;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player().getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            boolean matches = weaponType.is("Меч") ? stack.is(ItemTags.SWORDS) : stack.is(ItemTags.AXES);
            if (!matches) continue;

            float dmg = getAttackDamage(stack);
            if (dmg > maxDamage) {
                maxDamage = dmg;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private float getAttackDamage(ItemStack stack) {
        var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return 0.0F;
        float total = 0.0F;
        for (var entry : modifiers.modifiers()) {
            if (entry.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
                total += (float) entry.modifier().amount();
            }
        }
        return total;
    }

    private boolean isInCobweb() {
        if (player() == null || mc.level == null) return false;
        AABB box = player().getBoundingBox();
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            if (mc.level.getBlockState(pos).is(Blocks.COBWEB)) {
                return true;
            }
        }
        return false;
    }
}