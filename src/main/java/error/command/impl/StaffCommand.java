package error.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import error.Client;
import error.command.Command;
import error.staff.StaffManager;
import error.util.client.persiki.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.List;
import java.util.Set;

public class StaffCommand extends Command {

    public StaffCommand() {
        super("staff", "Управление списком стаффа");
    }

    @Override
    public List<String> aliases() {
        return List.of("staffs", "st");
    }

    private String getPrefix() {
        if (Client.INSTANCE != null && Client.INSTANCE.commandManager != null) {
            return Client.INSTANCE.commandManager.getPrefix();
        }
        return ".";
    }

    @Override
    public void build(LiteralArgumentBuilder<Object> builder) {
        SuggestionProvider<Object> playerSuggestions = (context, suggestionsBuilder) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                    String name = info.getProfile().name();
                    if (!StaffManager.getInstance().isStaff(name) &&
                            name.toLowerCase().startsWith(suggestionsBuilder.getRemaining().toLowerCase())) {
                        suggestionsBuilder.suggest(name);
                    }
                }
            }
            return suggestionsBuilder.buildFuture();
        };

        SuggestionProvider<Object> staffSuggestions = (context, suggestionsBuilder) -> {
            for (String s : StaffManager.getInstance().getStaff()) {
                if (s.toLowerCase().startsWith(suggestionsBuilder.getRemaining().toLowerCase())) {
                    suggestionsBuilder.suggest(s);
                }
            }
            return suggestionsBuilder.buildFuture();
        };

        builder.executes(context -> {
            String prefix = getPrefix();
            ChatUtil.info("Управление списком стаффа:");
            ChatUtil.entry(prefix + "staff add <ник>", "Добавить игрока в список стаффа");
            ChatUtil.entry(prefix + "staff remove <ник>", "Удалить игрока из стаффа");
            ChatUtil.entry(prefix + "staff list", "Список всех сохраненных стаффов");
            ChatUtil.entry(prefix + "staff clear", "Очистить список стаффа");
            return 1;
        });

        builder.then(LiteralArgumentBuilder.literal("add")
                .then(RequiredArgumentBuilder.<Object, String>argument("name", StringArgumentType.word())
                        .suggests(playerSuggestions)
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            if (StaffManager.getInstance().isStaff(name)) {
                                ChatUtil.info("Игрок '§b" + name + "§r' уже в списке стаффа!");
                                return 0;
                            }
                            StaffManager.getInstance().addStaff(name);
                            ChatUtil.success("Игрок '§a" + name + "§r' успешно добавлен в список стаффа!");
                            return 1;
                        }))
        );

        builder.then(LiteralArgumentBuilder.literal("remove")
                .then(RequiredArgumentBuilder.<Object, String>argument("name", StringArgumentType.word())
                        .suggests(staffSuggestions)
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            if (!StaffManager.getInstance().isStaff(name)) {
                                ChatUtil.error("Игрок '§c" + name + "§r' не найден в списке стаффа!");
                                return 0;
                            }
                            StaffManager.getInstance().removeStaff(name);
                            ChatUtil.success("Игрок '§c" + name + "§r' удален из списка стаффа!");
                            return 1;
                        }))
        );

        builder.then(LiteralArgumentBuilder.literal("list")
                .executes(context -> {
                    Set<String> staff = StaffManager.getInstance().getStaff();
                    if (staff.isEmpty()) {
                        ChatUtil.info("Список стаффа пуст.");
                    } else {
                        ChatUtil.info("Список стаффа (" + staff.size() + "):");
                        for (String s : staff) {
                            ChatUtil.entry(s, "");
                        }
                    }
                    return 1;
                })
        );

        builder.then(LiteralArgumentBuilder.literal("clear")
                .executes(context -> {
                    Set<String> staff = StaffManager.getInstance().getStaff();
                    if (staff.isEmpty()) {
                        ChatUtil.info("Список стаффа уже пуст.");
                    } else {
                        StaffManager.getInstance().clear();
                        ChatUtil.success("Список стаффа успешно очищен!");
                    }
                    return 1;
                })
        );
    }
}
