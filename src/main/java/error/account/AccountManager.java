package error.account;

import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import error.mixin.accessor.MinecraftAccessor;
import error.util.client.persiki.ChatUtil;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class AccountManager {

    private static final AccountManager INSTANCE = new AccountManager();
    public static AccountManager getInstance() { return INSTANCE; }

    private final List<String> accounts = new ArrayList<>();
    private final Set<String> favorites = new LinkedHashSet<>();
    @Setter
    private String activeAccount = "";

    private final Preferences prefs = Preferences.userRoot().node("ErrorDLC/accounts");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ScheduledExecutorService autoSaveExecutor = Executors.newSingleThreadScheduledExecutor();

    public AccountManager() {
        load();

        autoSaveExecutor.scheduleAtFixedRate(() -> {
            try {
                save();
            } catch (Exception ignored) {}
        }, 2, 2, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(this::save));
    }

    public void addAccount(String name) {
        if (name == null || name.trim().isEmpty()) return;
        name = name.trim();

        for (String acc : accounts) {
            if (acc.equalsIgnoreCase(name)) {
                ChatUtil.error("Аккаунт '§c" + name + "§r' уже существует!");
                return;
            }
        }

        accounts.add(name);
        if (accounts.size() == 1 || activeAccount.isEmpty()) {
            activeAccount = name;
            applyActiveSession();
        }
        save();
        ChatUtil.success("Аккаунт '§a" + name + "§r' успешно добавлен!");
    }

    public void removeAccount(String name) {
        if (name == null) return;
        boolean removed = accounts.removeIf(a -> a.equalsIgnoreCase(name));
        favorites.removeIf(a -> a.equalsIgnoreCase(name));

        if (removed) {
            if (activeAccount.equalsIgnoreCase(name)) {
                activeAccount = accounts.isEmpty() ? "" : accounts.get(0);
                applyActiveSession();
            }
            save();
            ChatUtil.success("Аккаунт '§c" + name + "§r' удален!");
        } else {
            ChatUtil.error("Аккаунт '§c" + name + "§r' не найден!");
        }
    }

    public void removeAll() {
        accounts.clear();
        favorites.clear();
        activeAccount = "";
        save();
        ChatUtil.info("Все аккаунты успешно удалены!");
    }

    public void randomAccount() {
        String[] prefixes = {"User", "Player", "Shadow", "Night", "Frost", "Ghost", "Storm", "Viper", "Echo", "Blade"};
        String rnd = prefixes[new Random().nextInt(prefixes.length)] + "_" + (1000 + new Random().nextInt(9000));
        addAccount(rnd);
        selectAccount(rnd);
    }

    public void toggleFavorite(String name) {
        if (name == null || name.trim().isEmpty()) return;
        final String targetName = name.trim();
        if (isFavorite(targetName)) {
            favorites.removeIf(a -> a.equalsIgnoreCase(targetName));
            ChatUtil.info("Аккаунт '§7" + targetName + "§r' удален из избранного.");
        } else {
            favorites.add(targetName);
            ChatUtil.success("Аккаунт '§a" + targetName + "§r' добавлен в избранное!");
        }
        save();
    }

    public boolean isFavorite(String name) {
        if (name == null) return false;
        for (String fav : favorites) {
            if (fav.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public void selectAccount(String name) {
        setSession(name);
    }

    public void setSession(String name) {
        if (name == null || name.trim().isEmpty()) return;

        boolean exists = accounts.stream().anyMatch(a -> a.equalsIgnoreCase(name));
        if (!exists) {
            accounts.add(name.trim());
        }

        this.activeAccount = name.trim();
        applyActiveSession();
        save();
    }

    public void relogin(String name) {
        setSession(name);
    }

    public List<String> getSortedAccounts() {
        List<String> sorted = new ArrayList<>();
        for (String fav : favorites) {
            for (String acc : accounts) {
                if (acc.equalsIgnoreCase(fav) && !sorted.contains(acc)) {
                    sorted.add(acc);
                }
            }
        }
        for (String acc : accounts) {
            if (!sorted.contains(acc)) {
                sorted.add(acc);
            }
        }
        return sorted;
    }

    public void applyActiveSession() {
        if (activeAccount == null || activeAccount.trim().isEmpty()) return;

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;

            String username = activeAccount.trim();
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            User newSession = new User(username, uuid, "", Optional.empty(), Optional.empty());

            ((MinecraftAccessor) mc).setUser(newSession);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getAccounts() {
        return new ArrayList<>(accounts);
    }

    public String getActiveAccount() {
        return activeAccount;
    }

    public void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("active", this.activeAccount != null ? this.activeAccount : "");

            JsonArray accArray = new JsonArray();
            for (String acc : accounts) {
                accArray.add(acc);
            }
            root.add("accounts", accArray);

            JsonArray favArray = new JsonArray();
            for (String fav : favorites) {
                favArray.add(fav);
            }
            root.add("favorites", favArray);

            String json = gson.toJson(root);
            autoSaveExecutor.submit(() -> {
                try {
                    prefs.put("accounts_json", json);
                    prefs.flush();
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            String json = prefs.get("accounts_json", null);
            if (json == null || json.isEmpty()) return;

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            accounts.clear();
            favorites.clear();

            if (root.has("accounts")) {
                for (JsonElement el : root.getAsJsonArray("accounts")) {
                    accounts.add(el.getAsString());
                }
            }

            if (root.has("favorites")) {
                for (JsonElement el : root.getAsJsonArray("favorites")) {
                    favorites.add(el.getAsString());
                }
            }

            if (root.has("active")) {
                this.activeAccount = root.get("active").getAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}