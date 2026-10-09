package error.module.impl.render;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BaseFinder extends Module {

    public final SliderSetting radius = slider("Радиус", 60.0f, 5.0f, 60.0f, 5.0f);
    public final CheckBox outline = checkbox("Обводка", true);
    public final CheckBox tracers = checkbox("Трейсеры", true);

    public final CheckBox showChests = checkbox("Сундуки и бочки", true);
    public final CheckBox showHoppers = checkbox("Воронки", true);
    public final CheckBox showTorches = checkbox("Факела", true);
    public final CheckBox showObsidian = checkbox("Обсидиан", true);
    public final CheckBox showRedstone = checkbox("Раздатчики/Выбрасыватели", true);
    public final CheckBox showPlanks = checkbox("Доски", true);

    private final Set<Block> targetBlocks = new HashSet<>();
    private volatile List<BlockPos> foundBlocks = new ArrayList<>();
    private int scanTick = 5;

    public BaseFinder() {
        super("BaseFinder", "Поиск баз и сундуков других игроков", Category.RENDER);
    }

    private Set<Block> getActiveTargetBlocks() {
        targetBlocks.clear();

        if (showChests.get()) {
            targetBlocks.add(Blocks.CHEST);
            targetBlocks.add(Blocks.TRAPPED_CHEST);
            targetBlocks.add(Blocks.ENDER_CHEST);
            targetBlocks.add(Blocks.BARREL);
        }

        if (showHoppers.get()) {
            targetBlocks.add(Blocks.HOPPER);
        }

        if (showTorches.get()) {
            targetBlocks.add(Blocks.TORCH);
            targetBlocks.add(Blocks.WALL_TORCH);
            targetBlocks.add(Blocks.SOUL_TORCH);
            targetBlocks.add(Blocks.SOUL_WALL_TORCH);
            targetBlocks.add(Blocks.REDSTONE_TORCH);
            targetBlocks.add(Blocks.REDSTONE_WALL_TORCH);
        }

        if (showObsidian.get()) {
            targetBlocks.add(Blocks.OBSIDIAN);
            targetBlocks.add(Blocks.CRYING_OBSIDIAN);
        }

        if (showRedstone.get()) {
            targetBlocks.add(Blocks.DISPENSER);
            targetBlocks.add(Blocks.DROPPER);
        }

        if (showPlanks.get()) {
            targetBlocks.add(Blocks.OAK_PLANKS);
            targetBlocks.add(Blocks.SPRUCE_PLANKS);
            targetBlocks.add(Blocks.BIRCH_PLANKS);
            targetBlocks.add(Blocks.JUNGLE_PLANKS);
            targetBlocks.add(Blocks.ACACIA_PLANKS);
            targetBlocks.add(Blocks.DARK_OAK_PLANKS);
            targetBlocks.add(Blocks.MANGROVE_PLANKS);
            targetBlocks.add(Blocks.CHERRY_PLANKS);
            targetBlocks.add(Blocks.BAMBOO_PLANKS);
            targetBlocks.add(Blocks.CRIMSON_PLANKS);
            targetBlocks.add(Blocks.WARPED_PLANKS);

            try {
                for (Block block : BuiltInRegistries.BLOCK) {
                    if (block == null) continue;
                    Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                    if (id != null) {
                        String path = id.getPath();
                        if (path.endsWith("_planks") || path.contains("planks")) {
                            targetBlocks.add(block);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return targetBlocks;
    }

    @Override
    protected void onEnable() {
        scanTick = 5;
        foundBlocks = new ArrayList<>();
    }

    @Override
    protected void onDisable() {
        foundBlocks = new ArrayList<>();
        scanTick = 0;
    }

    @EventTarget
    public void onPlayerTick(PlayerTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            foundBlocks = new ArrayList<>();
            return;
        }

        Set<Block> targets = getActiveTargetBlocks();
        if (targets.isEmpty()) {
            foundBlocks = new ArrayList<>();
            return;
        }

        if (++scanTick < 5) return;
        scanTick = 0;

        List<BlockPos> found = new ArrayList<>();
        int r = radius.getValue().intValue();
        int rSq = r * r;
        int px = mc.player.getBlockX();
        int py = mc.player.getBlockY();
        int pz = mc.player.getBlockZ();
        int minY = mc.level.getMinY();
        int maxY = mc.level.getMaxY();

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -r; dx <= r; dx++) {
            int x = px + dx;
            for (int dz = -r; dz <= r; dz++) {
                int z = pz + dz;
                if (!mc.level.hasChunk(x >> 4, z >> 4)) continue;
                for (int dy = -r; dy <= r; dy++) {
                    int y = py + dy;
                    if (y < minY || y > maxY) continue;
                    if (dx * dx + dy * dy + dz * dz > rSq) continue;

                    mutable.set(x, y, z);
                    Block block = mc.level.getBlockState(mutable).getBlock();
                    if (targets.contains(block)) {
                        found.add(mutable.immutable());
                    }
                }
            }
        }

        found.sort((a, b) -> Double.compare(a.distSqr(new BlockPos(px, py, pz)), b.distSqr(new BlockPos(px, py, pz))));

        if (found.size() > 2000) {
            found = found.subList(0, 2000);
        }

        this.foundBlocks = found;
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        Minecraft mc = Minecraft.getInstance();
        List<BlockPos> blocks = this.foundBlocks;
        if (mc.level == null || mc.player == null || blocks == null || blocks.isEmpty()) return;

        Vec3 cam = mc.player.getEyePosition();
        int color = ColorUtil.rgba(0, 220, 255, 255);
    }
}
