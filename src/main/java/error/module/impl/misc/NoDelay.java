package error.module.impl.misc;

import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import error.event.Event;
import error.event.EventTarget;
import error.event.list.GameTickEvent;
import error.event.list.PlayerTickEvent;
import error.mixin.accessor.LivingEntityAccessor;
import error.mixin.accessor.MinecraftAccessor;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.MultiModeSetting;
import error.module.impl.combat.AuraModule;

import java.util.List;

public class NoDelay extends Module {
    public static NoDelay INSTANCE;

    public final MultiModeSetting removals = multiMode("Снятия задержек", List.of("Прыжки", "Клик ПКМ", "Быстрый опыт"), "Прыжки", "Клик ПКМ", "Быстрый опыт");
    public final CheckBox jumpBypass = checkbox("Обход прыжков", true);
    public final MultiModeSetting stoppers = multiMode("Стоппер", List.of("В PvP", "На кулдауне"));

    public NoDelay() {
        super("NoDelay", "Убирает задержки взаимодействия и прыжков", Category.MISC);
        INSTANCE = this;
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null || !isEnabled()) return;

        // Stopper checks
        if (stoppers.isEnabled("В PvP") && AuraModule.INSTANCE != null && AuraModule.INSTANCE.isEnabled() && AuraModule.INSTANCE.getTarget() != null) {
            return;
        }

        if (stoppers.isEnabled("На кулдауне") && mc.player.getCooldowns().isOnCooldown(mc.player.getMainHandItem())) {
            return;
        }

        if (event instanceof GameTickEvent) {
            if (removals.isEnabled("Клик ПКМ")) {
                ((MinecraftAccessor) mc).setRightClickDelay(0);
            } else if (removals.isEnabled("Быстрый опыт")) {
                boolean holdingExp = mc.player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE) ||
                        (mc.player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE) && mc.player.getMainHandItem().getUseAnimation() == ItemUseAnimation.NONE);
                if (holdingExp) {
                    ((MinecraftAccessor) mc).setRightClickDelay(0);
                }
            }
        }

        if (event instanceof PlayerTickEvent) {
            if (removals.isEnabled("Прыжки")) {
                ((LivingEntityAccessor) mc.player).setNoJumpDelay(0);
            }
            if (jumpBypass.getValue() && mc.options.keyJump.isDown() && mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
        }
    }
}
