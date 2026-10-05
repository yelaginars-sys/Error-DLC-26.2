package error.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.module.impl.render.BetterMinecraft;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;

    @Shadow protected abstract void refreshTrimmedMessages();
    @Shadow protected abstract void addMessage(Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag);

    @Unique private static final Map<String, Integer> error$countMap = new ConcurrentHashMap<>();
    @Unique private static final Map<GuiMessage.Line, Long> error$lineTimestamps = new WeakHashMap<>();
    @Unique private static final Pattern STACK_PATTERN = Pattern.compile("^(.*?)(?:\\s*§7\\(x\\d+\\))+$", Pattern.DOTALL);

    @Unique private boolean error$isModifying = false;

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        // Vanilla chat enabled
    }

    @WrapOperation(method = "forEachLine", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$LineConsumer;accept(Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;IF)V"))
    private void wrapLineConsumerAccept(@Coerce Object consumer, GuiMessage.Line line, int lineIndex, float alpha, Operation<Void> original) {
        original.call(consumer, line, lineIndex, alpha);
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void onClearMessages(boolean history, CallbackInfo ci) {
        error$countMap.clear();
        error$lineTimestamps.clear();
    }

    @Unique
    private static String stripStack(String s) {
        if (s == null) return "";
        Matcher m = STACK_PATTERN.matcher(s.trim());
        if (m.matches()) {
            return m.group(1).trim();
        }
        return s.trim();
    }
}