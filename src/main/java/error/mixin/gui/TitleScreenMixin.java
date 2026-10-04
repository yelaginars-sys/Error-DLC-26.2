package error.mixin.gui;

import error.ui.account.AccountManagerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Shadow @Nullable private SplashRenderer splash;
    @Unique private Button error$accountButton;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void alignTitleButtons(CallbackInfo ci) {
        // Hide yellow splash text
        this.splash = null;

        float centerX = this.width / 2.0F;
        float startY = this.height / 2.0F - 10.0F;

        AbstractWidget spButton = null;
        AbstractWidget mpButton = null;
        AbstractWidget realmsButton = null;
        AbstractWidget optionsButton = null;
        AbstractWidget quitButton = null;

        for (Object child : this.children()) {
            if (child instanceof AbstractWidget widget) {
                String text = widget.getMessage() != null ? widget.getMessage().getString().toLowerCase() : "";

                // Hide small icon buttons and version/copyright text widgets
                if (widget.getWidth() <= 24 || text.contains("друзья") || text.contains("friends")
                        || text.contains("mojang") || text.contains("распространение") || text.contains("модифицировано")) {
                    widget.visible = false;
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
                }
            }
        }

        if (realmsButton != null) {
            realmsButton.visible = false;
        }

        float w = 204.0F;
        float h = 20.0F;
        float gap = 4.0F;

        // 1. Singleplayer
        if (spButton != null) {
            spButton.setX((int) (centerX - w / 2.0F));
            spButton.setY((int) startY);
            spButton.setWidth((int) w);
            spButton.setHeight((int) h);
        }

        // 2. Multiplayer
        float currentY = startY + h + gap;
        if (mpButton != null) {
            mpButton.setX((int) (centerX - w / 2.0F));
            mpButton.setY((int) currentY);
            mpButton.setWidth((int) w);
            mpButton.setHeight((int) h);
            currentY += h + gap;
        }

        // 3. Account Manager Button (Always re-created to prevent disappearing on screen re-init)
        if (error$accountButton != null) {
            this.removeWidget(error$accountButton);
        }
        error$accountButton = Button.builder(Component.literal("Аккаунт менеджер"), b -> {
            Minecraft.getInstance().setScreenAndShow(new AccountManagerScreen(this));
        }).bounds((int) (centerX - w / 2.0F), (int) currentY, (int) w, (int) h).build();
        this.addRenderableWidget(error$accountButton);

        currentY += h + gap;

        // 4. Options & Quit
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
    }
}
