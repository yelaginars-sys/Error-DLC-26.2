package error.util.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class Spectator {

    private static Entity target;

    private Spectator() {
    }

    public static boolean isActive() {
        return target != null;
    }

    public static Entity getTarget() {
        return target;
    }

    public static boolean start(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (entity == null || mc.player == null || mc.level == null) return false;
        if (entity == mc.player || entity.isRemoved()) return false;

        target = entity;
        mc.setCameraEntity(entity);

        return true;
    }

    public static boolean isTerrainLoaded(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (entity == null || mc.level == null) return false;
        ChunkPos pos = entity.chunkPosition();
        return mc.level.getChunkSource().getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false) != null;
    }

    public static int loadedChunksAround(Entity entity, int radius) {
        Minecraft mc = Minecraft.getInstance();
        if (entity == null || mc.level == null) return 0;

        ChunkPos center = entity.chunkPosition();
        int loaded = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (mc.level.getChunkSource().getChunk(center.x() + x, center.z() + z, ChunkStatus.FULL, false) != null) {
                    loaded++;
                }
            }
        }
        return loaded;
    }

    public static int chunksInSquare(int radius) {
        int side = radius * 2 + 1;
        return side * side;
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();
        target = null;
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
    }

    public static void reset() {
        target = null;
    }

    public static boolean validate() {
        if (target == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null
                || target.isRemoved()
                || target.level() != mc.level
                || (target instanceof LivingEntity living && !living.isAlive())) {
            stop();
            return true;
        }

        if (mc.getCameraEntity() != target) {
            mc.setCameraEntity(target);
        }
        return false;
    }
}
