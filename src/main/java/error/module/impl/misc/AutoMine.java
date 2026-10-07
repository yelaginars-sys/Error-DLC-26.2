package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
import error.util.RotationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoMine module ported from exclusive.
 * Automated diamond ore mining on FunTime /warp mine with anarchy hopping.
 */
public class AutoMine extends Module {

    public final SliderSetting reachRange = slider("Дистанция ломки", 4.2f, 3.0f, 5.0f, 0.1f);
    public final SliderSetting maxWaitTime = slider("Макс. ожидание (сек)", 30.0f, 10.0f, 60.0f, 1.0f);
    public final CheckBox disableJumping = checkbox("Запретить прыжки", true);

    private static final double MINE_MIN_X = -85.0;
    private static final double MINE_MAX_X = -65.0;
    private static final double MINE_MIN_Y = 72.0;
    private static final double MINE_MAX_Y = 82.0;
    private static final double MINE_MIN_Z = -4.0;
    private static final double MINE_MAX_Z = 15.0;

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+):(\\d+)(?::(\\d+))?");
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*(?:сек|sec|s)?");

    private enum State {
        CHECK_LOCATION,
        WAIT_TELEPORT_MINE,
        SCAN_HOLOGRAM,
        MINING,
        HOP_ANARCHY
    }

    private State state = State.CHECK_LOCATION;
    private final List<Integer> anarchyPool = new ArrayList<>();
    private int currentPoolIndex = 0;
    private long stateStartTime = 0L;
    private BlockPos currentTargetBlock = null;

    public AutoMine() {
        super("AutoMine", "Автоматическое копание руды на авто-шахте FunTime", Category.MISC);
        initAnarchyList();
    }

    private void initAnarchyList() {
        anarchyPool.clear();
        for (int i = 101; i <= 110; i++) anarchyPool.add(i);
        for (int i = 201; i <= 210; i++) anarchyPool.add(i);
        for (int i = 301; i <= 306; i++) anarchyPool.add(i);
        for (int i = 401; i <= 402; i++) anarchyPool.add(i);
        for (int i = 501; i <= 503; i++) anarchyPool.add(i);
        for (int i = 601; i <= 603; i++) anarchyPool.add(i);
        Collections.shuffle(anarchyPool);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        state = State.CHECK_LOCATION;
        stateStartTime = System.currentTimeMillis();
        currentTargetBlock = null;
        Collections.shuffle(anarchyPool);
        currentPoolIndex = 0;
        selectBestPickaxe();
    }

    @Override
    protected void onDisable() {
        RotationHandler.disengage("Smooth");
        mc.options.keyAttack.setDown(false);
        super.onDisable();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || mc.gameMode == null) return;

        if (disableJumping.getValue()) {
            mc.options.keyJump.setDown(false);
        }

        selectBestPickaxe();

        switch (state) {
            case CHECK_LOCATION -> handleCheckLocation();
            case WAIT_TELEPORT_MINE -> handleWaitTeleportMine();
            case SCAN_HOLOGRAM -> handleScanHologram();
            case MINING -> handleMining();
            case HOP_ANARCHY -> handleHopAnarchy();
        }
    }

    private void selectBestPickaxe() {
        if (player() == null) return;

        int bestSlot = -1;
        int highestPriority = -1;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player().getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                int priority = getPickaxePriority(stack);
                if (priority > highestPriority) {
                    highestPriority = priority;
                    bestSlot = slot;
                }
            }
        }

        if (bestSlot != -1 && player().getInventory().getSelectedSlot() != bestSlot) {
            player().getInventory().setSelectedSlot(bestSlot);
        }
    }

    private int getPickaxePriority(ItemStack stack) {
        if (stack.is(Items.NETHERITE_PICKAXE)) return 6;
        if (stack.is(Items.DIAMOND_PICKAXE)) return 5;
        if (stack.is(Items.IRON_PICKAXE)) return 4;
        if (stack.is(Items.GOLDEN_PICKAXE)) return 3;
        if (stack.is(Items.STONE_PICKAXE)) return 2;
        if (stack.is(Items.WOODEN_PICKAXE)) return 1;
        String name = stack.getItem().getDescriptionId().toLowerCase();
        if (name.contains("pickaxe") || name.contains("кирка")) return 1;
        return 0;
    }

    private boolean isPlayerInMineArea() {
        if (player() == null) return false;
        double x = player().getX();
        double y = player().getY();
        double z = player().getZ();

        return x >= MINE_MIN_X && x <= MINE_MAX_X
                && y >= MINE_MIN_Y && y <= MINE_MAX_Y
                && z >= MINE_MIN_Z && z <= MINE_MAX_Z;
    }

    private void handleCheckLocation() {
        if (isPlayerInMineArea()) {
            List<BlockPos> ores = scanReachableDiamonds();
            if (!ores.isEmpty()) {
                state = State.MINING;
                stateStartTime = System.currentTimeMillis();
                return;
            }
            state = State.SCAN_HOLOGRAM;
            stateStartTime = System.currentTimeMillis();
        } else {
            if (mc.getConnection() != null) {
                mc.getConnection().sendCommand("warp mine");
            }
            state = State.WAIT_TELEPORT_MINE;
            stateStartTime = System.currentTimeMillis();
        }
    }

    private void handleWaitTeleportMine() {
        if (System.currentTimeMillis() - stateStartTime >= 1000L) {
            if (isPlayerInMineArea()) {
                state = State.SCAN_HOLOGRAM;
                stateStartTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - stateStartTime >= 5000L) {
                if (mc.getConnection() != null) {
                    mc.getConnection().sendCommand("warp mine");
                }
                stateStartTime = System.currentTimeMillis();
            }
        }
    }

    private void handleScanHologram() {
        List<BlockPos> ores = scanReachableDiamonds();
        if (!ores.isEmpty()) {
            state = State.MINING;
            stateStartTime = System.currentTimeMillis();
            return;
        }

        HologramInfo holo = findMineHologram();
        if (holo != null) {
            if (holo.secondsLeft > 30) {
                state = State.HOP_ANARCHY;
                stateStartTime = System.currentTimeMillis();
                return;
            }
        }

        if (System.currentTimeMillis() - stateStartTime >= (long) (maxWaitTime.getValue() * 1000L)) {
            state = State.HOP_ANARCHY;
            stateStartTime = System.currentTimeMillis();
        }
    }

    private void handleMining() {
        List<BlockPos> ores = scanReachableDiamonds();
        if (ores.isEmpty()) {
            mc.options.keyAttack.setDown(false);
            currentTargetBlock = null;
            state = State.SCAN_HOLOGRAM;
            stateStartTime = System.currentTimeMillis();
            return;
        }

        BlockPos target = ores.get(0);
        currentTargetBlock = target;

        Vec3 center = Vec3.atCenterOf(target);
        Vec3 eyes = player().getEyePosition();
        double dx = center.x - eyes.x;
        double dy = center.y - eyes.y;
        double dz = center.z - eyes.z;
        double distH = Math.hypot(dx, dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distH));

        RotationHandler.setRotation(yaw, pitch);

        Direction hitFace = Direction.getApproximateNearest((float) dx, (float) dy, (float) dz).getOpposite();
        BlockHitResult hitResult = new BlockHitResult(center, hitFace, target, false);

        mc.options.keyAttack.setDown(true);
        if (mc.gameMode != null) {
            mc.gameMode.continueDestroyBlock(target, hitFace);
        }
        player().swing(InteractionHand.MAIN_HAND);
    }

    private void handleHopAnarchy() {
        mc.options.keyAttack.setDown(false);
        if (anarchyPool.isEmpty()) {
            initAnarchyList();
        }

        if (currentPoolIndex >= anarchyPool.size()) {
            currentPoolIndex = 0;
            Collections.shuffle(anarchyPool);
        }

        int targetAnarchy = anarchyPool.get(currentPoolIndex++);
        if (mc.getConnection() != null) {
            mc.getConnection().sendCommand("an" + targetAnarchy);
        }

        state = State.WAIT_TELEPORT_MINE;
        stateStartTime = System.currentTimeMillis();
    }

    private List<BlockPos> scanReachableDiamonds() {
        if (mc.level == null || player() == null) return Collections.emptyList();

        List<BlockPos> list = new ArrayList<>();
        double range = reachRange.getValue();
        BlockPos playerPos = player().blockPosition();
        int r = (int) Math.ceil(range);

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
                        if (player().distanceToSqr(Vec3.atCenterOf(pos)) <= range * range) {
                            list.add(pos);
                        }
                    }
                }
            }
        }

        list.sort((a, b) -> Double.compare(player().distanceToSqr(Vec3.atCenterOf(a)), player().distanceToSqr(Vec3.atCenterOf(b))));
        return list;
    }

    private HologramInfo findMineHologram() {
        if (mc.level == null || player() == null) return null;
        AABB box = player().getBoundingBox().inflate(30.0);

        for (Entity entity : mc.level.getEntities((Entity) null, box, e -> e instanceof ArmorStand)) {
            ArmorStand as = (ArmorStand) entity;
            Component name = as.getCustomName();
            if (name != null) {
                String str = name.getString().toLowerCase();
                long sec = parseTimeToSeconds(str);
                if (sec >= 0) {
                    HologramInfo info = new HologramInfo();
                    info.secondsLeft = sec;
                    return info;
                }
            }
        }
        return null;
    }

    private long parseTimeToSeconds(String text) {
        Matcher timeMatcher = TIME_PATTERN.matcher(text);
        if (timeMatcher.find()) {
            int minutes = Integer.parseInt(timeMatcher.group(1));
            int seconds = Integer.parseInt(timeMatcher.group(2));
            return minutes * 60L + seconds;
        }

        Matcher secMatcher = SECONDS_PATTERN.matcher(text);
        if (secMatcher.find()) {
            return Long.parseLong(secMatcher.group(1));
        }

        return -1;
    }

    private static class HologramInfo {
        long secondsLeft = -1;
    }
}
