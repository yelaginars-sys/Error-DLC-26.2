package error.util.display.head;

import com.mojang.authlib.GameProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;

final class HeadSkins {
    private static final int LIMIT = 128;
    private static final Map<UUID, Supplier<PlayerSkin>> CACHE = new LinkedHashMap<>(32, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Supplier<PlayerSkin>> eldest) {
            return size() > LIMIT;
        }
    };

    private HeadSkins() {
    }

    static Supplier<PlayerSkin> lookup(GameProfile profile) {
        return CACHE.computeIfAbsent(profile.id(), id -> Minecraft.getInstance().getSkinManager().createLookup(profile, false));
    }

    static void clear() {
        CACHE.clear();
    }
}
