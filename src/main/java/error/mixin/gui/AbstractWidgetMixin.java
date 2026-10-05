package error.mixin.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @Shadow public int width;
    @Shadow public int height;
    @Shadow public boolean active;
    @Shadow public boolean visible;
    @Shadow public abstract int getX();
    @Shadow public abstract int getY();
    @Shadow public abstract Component getMessage();
    @Shadow public abstract boolean isHovered();
    @Shadow public abstract boolean isFocused();
}
