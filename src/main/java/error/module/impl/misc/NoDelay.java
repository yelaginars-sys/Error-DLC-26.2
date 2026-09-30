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
import error.setting.impl.MultiModeSetting;

/**
 * Create by daun kvass
 */
public class NoDelay extends Module {
    public NoDelay() {
        super("NoDelay", "Уберает задержки", Category.MISC);
    }
    private final MultiModeSetting ms = multiMode("No dealy","Jump","Exp");
    @Override
    public void onEvent(Event event) {
        if (mc.player == null) return;
        if (event instanceof GameTickEvent) {if(ms.isEnabled("Exp")){if (mc.player.getMainHandItem().getItem() == Items.EXPERIENCE_BOTTLE || (mc.player.getOffhandItem().getItem() == Items.EXPERIENCE_BOTTLE && mc.player.getMainHandItem().getUseAnimation() == ItemUseAnimation.NONE)) ((MinecraftAccessor) mc).setRightClickDelay(0);}}
        if(event instanceof PlayerTickEvent){if (ms.isEnabled("Jump") &&mc.player!= null){((LivingEntityAccessor) mc.player).setNoJumpDelay(0);}
        }
    }
}
