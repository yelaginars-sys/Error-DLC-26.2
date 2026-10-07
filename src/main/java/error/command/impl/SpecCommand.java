package error.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import error.command.Command;
import error.util.client.persiki.ChatUtil;
import error.util.player.Spectator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SpecCommand extends Command {

    private static final int CHECK_RADIUS = 2;

    public SpecCommand() {
        super("spec", "Вселиться в сущность или игрока (клиентский спектатор)");
    }

    @Override
    public List<String> aliases() {
        return List.of("spectate", "spectator");
    }

    @Override
    public void build(LiteralArgumentBuilder<Object> builder) {
        SuggestionProvider<Object> playerSuggestions = (context, suggestionsBuilder) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                String remaining = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
                for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                    String name = info.getProfile().name();
                    if (!name.equalsIgnoreCase(mc.player != null ? mc.player.getName().getString() : "") &&
                            name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                        suggestionsBuilder.suggest(name);
                    }
                }
            }
            suggestionsBuilder.suggest("off");
            suggestionsBuilder.suggest("stop");
            suggestionsBuilder.suggest("next");
            suggestionsBuilder.suggest("prev");
            suggestionsBuilder.suggest("info");
            return suggestionsBuilder.buildFuture();
        };

        builder.executes(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return 0;

            if (Spectator.isActive()) {
                stopSpectate();
                return 1;
            }

            Entity looked = mc.crosshairPickEntity;
            if (looked == null || looked == mc.player) {
                ChatUtil.error("Посмотрите на сущность или укажите ник: .spec <ник>");
                return 0;
            }
            attachSpectate(looked);
            return 1;
        });

        builder.then(LiteralArgumentBuilder.literal("off").executes(context -> {
            stopSpectate();
            return 1;
        }));
        builder.then(LiteralArgumentBuilder.literal("stop").executes(context -> {
            stopSpectate();
            return 1;
        }));
        builder.then(LiteralArgumentBuilder.literal("exit").executes(context -> {
            stopSpectate();
            return 1;
        }));
        builder.then(LiteralArgumentBuilder.literal("next").executes(context -> {
            cycleSpectate(1);
            return 1;
        }));
        builder.then(LiteralArgumentBuilder.literal("prev").executes(context -> {
            cycleSpectate(-1);
            return 1;
        }));
        builder.then(LiteralArgumentBuilder.literal("info").executes(context -> {
            showInfo();
            return 1;
        }));

        builder.then(RequiredArgumentBuilder.<Object, String>argument("target", StringArgumentType.word())
                .suggests(playerSuggestions)
                .executes(context -> {
                    String arg = StringArgumentType.getString(context, "target");
                    if ("off".equalsIgnoreCase(arg) || "stop".equalsIgnoreCase(arg) || "exit".equalsIgnoreCase(arg)) {
                        stopSpectate();
                        return 1;
                    }
                    if ("next".equalsIgnoreCase(arg)) {
                        cycleSpectate(1);
                        return 1;
                    }
                    if ("prev".equalsIgnoreCase(arg)) {
                        cycleSpectate(-1);
                        return 1;
                    }
                    if ("info".equalsIgnoreCase(arg)) {
                        showInfo();
                        return 1;
                    }

                    Entity found = findEntity(arg);
                    if (found == null) {
                        ChatUtil.error("Сущность или игрок '" + arg + "' не найдены!");
                        return 0;
                    }
                    attachSpectate(found);
                    return 1;
                }));
    }

    private void attachSpectate(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player) return;

        if (!Spectator.start(entity)) {
            ChatUtil.error("Не удалось начать наблюдение за " + entity.getName().getString());
            return;
        }
        ChatUtil.success("Наблюдение за §f" + entity.getName().getString() + " §7(" + terrainStatus(entity) + "§7)");
    }

    private String terrainStatus(Entity entity) {
        int total = Spectator.chunksInSquare(CHECK_RADIUS);
        int loaded = Spectator.loadedChunksAround(entity, CHECK_RADIUS);
        String color = loaded == total ? "§a" : loaded == 0 ? "§c" : "§e";
        return color + loaded + "§7/§f" + total + " §7чанков";
    }

    private void showInfo() {
        Entity entity = Spectator.getTarget();
        if (entity == null) {
            ChatUtil.error("Режим спектатора не активен.");
            return;
        }
        ChatUtil.info("Спектатор за: §a" + entity.getName().getString() + " §7(" + terrainStatus(entity) + "§7)");
    }

    private void stopSpectate() {
        if (!Spectator.isActive()) {
            ChatUtil.error("Режим спектатора не активен.");
            return;
        }
        Spectator.stop();
        ChatUtil.info("Режим спектатора §cвыключен§7.");
    }

    private Entity findEntity(String query) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        String lower = query.toLowerCase(Locale.ROOT);

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (player.getName().getString().equalsIgnoreCase(query)) return player;
        }
        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (player.getName().getString().toLowerCase(Locale.ROOT).startsWith(lower)) return player;
        }

        try {
            Entity byId = mc.level.getEntity(Integer.parseInt(query));
            if (byId != null && byId != mc.player) return byId;
        } catch (NumberFormatException ignored) {
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            if (entity.getName().getString().toLowerCase(Locale.ROOT).startsWith(lower)) return entity;
        }
        return null;
    }

    private void cycleSpectate(int direction) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        List<Player> candidates = new ArrayList<>(mc.level.players().stream()
                .filter(p -> p != mc.player)
                .sorted(Comparator.comparingDouble(p -> p.distanceToSqr(mc.player)))
                .toList());

        if (candidates.isEmpty()) {
            ChatUtil.error("Рядом нет игроков для переключения!");
            return;
        }

        int index = candidates.indexOf(Spectator.getTarget());
        int next = index < 0
                ? (direction > 0 ? 0 : candidates.size() - 1)
                : Math.floorMod(index + direction, candidates.size());

        attachSpectate(candidates.get(next));
    }
}
