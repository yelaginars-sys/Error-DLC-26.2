package error.mixin.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void alignTitleButtons(CallbackInfo ci) {
        float centerX = this.width / 2.0F;
        float startY = this.height / 2.0F - 10.0F;

        AbstractWidget spButton = null;
        AbstractWidget mpButton = null;
        AbstractWidget optionsButton = null;
        AbstractWidget quitButton = null;

        for (Object child : this.children()) {
            if (child instanceof AbstractWidget widget) {
                String text = widget.getMessage() != null ? widget.getMessage().getString().toLowerCase() : "";

                if (text.contains("одиночная") || text.contains("singleplayer")) {
                    spButton = widget;
                } else if (text.contains("сетевая") || text.contains("multiplayer")) {
                    mpButton = widget;
                } else if (text.contains("настройки") || text.contains("options")) {
                    optionsButton = widget;
                } else if (text.contains("выйти") || text.contains("quit")) {
                    quitButton = widget;
                }
            }
        }

        float w = 204.0F;
        float h = 20.0F;
        float gap = 5.0F;

        if (spButton != null) {
            spButton.setX((int) (centerX - w / 2.0F));
            spButton.setY((int) startY);
            spButton.setWidth((int) w);
            spButton.setHeight((int) h);
        }

        if (mpButton != null) {
            mpButton.setX((int) (centerX - w / 2.0F));
            mpButton.setY((int) (startY + (h + gap)));
            mpButton.setWidth((int) w);
            mpButton.setHeight((int) h);
        }

        if (optionsButton != null) {
            optionsButton.setX((int) (centerX - w / 2.0F));
            optionsButton.setY((int) (startY + (h + gap) * 2));
            optionsButton.setWidth((int) (w / 2.0F - 2.5F));
            optionsButton.setHeight((int) h);
        }

        if (quitButton != null) {
            quitButton.setX((int) (centerX + 2.5F));
            quitButton.setY((int) (startY + (h + gap) * 2));
            quitButton.setWidth((int) (w / 2.0F - 2.5F));
            quitButton.setHeight((int) h);
        }
    }
}
