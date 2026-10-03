package error.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.module.impl.render.BetterMinecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
@Mixin(EditBox.class)
public abstract class EditBoxMixin {

    @Shadow public abstract String getValue();
    @Shadow private int displayPos;

    @Unique private String error$prevValue = "";
    @Unique private final Map<Integer, Long> error$charTimestamps = new HashMap<>();

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    private void onExtractRenderStateHead(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (mod == null || !mod.isEnabled() || !mod.modes.isEnabled("Chat")) return;

        String currentVal = this.getValue();
        if (!currentVal.equals(error$prevValue)) {
            long now = System.currentTimeMillis();

            int len = currentVal.length();
            int prevLen = error$prevValue.length();

            if (len > prevLen) {
                for (int i = prevLen; i < len; i++) {
                    error$charTimestamps.put(i, now);
                }
            } else {
                error$charTimestamps.keySet().removeIf(idx -> idx >= len);
            }

            error$prevValue = currentVal;
        }
    }

    @WrapOperation(
            method = "extractWidgetRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V"
            )
    )
    private void wrapDrawText(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence charSequence, int x, int y, int color, boolean dropShadow, Operation<Void> original) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (mod == null || !mod.isEnabled() || !mod.modes.isEnabled("Chat") ) {
            original.call(graphics, font, charSequence, x, y, color, dropShadow);
            return;
        }

        error$renderAnimatedText(graphics, font, charSequence, x, y, color, dropShadow, original);
    }

    @Unique
    private void error$renderAnimatedText(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence charSequence, int x, int y, int color, boolean dropShadow, Operation<Void> original) {
        List<error$CharData> chars = new ArrayList<>();
        charSequence.accept((charIndex, style, codePoint) -> {
            chars.add(new error$CharData(charIndex, style, codePoint));
            return true;
        });

        int currentX = x;
        long now = System.currentTimeMillis();
        float animDuration = Math.max(10.0F, BetterMinecraft.INSTANCE.animSpeed.getValue());

        for (error$CharData cd : chars) {
            String str = new String(Character.toChars(cd.codePoint));
            int charWidth = font.width(str);
            int globalIndex = this.displayPos + cd.charIndex;

            Long spawnTime = error$charTimestamps.get(globalIndex);
            float progress = (spawnTime == null) ? 1.0F : Math.min(1.0F, (now - spawnTime) / animDuration);

            FormattedCharSequence singleChar = FormattedCharSequence.forward(str, cd.style);

            if (progress < 1.0F) {
                float scale = error$easeOutBack(progress);
                float centerX = currentX + charWidth / 2.0F;
                float centerY = y + 4.5F;

                Matrix3x2fStack pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(centerX, centerY);
                pose.scale(scale, scale);
                pose.translate(-centerX, -centerY);

                original.call(graphics, font, singleChar, currentX, y, color, dropShadow);

                pose.popMatrix();
            } else {
                original.call(graphics, font, singleChar, currentX, y, color, dropShadow);
            }

            currentX += charWidth;
        }
    }

    @Unique
    private static float error$easeOutBack(float x) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    @Unique
    private static class error$CharData {
        final int charIndex;
        final Style style;
        final int codePoint;

        error$CharData(int charIndex, Style style, int codePoint) {
            this.charIndex = charIndex;
            this.style = style;
            this.codePoint = codePoint;
        }
    }
}