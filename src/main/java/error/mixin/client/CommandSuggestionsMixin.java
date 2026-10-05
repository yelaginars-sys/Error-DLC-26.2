package error.mixin.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import error.Client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {

    @Shadow @Final EditBox input;
    @Shadow @Final Minecraft minecraft;
    @Shadow private ParseResults<ClientSuggestionProvider> currentParse;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private CommandSuggestions.SuggestionsList suggestions;
    @Shadow private boolean currentParseIsCommand;
    @Shadow private boolean currentParseIsMessage;
    @Shadow private boolean keepSuggestions;
    @Shadow private boolean allowSuggestions;
    @Shadow private boolean onlyShowIfCursorPastError;
    @Shadow @Final private List<FormattedCharSequence> commandUsage;
    @Shadow private int commandUsagePosition;
    @Shadow private int commandUsageWidth;

    @Shadow public abstract void showSuggestions(boolean immediateNarration);
    @Shadow protected abstract void recomputeUsageBoxWidth();

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void onUpdateCommandInfo(CallbackInfo ci) {
        String command = this.input.getValue();
        if (Client.INSTANCE == null || Client.INSTANCE.commandManager == null) {
            return;
        }

        String prefix = Client.INSTANCE.commandManager.getPrefix();
        if (command == null || !command.startsWith(prefix) || error.module.impl.misc.UnHook.unhooked) {
            return;
        }

        ci.cancel();

        if (this.currentParse != null && !this.currentParse.getReader().getString().equals(command)) {
            this.currentParse = null;
            this.currentParseIsCommand = false;
            this.currentParseIsMessage = false;
        }

        if (!this.keepSuggestions) {
            this.input.setSuggestion(null);
            this.suggestions = null;
        }

        this.commandUsage.clear();
        StringReader reader = new StringReader(command);
        reader.skip(); // skip prefix (e.g. '.')

        int cursorPosition = this.input.getCursorPosition();
        @SuppressWarnings("unchecked")
        CommandDispatcher<ClientSuggestionProvider> clientCommands =
                (CommandDispatcher<ClientSuggestionProvider>) (Object) Client.INSTANCE.commandManager.getDispatcher();

        ClientSuggestionProvider provider = this.minecraft.player != null && this.minecraft.player.connection != null
                ? this.minecraft.player.connection.getSuggestionsProvider()
                : null;

        if (this.currentParse == null) {
            this.currentParse = clientCommands.parse(reader, provider);
            this.currentParseIsCommand = true;
            this.currentParseIsMessage = false;
        }

        int parseStart = this.onlyShowIfCursorPastError ? reader.getCursor() : 1;
        if (cursorPosition >= parseStart && (this.suggestions == null || !this.keepSuggestions)) {
            this.pendingSuggestions = clientCommands.getCompletionSuggestions(this.currentParse, cursorPosition);
            this.pendingSuggestions.thenAccept(suggestionResult -> {
                if (this.pendingSuggestions.isDone()) {
                    updateClientUsageInfo(this.currentParse, suggestionResult, clientCommands, provider);
                }
            });
        }
    }

    private void updateClientUsageInfo(ParseResults<ClientSuggestionProvider> currentParse, Suggestions suggestions,
                                      CommandDispatcher<ClientSuggestionProvider> clientCommands, ClientSuggestionProvider provider) {
        boolean trailingCharacters = false;
        if (this.input.getCursorPosition() == this.input.getValue().length()) {
            if (suggestions.isEmpty() && !currentParse.getExceptions().isEmpty()) {
                int literals = 0;
                for (Map.Entry<CommandNode<ClientSuggestionProvider>, CommandSyntaxException> entry : currentParse.getExceptions().entrySet()) {
                    CommandSyntaxException exception = entry.getValue();
                    if (exception.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect()) {
                        literals++;
                    } else {
                        this.commandUsage.add(formatException(exception));
                    }
                }
                if (literals > 0) {
                    this.commandUsage.add(formatException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(currentParse.getReader())));
                }
            } else if (currentParse.getReader().canRead()) {
                trailingCharacters = true;
            }
        }

        SuggestionContext<ClientSuggestionProvider> suggestionContextAtCursor = currentParse.getContext().findSuggestionContext(this.input.getCursorPosition());
        if (this.commandUsage.isEmpty()) {
            if (provider != null) {
                Map<CommandNode<ClientSuggestionProvider>, String> usage = clientCommands.getSmartUsage(suggestionContextAtCursor.parent, provider);
                for (Map.Entry<CommandNode<ClientSuggestionProvider>, String> entry : usage.entrySet()) {
                    if (!(entry.getKey() instanceof LiteralCommandNode)) {
                        this.commandUsage.add(FormattedCharSequence.forward(entry.getValue(), CommandSuggestions.USAGE_FORMAT));
                    }
                }
            }
            if (this.commandUsage.isEmpty() && trailingCharacters) {
                this.commandUsage.add(formatException(Commands.getParseException(currentParse)));
            }
        }

        this.recomputeUsageBoxWidth();
        if (!this.commandUsage.isEmpty()) {
            this.commandUsagePosition = Mth.clamp(
                    this.input.getScreenX(suggestionContextAtCursor.startPos),
                    0,
                    this.input.getScreenX(0) + this.input.getInnerWidth() - this.commandUsageWidth
            );
        } else {
            this.commandUsagePosition = 0;
        }

        this.suggestions = null;
        if (this.allowSuggestions && (Boolean) this.minecraft.options.autoSuggestions().get()) {
            this.showSuggestions(false);
        }
    }

    private static FormattedCharSequence formatException(CommandSyntaxException e) {
        Component message = ComponentUtils.fromMessage(e.getRawMessage());
        String context = e.getContext();
        return context == null
                ? message.getVisualOrderText()
                : Component.translatable("command.context.parse_error", message, e.getCursor(), context).getVisualOrderText();
    }

    @ModifyVariable(method = "sortSuggestions", at = @At("STORE"), ordinal = 0)
    private String modifyLastWordForClientCommands(String lastWord) {
        if (lastWord != null && lastWord.startsWith(".")) {
            return lastWord.substring(1);
        }
        return lastWord;
    }
}
