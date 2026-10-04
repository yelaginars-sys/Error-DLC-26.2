package error.mixin.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

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
        AbstractWidget realmsButton = null;
        AbstractWidget optionsButton = null;
        AbstractWidget quitButton = null;
        List<AbstractWidget> smallIconButtons = new ArrayList<>();

        for (Object child : this.children()) {
            if (child instanceof AbstractWidget widget) {
                String text = widget.getMessage() != null ? widget.getMessage().getString().toLowerCase() : "";

                if (widget.getWidth() <= 24) {
                    smallIconButtons.add(widget);
                } else if (text.contains("одиночная") || text.contains("singleplayer")) {
                    spButton = widget;
                } else if (text.contains("мультиплеер") || text.contains("сетевая") || text.contains("multiplayer")) {
                    mpButton = widget;
                } else if (text.contains("realms")) {
                    realmsButton = widget;
                } else if (text.contains("настройки") || text.contains("options")) {
                    optionsButton = widget;
                } else if (text.contains("выход") || text.contains("выйти") || text.contains("quit")) {
                    quitButton = widget;
                } else if (text.contains("друзья") || text.contains("friends")) {
                    smallIconButtons.add(widget);
                }
            }
        }

        float w = 204.0F;
        float h = 20.0F;
        float gap = 4.0F;

        if (spButton != null) {
            spButton.setX((int) (centerX - w / 2.0F));
            spButton.setY((int) startY);
            spButton.setWidth((int) w);
            spButton.setHeight((int) h);
        }

        float currentY = startY + h + gap;
        if (mpButton != null) {
            mpButton.setX((int) (centerX - w / 2.0F));
            mpButton.setY((int) currentY);
            mpButton.setWidth((int) w);
            mpButton.setHeight((int) h);
            currentY += h + gap;
        }

        if (realmsButton != null) {
            realmsButton.setX((int) (centerX - w / 2.0F));
            realmsButton.setY((int) currentY);
            realmsButton.setWidth((int) w);
            realmsButton.setHeight((int) h);
            currentY += h + gap;
        }

        float optionsW = 98.0F;
        if (optionsButton != null) {
            optionsButton.setX((int) (centerX - w / 2.0F));
            optionsButton.setY((int) currentY);
            optionsButton.setWidth((int) optionsW);
            optionsButton.setHeight((int) h);
        }

        if (quitButton != null) {
            quitButton.setX((int) (centerX + w / 2.0F - optionsW));
            quitButton.setY((int) currentY);
            quitButton.setWidth((int) optionsW);
            quitButton.setHeight((int) h);
        }

        // Align small icon buttons neatly on the sides of the bottom row (Options / Quit)
        float smallW = 20.0F;
        float smallH = 20.0F;
        float leftX = centerX - w / 2.0F - gap - smallW;
        float rightX = centerX + w / 2.0F + gap;

        int leftCount = 0;
        int rightCount = 0;

        for (AbstractWidget btn : smallIconButtons) {
            btn.setWidth((int) smallW);
            btn.setHeight((int) smallH);

            String text = btn.getMessage() != null ? btn.getMessage().getString().toLowerCase() : "";
            if (text.contains("язык") || text.contains("lang") || (leftCount == 0 && rightCount > 0)) {
                btn.setX((int) (leftX - leftCount * (smallW + gap)));
                btn.setY((int) currentY);
                leftCount++;
            } else {
                btn.setX((int) (rightX + rightCount * (smallW + gap)));
                btn.setY((int) currentY);
                rightCount++;
            }
        }
    }
}
