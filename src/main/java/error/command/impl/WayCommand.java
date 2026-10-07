package error.command.impl;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import error.IMinecraft;
import error.command.Command;
import error.event.EventTarget;
import error.event.list.Render2DEvent;
import error.util.client.persiki.ChatUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.world.phys.Vec3;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WayCommand extends Command implements IMinecraft {

    private static final Path FILE = Path.of("error/waypoints.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record Waypoint(String name, Vec3 pos) {}

    public static final Map<String, Waypoint> waypoints = new ConcurrentHashMap<>();

    public WayCommand() {
        super("way", "Вейпоинты — метки на координатах");
        load();
    }

    @Override
    public List<String> aliases() {
        return List.of("waypoint", "waypoints");
    }

    @Override
    public void build(LiteralArgumentBuilder<Object> builder) {
        builder.then(LiteralArgumentBuilder.<Object>literal("add")
                .then(RequiredArgumentBuilder.<Object, String>argument("name", StringArgumentType.word())
                        .executes(c -> {
                            String name = StringArgumentType.getString(c, "name");
                            if (mc.player == null) return 0;
                            Vec3 pos = mc.player.position();
                            boolean replaced = waypoints.put(name.toLowerCase(), new Waypoint(name, pos)) != null;
                            save();
                            ChatUtil.print("§aВейпоинт §f" + name + " §a" + (replaced ? "обновлен" : "добавлен") + " на §f" + (int)pos.x + " " + (int)pos.y + " " + (int)pos.z + "§a!");
                            return 1;
                        })))
                .then(LiteralArgumentBuilder.<Object>literal("delete")
                        .then(RequiredArgumentBuilder.<Object, String>argument("name", StringArgumentType.word())
                                .executes(c -> {
                                    String name = StringArgumentType.getString(c, "name");
                                    if (waypoints.remove(name.toLowerCase()) != null) {
                                        save();
                                        ChatUtil.print("§aВейпоинт §f" + name + " §aудален!");
                                    } else {
                                        ChatUtil.print("§cВейпоинт §f" + name + " §cне найден!");
                                    }
                                    return 1;
                                })))
                .then(LiteralArgumentBuilder.<Object>literal("clear")
                        .executes(c -> {
                            int count = waypoints.size();
                            waypoints.clear();
                            save();
                            ChatUtil.print("§aУдалено вейпоинтов: §f" + count);
                            return 1;
                        }))
                .then(LiteralArgumentBuilder.<Object>literal("list")
                        .executes(c -> {
                            if (waypoints.isEmpty()) {
                                ChatUtil.print("§7Список вейпоинтов пуст!");
                                return 1;
                            }
                            ChatUtil.print("§7Вейпоинты (" + waypoints.size() + "):");
                            for (Waypoint w : waypoints.values()) {
                                ChatUtil.print("§8 · §a" + w.name() + " §7(X: " + (int)w.pos().x + " Y: " + (int)w.pos().y + " Z: " + (int)w.pos().z + ")");
                            }
                            return 1;
                        }));
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.level == null || waypoints.isEmpty()) return;

        Vec3 camPos = mc.gameRenderer.mainCamera().position();

        for (Waypoint w : waypoints.values()) {
            float dist = (float) camPos.distanceTo(w.pos());

            String text = w.name().replace('_', ' ') + " " + (int) dist + "м";
            float wr = Fonts.SF_MEDIUM.getWidth(text, 8.0f) + 10;
            float h = 14;

            float x = mc.getWindow().getGuiScaledWidth() / 2.0f - wr / 2.0f;
            float y = mc.getWindow().getGuiScaledHeight() / 2.0f - 40;

            Render2D.drawBlur(x, y, wr, h, 6.0f, 0x80FFFFFF, 0.85f);
            Render2D.drawRoundedOutline(x, y, wr, h, 6.0f, 1.0f, 0x50FFFFFF);

            Fonts.drawString(Fonts.SF_MEDIUM, text, x + 5, y + 3, 8.0f, 0xFFFFFFFF);
        }
    }

    private static void save() {
        JsonArray arr = new JsonArray();
        for (Waypoint w : waypoints.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", w.name());
            obj.addProperty("x", w.pos().x);
            obj.addProperty("y", w.pos().y);
            obj.addProperty("z", w.pos().z);
            arr.add(obj);
        }
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer wtr = new OutputStreamWriter(new FileOutputStream(FILE.toFile()), StandardCharsets.UTF_8)) {
                GSON.toJson(arr, wtr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader r = new InputStreamReader(new FileInputStream(FILE.toFile()), StandardCharsets.UTF_8)) {
            JsonArray arr = JsonParser.parseReader(r).getAsJsonArray();
            waypoints.clear();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String name = obj.get("name").getAsString();
                Vec3 pos = new Vec3(
                        obj.get("x").getAsDouble(),
                        obj.get("y").getAsDouble(),
                        obj.get("z").getAsDouble());
                waypoints.put(name.toLowerCase(), new Waypoint(name, pos));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
