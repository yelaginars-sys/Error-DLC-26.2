package dev.syntrix.clienttest.client.visual;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import java.nio.file.*;
import java.util.*;

/** Local friend names. Checks both local list and Error DLC FriendManager. */
public final class VisualFriends {
    private static final Set<String> names = new TreeSet<>();
    private static boolean readable = true;
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("noctra-crystalaura/friends.json");

    public static boolean contains(Player player) {
        if (player == null) return false;
        if (error.friend.FriendManager.getInstance() != null && error.friend.FriendManager.getInstance().isFriend(player)) {
            return true;
        }
        return names.contains(player.getGameProfile().name().toLowerCase(Locale.ROOT));
    }

    public static void initialize() {
        try {
            if (Files.exists(FILE)) {
                var array = JsonParser.parseString(Files.readString(FILE)).getAsJsonArray();
                for (var name : array) {
                    String text = name.getAsString();
                    if (!text.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException();
                    names.add(text.toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception error) {
            names.clear();
            readable = false;
            org.slf4j.LoggerFactory.getLogger("noctra-visual").warn("Cannot read friends.json; preserving the file", error);
        }
    }

    public static void addFriend(String name) {
        names.add(name.toLowerCase(Locale.ROOT));
        save();
    }

    public static void removeFriend(String name) {
        names.remove(name.toLowerCase(Locale.ROOT));
        save();
    }

    private static boolean save() {
        if (!readable) return false;
        Path temporary = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), ".friends-", ".tmp");
            Files.writeString(temporary, new Gson().toJson(names));
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (Exception error) {
            org.slf4j.LoggerFactory.getLogger("noctra-visual").warn("Could not save friends.json", error);
            return false;
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (Exception ignored) {}
        }
    }

    private VisualFriends() {}
}
