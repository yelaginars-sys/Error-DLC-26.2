package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.util.render.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DanjHelper module ported from exclusive.
 * Highlights dungeon barrels, parses hologram timers, and sends notifications.
 */
public class DanjHelper extends Module {

    private final Map<BlockPos, DanjBlockData> trackedBlocks = new ConcurrentHashMap<>();
    private final Set<BlockPos> gpsNotified = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<BlockPos> chatNotified = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*(с|s|сек|sec)");

    public DanjHelper() {
        super("DanjHelper", "Помощник для прохождения данжей", Category.MISC);
    }

    @Override
    protected void onDisable() {
        resetState();
        super.onDisable();
    }

    private void resetState() {
        trackedBlocks.clear();
        gpsNotified.clear();
        chatNotified.clear();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || mc.level == null) return;
        if (!isInDanjZone()) return;

        scanBarrelsAndHolograms();
        checkNotifications();
    }

    private boolean isInDanjZone() {
        if (player() == null) return false;
        double x = player().getX();
        double z = player().getZ();
        return Math.abs(Math.abs(x) - 2000.0) <= 150.0 && Math.abs(Math.abs(z) - 2000.0) <= 150.0;
    }

    private boolean isBlockInZone(BlockPos pos) {
        if (player() == null) return false;
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        boolean inXZ = Math.abs(Math.abs(x) - 2000.0) <= 150.0 && Math.abs(Math.abs(z) - 2000.0) <= 150.0;
        boolean inY = Math.abs(y - player().getY()) <= 30.0;
        return inXZ && inY;
    }

    private void scanBarrelsAndHolograms() {
        if (player() == null || mc.level == null) return;
        AABB box = player().getBoundingBox().inflate(32.0);

        for (Entity entity : mc.level.getEntities((Entity) null, box, e -> e instanceof ArmorStand)) {
            ArmorStand armorStand = (ArmorStand) entity;
            if (!armorStand.hasCustomName() || armorStand.getCustomName() == null) continue;

            String name = armorStand.getCustomName().getString();
            String clean = name.replaceAll("§.", "").trim();

            long seconds = parseTimeToSeconds(clean);
            if (seconds >= 0) {
                BlockPos pos = armorStand.blockPosition().below();
                trackedBlocks.computeIfAbsent(pos, DanjBlockData::new).updateFromHologram(seconds);
            }
        }
    }

    private long parseTimeToSeconds(String text) {
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

    private void checkNotifications() {
        for (Map.Entry<BlockPos, DanjBlockData> entry : trackedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            DanjBlockData data = entry.getValue();

            if (!data.hasTimer()) continue;

            float remaining = data.getRemainingSeconds();
            if (remaining <= 30.0f && remaining > 0.0f) {
                if (!gpsNotified.contains(pos)) {
                    if (mc.getConnection() != null) {
                        mc.getConnection().sendChat(".gps set " + pos.getX() + " " + pos.getZ());
                    }
                    gpsNotified.add(pos);
                }

                if (!chatNotified.contains(pos)) {
                    chatNotified.add(pos);
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!inGame() || player() == null || mc.level == null) return;
        if (!isInDanjZone()) return;

        ChunkPos playerChunkPos = player().chunkPosition();
        int renderDistance = Math.min(mc.options.renderDistance().get(), 4);

        for (int chunkX = playerChunkPos.x() - renderDistance; chunkX <= playerChunkPos.x() + renderDistance; chunkX++) {
            for (int chunkZ = playerChunkPos.z() - renderDistance; chunkZ <= playerChunkPos.z() + renderDistance; chunkZ++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof BarrelBlockEntity) {
                        BlockPos pos = blockEntity.getBlockPos();
                        if (!isBlockInZone(pos)) continue;

                        if (!trackedBlocks.containsKey(pos)) {
                            trackedBlocks.put(pos, new DanjBlockData(pos));
                        }

                        DanjBlockData data = trackedBlocks.get(pos);
                        Color color = getTimerColor(data);

                        Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 60);
                        Color outline = new Color(color.getRed(), color.getGreen(), color.getBlue(), 220);

                        Render3D.drawBox(pos, fill, outline, true, true);
                    }
                }
            }
        }
    }

    private Color getTimerColor(DanjBlockData data) {
        if (!data.hasTimer()) {
            return new Color(180, 180, 180);
        }

        float remaining = data.getRemainingSeconds();

        if (remaining <= 0) {
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.3 + 0.7);
            int g = (int) (255 * pulse);
            return new Color(0, g, (int) (128 * pulse));
        }

        if (remaining > 120) {
            return new Color(255, 60, 60);
        } else if (remaining > 20) {
            float factor = (remaining - 20f) / 100f;
            int r = 255;
            int g = (int) (220 - 160 * factor);
            int b = (int) (50 + 10 * factor);
            return new Color(r, Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
        } else {
            float factor = remaining / 20f;
            int r = (int) (60 + 195 * factor);
            int g = (int) (255 - 35 * factor);
            int b = (int) (60 - 10 * factor);
            return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
        }
    }

    private static class DanjBlockData {
        final BlockPos pos;
        long targetEndTime = -1L;
        boolean hasTimer = false;

        DanjBlockData(BlockPos pos) {
            this.pos = pos;
        }

        void updateFromHologram(long seconds) {
            this.targetEndTime = System.currentTimeMillis() + seconds * 1000L;
            this.hasTimer = true;
        }

        boolean hasTimer() {
            return hasTimer;
        }

        float getRemainingSeconds() {
            if (!hasTimer) return -1.0f;
            long diff = targetEndTime - System.currentTimeMillis();
            return Math.max(0.0f, diff / 1000.0f);
        }
    }
}
