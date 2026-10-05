package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.ui.hud.impl.DynamicIslandHud;
import error.util.client.clients.Theme;
import error.util.client.persiki.ChatUtil;
import error.util.client.clients.ColorUtil;
import error.util.player.TeleportUtil;
import error.util.render.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public class KeyFinderTeleport extends Module {
    public static KeyFinderTeleport INSTANCE;

    public final BindSetting searchKey = bind("Кнопка поиска", GLFW.GLFW_KEY_UNKNOWN);
    public final BindSetting clearKey = bind("Очистить найденное", GLFW.GLFW_KEY_UNKNOWN);
    public final ModeSetting searchType = mode("Тип поиска", "Всё", "Всё", "Вагонетка с сундуком", "Сундук рядом со спавнером");
    public final CheckBox renderFound = checkbox("Отображать найденное", true);

    private final Set<Long> visitedChunks = new HashSet<>();
    private final Set<Long> spawnerPositions = new HashSet<>();
    private final Set<BlockPos> foundChests = new HashSet<>();
    private final Set<BlockPos> foundMinecarts = new HashSet<>();

    private boolean searching = false;
    private boolean waitingTeleport = false;

    private int curTargetX;
    private int curTargetZ;
    private int startChunkX;
    private int startChunkZ;
    private int spiralX;
    private int spiralZ;
    private int dirX = 1;
    private int dirZ = 0;
    private int stepLimit = 1;
    private int curStep = 0;
    private int legCount = 0;

    private BlockPos pendingFoundPos = null;

    public KeyFinderTeleport() {
        super("KeyFinderTeleport", "Автоматически ищет ключ карты и спавнеры под землёй", Category.MISC);
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        stopSearch();
        super.onDisable();
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        if (clearKey.matches(event.getKey())) {
            clearFound();
            ChatUtil.info("Найденные объекты очищены");
            DynamicIslandHud.showNotification("Найденные объекты очищены", true, 3000L);
        } else if (searchKey.matches(event.getKey())) {
            if (!searching && !TeleportUtil.isActive()) {
                startSearch();
            } else {
                stopSearch();
                ChatUtil.error("Поиск остановлен");
                DynamicIslandHud.showNotification("Поиск остановлен", false, 3000L);
            }
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || mc.level == null) {
            stopSearch();
            return;
        }

        TeleportUtil.tick();

        if (pendingFoundPos != null) {
            player().setPos(pendingFoundPos.getX() + 0.5, pendingFoundPos.getY(), pendingFoundPos.getZ() + 0.5);
            pendingFoundPos = null;
            searching = false;
            waitingTeleport = false;
            ChatUtil.success("Телепортация к найденному завершена");
            DynamicIslandHud.showNotification("Телепортация завершена", true, 4000L);
        } else if (searching) {
            if (!player().getAbilities().flying) {
                ChatUtil.error("Поиск остановлен: требуется режим полёта");
                DynamicIslandHud.showNotification("Требуется режим полёта!", false, 3000L);
                stopSearch();
            } else if (!TeleportUtil.isActive()) {
                if (waitingTeleport) {
                    waitingTeleport = false;
                    scanChunk(curTargetX, curTargetZ);
                    if (!searching) return;
                }

                if (!nextSpiralChunk()) {
                    finishSearch();
                } else {
                    TeleportUtil.teleportTo(curTargetX, curTargetZ, true);
                    waitingTeleport = TeleportUtil.isActive();
                    if (!waitingTeleport) {
                        stopSearch();
                    }
                }
            }
        }
    }

    private void startSearch() {
        if (player() == null || mc.level == null) return;

        if (!player().getAbilities().flying) {
            ChatUtil.error("Для поиска требуется режим полёта");
            DynamicIslandHud.showNotification("Для поиска требуется режим полёта!", false, 3000L);
            return;
        }

        startChunkX = Math.floorDiv(Mth.floor(player().getX()), 16) * 16;
        startChunkZ = Math.floorDiv(Mth.floor(player().getZ()), 16) * 16;
        curTargetX = startChunkX;
        curTargetZ = startChunkZ;
        spiralX = 0;
        spiralZ = 0;
        dirX = 1;
        dirZ = 0;
        stepLimit = 1;
        curStep = 0;
        legCount = 0;
        waitingTeleport = false;
        searching = true;
        pendingFoundPos = null;

        ChatUtil.success("Начинаем поиск...");
        DynamicIslandHud.showNotification("Начинаем поиск...", true, 3000L);
        scanChunk(startChunkX, startChunkZ);
    }

    private boolean stepSpiral() {
        curStep++;
        spiralX += dirX;
        spiralZ += dirZ;
        if (curStep == stepLimit) {
            curStep = 0;
            int oldDirX = dirX;
            dirX = -dirZ;
            dirZ = oldDirX;
            if (++legCount == 2) {
                legCount = 0;
                stepLimit++;
            }
        }
        int maxDist = Math.max(Math.abs(spiralX), Math.abs(spiralZ));
        return maxDist * 16 <= 32000;
    }

    private boolean nextSpiralChunk() {
        while (stepSpiral()) {
            int x = startChunkX + spiralX * 16;
            int z = startChunkZ + spiralZ * 16;
            if (x >= -16000 && x <= 16000 && z >= -16000 && z <= 16000 && !visitedChunks.contains(chunkKey(x >> 4, z >> 4))) {
                curTargetX = x;
                curTargetZ = z;
                return true;
            }
        }
        return false;
    }

    private void scanChunk(int blockX, int blockZ) {
        if (mc.level == null) return;
        if (!visitedChunks.add(chunkKey(blockX >> 4, blockZ >> 4))) return;

        String mode = searchType.getValue();
        int bottomY = mc.level.getMinY();
        int maxY = bottomY + mc.level.getHeight() - 1;
        int scanTopY = Math.min(60, maxY);

        boolean scanMinecarts = "Всё".equals(mode) || "Вагонетка с сундуком".equals(mode);
        boolean scanChests = "Всё".equals(mode) || "Сундук рядом со спавнером".equals(mode);

        if (scanMinecarts) {
            AABB searchBox = new AABB(blockX - 8, bottomY, blockZ - 8, blockX + 8, scanTopY + 1, blockZ + 8);
            for (MinecartChest minecart : mc.level.getEntitiesOfClass(MinecartChest.class, searchBox, Entity::isAlive)) {
                BlockPos pos = minecart.blockPosition();
                if (foundMinecarts.add(pos)) {
                    onTargetFound(pos, "Вагонетка с сундуком", true);
                    return;
                }
            }
        }

        if (scanChests) {
            scanBlocksForChests(blockX, blockZ, bottomY, scanTopY);
        }
    }

    private void scanBlocksForChests(int blockX, int blockZ, int bottomY, int scanTopY) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = bottomY; y <= scanTopY; y++) {
                    mpos.set(blockX + x, y, blockZ + z);
                    BlockState state = mc.level.getBlockState(mpos);
                    if (state.getBlock() instanceof ChestBlock) {
                        BlockPos chestPos = mpos.immutable();
                        BlockPos spawnerPos = findNearbySpawner(chestPos);
                        if (spawnerPos != null) {
                            if (spawnerPositions.add(spawnerPos.asLong())) {
                                foundChests.add(chestPos);
                                onTargetFound(chestPos, "Сундук у спавнера", false);
                                return;
                            }
                            foundChests.add(chestPos);
                        }
                    }
                }
            }
        }
    }

    private BlockPos findNearbySpawner(BlockPos chestPos) {
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                checkPos.set(chestPos.getX() + dx, chestPos.getY(), chestPos.getZ() + dz);
                if (mc.level.getBlockState(checkPos).is(Blocks.SPAWNER)) {
                    return checkPos.immutable();
                }
            }
        }
        return null;
    }

    private void onTargetFound(BlockPos pos, String label, boolean isMinecart) {
        ChatUtil.success("Найден(а) " + label + " на " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        DynamicIslandHud.showNotification("Найдено: " + label + " [" + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "]", true, 5000L);

        if (isMinecart) {
            pendingFoundPos = pos.below();
        } else {
            pendingFoundPos = pos;
        }

        visitedChunks.add(chunkKey(pos.getX() >> 4, pos.getZ() >> 4));
        waitingTeleport = false;
        searching = false;
    }

    private void finishSearch() {
        int total = foundChests.size() + foundMinecarts.size();
        ChatUtil.info("Поиск завершён. Найдено: §e" + total);
        DynamicIslandHud.showNotification("Поиск завершён. Найдено: " + total, true, 4000L);
        stopSearch();
    }

    private void stopSearch() {
        searching = false;
        waitingTeleport = false;
        pendingFoundPos = null;
        TeleportUtil.reset();
    }

    private void clearFound() {
        visitedChunks.clear();
        spawnerPositions.clear();
        foundChests.clear();
        foundMinecarts.clear();
        stopSearch();
    }

    private long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!renderFound.getValue() || (foundChests.isEmpty() && foundMinecarts.isEmpty())) return;

        int accent = Theme.getAccentColor();
        Color chestFill = new Color(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), 50);
        Color chestOut = new Color(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), 240);

        Color spawnerFill = new Color(255, 120, 0, 60);
        Color spawnerOut = new Color(255, 160, 0, 240);

        for (BlockPos pos : foundChests) {
            Render3D.drawBox(pos, spawnerFill, spawnerOut, true, true, true);
        }

        for (BlockPos pos : foundMinecarts) {
            Render3D.drawBox(pos, chestFill, chestOut, true, true, true);
        }
    }
}
