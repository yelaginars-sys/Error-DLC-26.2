package error.util.player;

import error.util.client.persiki.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class TeleportUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    private static boolean active;
    private static boolean stage2;
    private static boolean reachedTarget;
    private static int targetX;
    private static int targetZ;
    private static int waitTicks;
    private static Vec3 targetPos;
    private static ClientLevel targetLevel;
    private static boolean silent;

    public static boolean isActive() {
        return active;
    }

    public static void reset() {
        active = false;
        stage2 = false;
        reachedTarget = false;
        targetPos = null;
        targetLevel = null;
        silent = false;
        waitTicks = 0;
    }

    public static void teleportTo(int x, int z, boolean isSilent) {
        if (mc.player == null || mc.level == null) return;
        reset();

        if (!mc.player.getAbilities().flying) {
            ChatUtil.error("Вы не в режиме полёта, телепорт невозможен");
            return;
        }

        targetX = x;
        targetZ = z;
        waitTicks = 0;
        targetPos = null;
        stage2 = false;
        reachedTarget = false;
        targetLevel = mc.level;
        silent = isSilent;
        active = true;

        if (!silent) {
            ChatUtil.info("Телепортация на " + x + " " + z);
        }
    }

    public static void teleportToPlayer(String name) {
        if (mc.player == null || mc.level == null) return;
        reset();

        if (!mc.player.getAbilities().flying) {
            ChatUtil.error("Вы не в режиме полёта, телепорт невозможен");
            return;
        }

        Player target = mc.level.players().stream()
                .filter(p -> p.getScoreboardName().equalsIgnoreCase(name))
                .findFirst().orElse(null);

        if (target == null) {
            ChatUtil.error("Игрок " + name + " не найден");
            return;
        }

        Vec3 pos = target.position();
        int bx = Mth.floor(pos.x);
        int bz = Mth.floor(pos.z);
        int by = Mth.floor(pos.y);

        if (isSolid(target.blockPosition().above())) {
            sendPos(pos.x, pos.y, pos.z);
            ChatUtil.success("Попытка телепортации к " + name);
        } else {
            for (int y = by; y > mc.level.getMinY(); y--) {
                BlockPos check = new BlockPos(bx, y, bz);
                if (isSolid(check)) {
                    sendPos(bx + 0.5, y, bz + 0.5);
                    ChatUtil.success("Попытка телепортации к " + name);
                    return;
                }
            }
            ChatUtil.error("Не найден блок под игроком " + name);
        }
    }

    public static void tick() {
        if (!active) return;

        if (mc.player == null || mc.level == null || mc.level != targetLevel) {
            reset();
            return;
        }

        if (!mc.player.getAbilities().flying) {
            reset();
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        if (targetPos != null) {
            if (!mc.player.blockPosition().equals(BlockPos.containing(targetPos))) {
                mc.player.setPos(targetPos.x, targetPos.y, targetPos.z);
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                            targetPos.x, targetPos.y, targetPos.z, false, mc.player.horizontalCollision));
                }
                waitTicks = 2;
                return;
            }

            targetPos = null;
            if (reachedTarget) {
                if (!silent) {
                    ChatUtil.success("Телепортация завершена: " + targetX + " " + targetZ);
                }
                reset();
                return;
            }

            stage2 = true;
        }

        if (isChunkLoaded(targetX + 0.5, targetZ + 0.5)) {
            int topY = findTopSolidBlock(targetX, targetZ);
            if (topY == Integer.MIN_VALUE) {
                ChatUtil.error("Не найден блок на координатах " + targetX + " " + targetZ);
                reset();
            } else {
                reachedTarget = true;
                setIntermediatePos(targetX + 0.5, topY, targetZ + 0.5);
            }
        } else if (!stage2) {
            setIntermediatePos(mc.player.getX(), 1.0, mc.player.getZ());
        } else {
            double px = mc.player.getX();
            double py = mc.player.getY();
            double pz = mc.player.getZ();
            double dx = targetX + 0.5 - px;
            double dz = targetZ + 0.5 - pz;
            double dist = Math.hypot(dx, dz);

            if (!(dist < 1.0)) {
                double step = calcStep(px, pz, dx, dz, dist);
                if (!(step <= 1.0)) {
                    setIntermediatePos(px + (dx / dist) * step, py, pz + (dz / dist) * step);
                }
            }
        }
    }

    private static void sendPos(double x, double y, double z) {
        if (mc.player != null) {
            mc.player.setPos(x, y, z);
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                        x, y, z, false, mc.player.horizontalCollision));
            }
        }
    }

    private static void setIntermediatePos(double x, double y, double z) {
        targetPos = new Vec3(x, y, z);
        sendPos(x, y, z);
        waitTicks = 2;
    }

    private static boolean isChunkLoaded(double x, double z) {
        if (mc.level == null) return false;
        int cx = Mth.floor(x) >> 4;
        int cz = Mth.floor(z) >> 4;
        return mc.level.getChunkSource().hasChunk(cx, cz);
    }

    private static boolean isSolid(BlockPos pos) {
        return mc.level != null && !mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty();
    }

    private static int findTopSolidBlock(int x, int z) {
        if (mc.level == null) return Integer.MIN_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int maxY = mc.level.getMaxY();
        int minY = mc.level.getMinY();
        for (int y = maxY; y >= minY; y--) {
            pos.set(x, y, z);
            if (isSolid(pos)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static double calcStep(double x, double z, double dx, double dz, double dist) {
        double normX = dx / dist;
        double normZ = dz / dist;
        double validStep = 0.0;
        double step = 0.0;

        while (step <= dist && isChunkLoaded(x + normX * step, z + normZ * step)) {
            validStep = step++;
        }

        return validStep;
    }
}
