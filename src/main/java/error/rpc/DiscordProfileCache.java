package error.rpc;

public class DiscordProfileCache {

    private static volatile String username = "";
    private static volatile String userId = "";
    private static volatile String avatar = "";
    private static volatile String avatarUrl = "";

    public static void setUser(String name, String id, String av) {
        if (name != null && !name.isEmpty()) {
            username = name;
        }
        if (id != null && !id.isEmpty()) {
            userId = id;
        }
        if (av != null) {
            avatar = av;
        }

        if (!avatar.isEmpty() && !userId.isEmpty()) {
            avatarUrl = "https://cdn.discordapp.com/avatars/" + userId + "/" + avatar + ".png?size=64";
        } else if (!userId.isEmpty()) {
            try {
                long idLong = Long.parseLong(userId);
                int defaultIndex = (int) ((idLong >> 22) % 6);
                avatarUrl = "https://cdn.discordapp.com/embed/avatars/" + defaultIndex + ".png";
            } catch (Exception e) {
                avatarUrl = "https://cdn.discordapp.com/embed/avatars/0.png";
            }
        } else {
            avatarUrl = "";
        }
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String name) {
        username = name;
    }

    public static String getUserId() {
        return userId;
    }

    public static String getAvatar() {
        return avatar;
    }

    public static String getAvatarUrl() {
        return avatarUrl;
    }

    public static void reset() {
        username = "";
        userId = "";
        avatar = "";
        avatarUrl = "";
    }
}
