package error.module.impl.movement;

import error.event.EventTarget;
import error.event.list.NoSlowEvent;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

public class NoSlow extends Module {
    public static NoSlow INSTANCE;

    public final ModeSetting mode = mode("Мод", "Vanilla", "Vanilla", "ReallyWorld", "Grim New");

    public final ModeSetting tickrateMode = mode("Мод тикрейта", "Легитный", "Легитный", "Обычный")
            .visible(() -> mode.is("ReallyWorld"));

    public final CheckBox sprintAlways = checkbox("Позволять спринт", true);
    public final CheckBox onlyFoodAndPotions = checkbox("Только расходники", false);

    private int grimTicks = 0;
    private int rwLegitTicks = 0;
    private int rwNormalTicks = 0;
    private long lastHurtTime = 0L;

    public NoSlow() {
        super("NoSlow", "Убирает замедление при использовании предметов (еда, зелья, лук)", Category.MOVEMENT);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        this.grimTicks = 0;
        this.rwLegitTicks = 0;
        this.rwNormalTicks = 0;
        this.lastHurtTime = 0L;
        super.onEnable();
    }

    @Override
    protected void onDisable() {
        this.grimTicks = 0;
        this.rwLegitTicks = 0;
        this.rwNormalTicks = 0;
        super.onDisable();
    }

    private boolean canNoSlow() {
        if (!inGame() || player() == null) return false;
        if (player().isUsingItem() && !player().isPassenger() && !player().isFallFlying()) {
            ItemStack item = player().getUseItem();
            if (item.isEmpty()) return false;
            if (onlyFoodAndPotions.getValue()) {
                ItemUseAnimation anim = item.getUseAnimation();
                return anim == ItemUseAnimation.EAT || anim == ItemUseAnimation.DRINK;
            }
            return item.getUseAnimation() != ItemUseAnimation.NONE;
        }
        return false;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null) return;
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        if (mode.is("Grim New") && canNoSlow()) {
            this.grimTicks++;
        } else {
            this.grimTicks = 0;
        }

        if (mode.is("ReallyWorld")) {
            if (tickrateMode.is("Легитный") && canNoSlow()) {
                this.rwLegitTicks++;
            } else {
                this.rwLegitTicks = 0;
            }

            if (tickrateMode.is("Обычный") && canNoSlow()) {
                this.rwNormalTicks++;
            } else {
                this.rwNormalTicks = 0;
            }
        }
    }

    @EventTarget
    public void onNoSlow(NoSlowEvent event) {
        if (!inGame() || player() == null || !canNoSlow()) return;

        if (mode.is("Vanilla")) {
            event.cancel();
            return;
        }

        if (mode.is("Grim New")) {
            if (this.grimTicks > 1 && (this.grimTicks & 1) == 0) {
                event.cancel();
            }
            return;
        }

        if (mode.is("ReallyWorld")) {
            if (tickrateMode.is("Обычный")) {
                if (this.rwNormalTicks >= 2) {
                    boolean allow = player().getUsedItemHand() == InteractionHand.OFF_HAND;
                    if (player().getUsedItemHand() == InteractionHand.MAIN_HAND) {
                        boolean waterOrHurt = player().isInWater();
                        if (player().hurtTime != 0) {
                            this.lastHurtTime = System.currentTimeMillis();
                        }
                        if (System.currentTimeMillis() - this.lastHurtTime < 1100L) {
                            waterOrHurt = true;
                        }
                        if (waterOrHurt || this.rwNormalTicks >= 3) {
                            allow = true;
                        }
                    }
                    if (allow) {
                        event.cancel();
                        this.rwNormalTicks = 0;
                    }
                }
            } else if (tickrateMode.is("Легитный")) {
                if (!player().isInWater() && this.rwLegitTicks > 2) {
                    event.cancel();
                    this.rwLegitTicks = 0;
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!inGame() || player() == null) return;
        if (mode.is("ReallyWorld") && tickrateMode.is("Обычный")) {
            if (event.isReceive() && event.getPacket() instanceof ClientboundSetHeldSlotPacket packet) {
                if (canNoSlow()) {
                    int selected = player().getInventory().getSelectedSlot();
                    if (packet.slot() != selected && mc.getConnection() != null) {
                        mc.getConnection().send(new ServerboundSetCarriedItemPacket((selected % 8) + 1));
                        mc.getConnection().send(new ServerboundSetCarriedItemPacket(selected));
                        event.cancel();
                    }
                }
            }
        }
    }
}
