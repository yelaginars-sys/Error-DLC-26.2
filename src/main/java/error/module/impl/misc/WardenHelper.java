package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.GameTickEvent;
import error.event.list.Render3DEvent;
import error.event.list.WorldLeaveEvent;
import error.module.Category;
import error.module.Module;
import error.util.client.clients.ColorUtil;
import error.util.render.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WardenHelper extends Module {
    public static WardenHelper INSTANCE;

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*(?:с|s|сек|sec)");

    private final Map<BlockPos, ChestData> trackedChests = new ConcurrentHashMap<>();

    private static final int COLOR_RED = ColorUtil.rgba(255, 60, 60, 180);
    private static final int COLOR_YELLOW = ColorUtil.rgba(255, 220, 50, 180);
    private static final int COLOR_GREEN = ColorUtil.rgba(60, 255, 60, 180);
    private static final int COLOR_UNKNOWN = ColorUtil.rgba(180, 180, 180, 120);

    public WardenHelper() {
        super("WardenHelper", "Помощник для безопасного фарма Вардена", Category.MISC);
        INSTANCE = this;
    }

    public static WardenHelper getInstance() {
        return INSTANCE;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        trackedChests.clear();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        trackedChests.clear();
    }

    @EventTarget
    public void onWorldLeave(WorldLeaveEvent event) {
        trackedChests.clear();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (!isInWardenZone()) return;

        updateTimersFromHolograms();
    }

    public boolean isInWardenZone() {
        if (mc.player == null) return false;
        double x = mc.player.getX();
        double z = mc.player.getZ();
        return Math.abs(Math.abs(x) - 2000.0) <= 150.0 && Math.abs(Math.abs(z) - 2000.0) <= 150.0;
    }

    private boolean isChestInZone(BlockPos pos) {
        if (mc.player == null) return false;
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        boolean inXZ = Math.abs(Math.abs(x) - 2000.0) <= 150.0 && Math.abs(Math.abs(z) - 2000.0) <= 150.0;
        boolean inY = Math.abs(y - mc.player.getY()) <= 30.0;
        return inXZ && inY;
    }

    private void updateTimersFromHolograms() {
        for (Map.Entry<BlockPos, ChestData> entry : trackedChests.entrySet()) {
            BlockPos pos = entry.getKey();
            ChestData data = entry.getValue();

            AABB searchBox = new AABB(pos).inflate(1.0, 3.0, 1.0);
            var stands = mc.level.getEntitiesOfClass(
                    ArmorStand.class, searchBox, e -> e.hasCustomName()
            );

            boolean foundTimer = false;
            for (ArmorStand stand : stands) {
                if (stand.getCustomName() == null) continue;
                String name = stand.getCustomName().getString();
                String clean = stripFormatting(name);
                if (clean.isEmpty()) continue;

                long seconds = parseTimeToSeconds(clean);
                if (seconds >= 0) {
                    data.updateFromHologram(seconds);
                    foundTimer = true;
                    break;
                }
            }

            if (!foundTimer) {
                data.hologramVisible = false;
            }
        }
    }

    public static long parseTimeToSeconds(String text) {
        Matcher timeMatcher = TIME_PATTERN.matcher(text);
        if (timeMatcher.find()) {
            if (timeMatcher.group(3) != null) {
                int hours = Integer.parseInt(timeMatcher.group(1));
                int minutes = Integer.parseInt(timeMatcher.group(2));
                int seconds = Integer.parseInt(timeMatcher.group(3));
                return hours * 3600L + minutes * 60L + seconds;
            } else {
                int minutes = Integer.parseInt(timeMatcher.group(1));
                int seconds = Integer.parseInt(timeMatcher.group(2));
                return minutes * 60L + seconds;
            }
        }

        Matcher secMatcher = SECONDS_PATTERN.matcher(text);
        if (secMatcher.find()) {
            return Long.parseLong(secMatcher.group(1));
        }

        String stripped = text.replaceAll("[^0-9:]", "").trim();
        if (!stripped.isEmpty() && stripped.matches("\\d+")) {
            long val = Long.parseLong(stripped);
            if (val > 0 && val < 36000) {
                return val;
            }
        }

        return -1;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (!isInWardenZone()) return;

        ChunkPos playerChunkPos = mc.player.chunkPosition();
        int renderDistance = Math.min(mc.options.renderDistance().get(), 6);

        for (int chunkX = playerChunkPos.x() - renderDistance; chunkX <= playerChunkPos.x() + renderDistance; chunkX++) {
            for (int chunkZ = playerChunkPos.z() - renderDistance; chunkZ <= playerChunkPos.z() + renderDistance; chunkZ++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ChestBlockEntity) {
                        BlockPos pos = blockEntity.getBlockPos();
                        if (!isChestInZone(pos)) continue;

                        if (!trackedChests.containsKey(pos)) {
                            trackedChests.put(pos, new ChestData(pos));
                        }

                        ChestData data = trackedChests.get(pos);
                        if (!data.hasTimer()) continue;

                        int colorArgb = getTimerColor(data);
                        Color fill = new Color((colorArgb >> 16) & 0xFF, (colorArgb >> 8) & 0xFF, colorArgb & 0xFF, 70);
                        Color out = new Color((colorArgb >> 16) & 0xFF, (colorArgb >> 8) & 0xFF, colorArgb & 0xFF, 200);

                        Render3D.drawBox(new AABB(pos), fill, out, true, true, true);
                    }
                }
            }
        }
    }

    public long getRemainingSeconds(BlockPos pos) {
        ChestData data = trackedChests.get(pos);
        if (data == null || !data.hasTimer()) return -1;
        return (long) data.getRemainingSeconds();
    }

    private int getTimerColor(ChestData data) {
        if (!data.hasTimer()) {
            return COLOR_UNKNOWN;
        }

        float remaining = data.getRemainingSeconds();

        if (remaining <= 0) {
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.3 + 0.7);
            int g = (int) (255 * pulse);
            return ColorUtil.rgba(0, g, (int) (128 * pulse), 220);
        }

        if (remaining > 120) {
            return COLOR_RED;
        } else if (remaining > 20) {
            float factor = 1.0f - (remaining - 20f) / 100f;
            return ColorUtil.lerp(COLOR_RED, COLOR_YELLOW, factor);
        } else {
            float factor = 1.0f - remaining / 20f;
            return ColorUtil.lerp(COLOR_YELLOW, COLOR_GREEN, factor);
        }
    }

    private static String stripFormatting(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    public static class ChestData {
        public final BlockPos pos;
        public long readyAtMs = -1;
        public long lastHologramUpdateMs = 0;
        public boolean hologramVisible = false;
        public long lastReadSeconds = -1;

        public ChestData(BlockPos pos) {
            this.pos = pos;
        }

        public void updateFromHologram(long remainingSeconds) {
            hologramVisible = true;
            lastHologramUpdateMs = System.currentTimeMillis();

            long newReadyAt = System.currentTimeMillis() + remainingSeconds * 1000L;

            if (readyAtMs == -1) {
                readyAtMs = newReadyAt;
                lastReadSeconds = remainingSeconds;
            } else {
                float currentEstimate = getRemainingSeconds();
                float diff = Math.abs(currentEstimate - remainingSeconds);

                if (diff > 3) {
                    readyAtMs = newReadyAt;
                }
                lastReadSeconds = remainingSeconds;
            }
        }

        public boolean hasTimer() {
            return readyAtMs > 0;
        }

        public float getRemainingSeconds() {
            if (readyAtMs <= 0) return -1;
            float remaining = (readyAtMs - System.currentTimeMillis()) / 1000f;
            return Math.max(0, remaining);
        }

        public void reset() {
            readyAtMs = -1;
            hologramVisible = false;
            lastReadSeconds = -1;
        }
    }
}
