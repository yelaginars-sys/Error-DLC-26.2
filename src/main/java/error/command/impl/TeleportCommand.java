package error.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import error.Client;
import error.command.Command;
import error.util.client.persiki.ChatUtil;
import error.util.player.TeleportUtil;

import java.util.List;

public class TeleportCommand extends Command {

    public TeleportCommand() {
        super("teleport", "Пакетная телепортация к координатам или игроку (в режиме полёта)");
    }

    @Override
    public List<String> aliases() {
        return List.of("tp");
    }

    private String getPrefix() {
        if (Client.INSTANCE != null && Client.INSTANCE.commandManager != null) {
            return Client.INSTANCE.commandManager.getPrefix();
        }
        return ".";
    }

    @Override
    public void build(LiteralArgumentBuilder<Object> builder) {
        builder.executes(context -> {
            String prefix = getPrefix();
            ChatUtil.info("Использование команды телепорта:");
            ChatUtil.entry(prefix + "tp <x> <z>", "Телепортация на координаты X Z");
            ChatUtil.entry(prefix + "tp <ник>", "Телепортация к игроку");
            return 1;
        });

        // .tp <x> <z>
        RequiredArgumentBuilder<Object, Integer> xArg = RequiredArgumentBuilder.argument("x", IntegerArgumentType.integer());
        RequiredArgumentBuilder<Object, Integer> zArg = RequiredArgumentBuilder.argument("z", IntegerArgumentType.integer());
        zArg.executes(context -> {
            int x = IntegerArgumentType.getInteger(context, "x");
            int z = IntegerArgumentType.getInteger(context, "z");
            TeleportUtil.teleportTo(x, z, false);
            return 1;
        });
        xArg.then(zArg);
        builder.then(xArg);

        // .tp <player>
        RequiredArgumentBuilder<Object, String> playerArg = RequiredArgumentBuilder.argument("player", StringArgumentType.word());
        playerArg.executes(context -> {
            String player = StringArgumentType.getString(context, "player");
            TeleportUtil.teleportToPlayer(player);
            return 1;
        });
        builder.then(playerArg);
    }
}
