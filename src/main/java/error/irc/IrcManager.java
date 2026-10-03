package error.irc;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import error.util.client.clients.ColorUtil;
import error.util.client.persiki.ChatUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class IrcManager {
    private static final IrcManager INSTANCE = new IrcManager();

    private final List<IrcUser> onlineUsers = new CopyOnWriteArrayList<>();
    private final List<IrcMessage> chatMessages = new CopyOnWriteArrayList<>();

    @Setter
    private boolean enabled = true;

    private IrcManager() {
        initDefaultUsers();
    }

    public static IrcManager getInstance() {
        return INSTANCE;
    }

    private void initDefaultUsers() {
        onlineUsers.add(new IrcUser("walfini", "Dev", ColorUtil.rgba(255, 85, 85, 255), true));
        onlineUsers.add(new IrcUser("Vasya", "VIP", ColorUtil.rgba(255, 170, 0, 255), true));
        onlineUsers.add(new IrcUser("LumenUser", "User", ColorUtil.rgba(85, 255, 255, 255), true));
    }

    public void updateSelfPresence() {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getUser() != null) {
            String name = mc.getUser().getName();
            if (getIrcUser(name) == null) {
                onlineUsers.add(new IrcUser(name, "User", ColorUtil.rgba(85, 255, 85, 255), true));
            }
        }
    }

    public IrcUser getIrcUser(String username) {
        if (username == null) return null;
        for (IrcUser user : onlineUsers) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    public boolean isIrcUser(String username) {
        return getIrcUser(username) != null;
    }

    public void sendIrcMessage(String text) {
        if (!enabled || text == null || text.trim().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        String name = mc.getUser() != null ? mc.getUser().getName() : "Unknown";
        IrcUser self = getIrcUser(name);
        if (self == null) {
            self = new IrcUser(name, "User", ColorUtil.rgba(85, 255, 85, 255), true);
            onlineUsers.add(self);
        }

        IrcMessage msg = new IrcMessage(self, text, System.currentTimeMillis());
        chatMessages.add(msg);
        if (text.startsWith("[COSMETIC] ")) {
            processIrcMessage(self.getUsername(), text);
        } else {
            ChatUtil.info("[IRC] " + self.getUsername() + " (" + self.getRole() + "): " + text);
        }
    }

    public void broadcastCosmetics() {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        error.module.impl.render.CustomModels cm = error.module.impl.render.CustomModels.INSTANCE;
        String model = (cm != null && cm.isState()) ? cm.model.getValue() : "None";
        String verityFace = cm != null ? cm.face.getValue() : "verity";
        String cape = "ErrorCape";

        String payload = "[COSMETIC] " + mc.player.getUUID() + " " + mc.getUser().getName() + " " + model + " " + cape + " " + verityFace;
        error.cosmetic.IrcCosmetics.updateCosmetics(mc.player.getUUID(), model, cape, verityFace);
    }

    public void processIrcMessage(String senderName, String message) {
        if (message == null || !message.startsWith("[COSMETIC] ")) return;
        String[] parts = message.substring(11).split(" ");
        if (parts.length >= 3) {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(parts[0]);
                String model = parts[2];
                String cape = parts.length > 3 ? parts[3] : "ErrorCape";
                String verityFace = parts.length > 4 ? parts[4] : "verity";
                error.cosmetic.IrcCosmetics.updateCosmetics(uuid, model, cape, verityFace);
            } catch (Exception ignored) {}
        }
    }
}
