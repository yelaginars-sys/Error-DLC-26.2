package error.cosmetic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IrcCosmetics {
    public static final Map<UUID, String> USER_MODELS = new ConcurrentHashMap<>();
    public static final Map<UUID, String> USER_CAPES = new ConcurrentHashMap<>();
    public static final Map<UUID, String> USER_VERITY_FACES = new ConcurrentHashMap<>();

    public static void updateCosmetics(UUID uuid, String model, String cape, String verityFace) {
        if (uuid == null) return;
        if (model != null && !model.isEmpty()) USER_MODELS.put(uuid, model);
        if (cape != null && !cape.isEmpty()) USER_CAPES.put(uuid, cape);
        if (verityFace != null && !verityFace.isEmpty()) USER_VERITY_FACES.put(uuid, verityFace);
    }

    public static String getModel(UUID uuid) {
        if (uuid == null) return null;
        return USER_MODELS.get(uuid);
    }

    public static String getVerityFace(UUID uuid) {
        if (uuid == null) return "verity";
        return USER_VERITY_FACES.getOrDefault(uuid, "verity");
    }
}
