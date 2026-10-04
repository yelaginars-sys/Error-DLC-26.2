package error.mixin.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void alignPauseButtons(CallbackInfo ci) {
        float centerX = this.width / 2.0F;
        float centerY = this.height / 2.0F;

        AbstractWidget returnButton = null;
        AbstractWidget advancementsButton = null;
        AbstractWidget statsButton = null;
        AbstractWidget feedbackButton = null;
        AbstractWidget bugsButton = null;
        AbstractWidget optionsButton = null;
        AbstractWidget lanButton = null;
        AbstractWidget quitButton = null;

        for (Object child : this.children()) {
            if (child instanceof AbstractWidget widget) {
                String text = widget.getMessage() != null ? widget.getMessage().getString().toLowerCase() : "";

                if (text.contains("вернуться") || text.contains("back to game") || text.contains("return")) {
                    returnButton = widget;
                } else if (text.contains("достижения") || text.contains("advancements")) {
                    advancementsButton = widget;
                } else if (text.contains("статистика") || text.contains("stats")) {
                    statsButton = widget;
                } else if (text.contains("отзыв") || text.contains("feedback")) {
                    feedbackButton = widget;
                } else if (text.contains("ошибку") || text.contains("report") || text.contains("bugs")) {
                    bugsButton = widget;
                } else if (text.contains("настройки") || text.contains("options")) {
                    optionsButton = widget;
                } else if (text.contains("открыть") || text.contains("lan") || text.contains("открытый")) {
                    lanButton = widget;
                } else if (text.contains("сохранить") || text.contains("выйти") || text.contains("disconnect") || text.contains("quit")) {
                    quitButton = widget;
                } else if (text.contains("друзья") || text.contains("жалобы") || text.contains("friends") || text.contains("reports")) {
                    widget.visible = false;
                }
            }
        }

        float wFull = 210.0F;
        float wHalf = 102.0F;
        float h = 20.0F;
        float gap = 5.0F;
        float startY = centerY - 52.0F;

        // 1. Return to Game
        if (returnButton != null) {
            returnButton.setX((int) (centerX - wFull / 2.0F));
            returnButton.setY((int) startY);
            returnButton.setWidth((int) wFull);
            returnButton.setHeight((int) h);
        }

        // 2. Advancements & Stats
        float row1Y = startY + h + gap;
        if (advancementsButton != null) {
            advancementsButton.setX((int) (centerX - wFull / 2.0F));
            advancementsButton.setY((int) row1Y);
            advancementsButton.setWidth((int) wHalf);
            advancementsButton.setHeight((int) h);
        }
        if (statsButton != null) {
            statsButton.setX((int) (centerX + wFull / 2.0F - wHalf));
            statsButton.setY((int) row1Y);
            statsButton.setWidth((int) wHalf);
            statsButton.setHeight((int) h);
        }

        // 3. Feedback & Bugs
        float row2Y = row1Y + h + gap;
        if (feedbackButton != null) {
            feedbackButton.setX((int) (centerX - wFull / 2.0F));
            feedbackButton.setY((int) row2Y);
            feedbackButton.setWidth((int) wHalf);
            feedbackButton.setHeight((int) h);
        }
        if (bugsButton != null) {
            bugsButton.setX((int) (centerX + wFull / 2.0F - wHalf));
            bugsButton.setY((int) row2Y);
            bugsButton.setWidth((int) wHalf);
            bugsButton.setHeight((int) h);
        }

        // 4. Options & Open to LAN
        float row3Y = (feedbackButton != null || bugsButton != null) ? (row2Y + h + gap) : row2Y;
        if (optionsButton != null) {
            optionsButton.setX((int) (centerX - wFull / 2.0F));
            optionsButton.setY((int) row3Y);
            optionsButton.setWidth((int) wHalf);
            optionsButton.setHeight((int) h);
        }
        if (lanButton != null) {
            lanButton.setX((int) (centerX + wFull / 2.0F - wHalf));
            lanButton.setY((int) row3Y);
            lanButton.setWidth((int) wHalf);
            lanButton.setHeight((int) h);
        }

        // 5. Save & Quit
        float row4Y = row3Y + h + gap;
        if (quitButton != null) {
            quitButton.setX((int) (centerX - wFull / 2.0F));
            quitButton.setY((int) row4Y);
            quitButton.setWidth((int) wFull);
            quitButton.setHeight((int) h);
        }
    }
}
