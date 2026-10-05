package error.staff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.prefs.Preferences;

public class StaffManager {
    private static final StaffManager INSTANCE = new StaffManager();
    private final Set<String> staff = new HashSet<>();
    private final Preferences prefs = Preferences.userRoot().node("ErrorDLC/staff");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final String[] STAFF_PREFIXES = {
            "admin", "adm", "moder", "mod", "helper", "help", "staff", "st",
            "support", "куратор", "админ", "модер", "хелпер", "разраб", "dev",
            "owner", "овнер", "тех", "tech", "спец", "spec", "глав"
    };

    public StaffManager() {
        load();
    }

    public static StaffManager getInstance() {
        return INSTANCE;
    }

    public void addStaff(String name) {
        if (name == null || name.isEmpty()) return;
        staff.add(name.toLowerCase(Locale.ROOT));
        save();
    }

    public void removeStaff(String name) {
        if (name == null || name.isEmpty()) return;
        staff.remove(name.toLowerCase(Locale.ROOT));
        save();
    }

    public boolean isStaff(String name) {
        if (name == null) return false;
        return staff.contains(name.toLowerCase(Locale.ROOT));
    }

    public boolean isStaff(Player player) {
        if (player == null || player.getGameProfile() == null) return false;
        return isStaff(player.getGameProfile().name());
    }

    public static boolean hasStaffPrefix(String nameOrDisplayName) {
        if (nameOrDisplayName == null || nameOrDisplayName.isEmpty()) return false;
        String lower = nameOrDisplayName.toLowerCase(Locale.ROOT);
        for (String prefix : STAFF_PREFIXES) {
            if (lower.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getStaff() {
        return staff;
    }

    public void clear() {
        staff.clear();
        save();
    }

    public void save() {
        try {
            prefs.put("staff_json", gson.toJson(staff));
            prefs.flush();
        } catch (Exception ignored) {}
    }

    public void load() {
        try {
            String json = prefs.get("staff_json", null);
            if (json != null && !json.isEmpty()) {
                Set<String> loaded = gson.fromJson(json, new TypeToken<Set<String>>(){}.getType());
                if (loaded != null) {
                    staff.clear();
                    staff.addAll(loaded);
                }
            }
        } catch (Exception ignored) {}
    }
}
