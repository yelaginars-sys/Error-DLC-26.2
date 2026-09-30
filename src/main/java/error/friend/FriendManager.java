package error.friend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

public class FriendManager {
    private static final FriendManager INSTANCE = new FriendManager();
    private final Set<String> friends = new HashSet<>();
    private final Preferences prefs = Preferences.userRoot().node("ErrorDLC/friends");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public FriendManager() {
        load();
    }

    public static FriendManager getInstance() {
        return INSTANCE;
    }

    public void addFriend(String name) {
        friends.add(name.toLowerCase());
        save();
    }

    public void removeFriend(String name) {
        friends.remove(name.toLowerCase());
        save();
    }

    public boolean isFriend(String name) {
        if (name == null) return false;
        return friends.contains(name.toLowerCase());
    }

    public boolean isFriend(Player player) {
        if (player == null || player.getGameProfile() == null) return false;
        return isFriend(player.getGameProfile().name());
    }

    public boolean toggle(String name) {
        if (isFriend(name)) {
            removeFriend(name);
            return false;
        } else {
            addFriend(name);
            return true;
        }
    }

    public void save() {
        try {
            prefs.put("friends_json", gson.toJson(friends));
            prefs.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            String json = prefs.get("friends_json", null);
            if (json == null || json.isEmpty()) return;
            Set<String> loaded = gson.fromJson(json, new TypeToken<Set<String>>(){}.getType());
            if (loaded != null) {
                friends.clear();
                for (String s : loaded) {
                    friends.add(s.toLowerCase());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<String> getFriends() {
        return friends;
    }
}