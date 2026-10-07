package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
import error.util.render.Render3D;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backtrack ported from Exclusive Client:
 * Records historical hitboxes of targets and allows hitting them in past positions to compensate for latency.
 */
public final class Backtrack extends Module {
    public static Backtrack INSTANCE;

    public final SliderSetting reach = slider("Дистанция", 3.0F, 1.0F, 8.0F, 0.1F);
    public final SliderSetting window = slider("Окно (мс)", 200.0F, 0.0F, 1000.0F, 10.0F);
    public final CheckBox showBox = checkbox("Отображать хитбокс", true);
    public final CheckBox pingCompensation = checkbox("Компенсация пинга", true);
    public final CheckBox checkWalls = checkbox("Проверка стен", true);
    public final CheckBox smartOnly = checkbox("Умный режим", true);
    public final CheckBox onlyPlayers = checkbox("Только игроки", true);

    private final Map<Integer, List<Position>> positions = new ConcurrentHashMap<>();
    private final Map<Integer, Vec3> savedPos = new ConcurrentHashMap<>();
    private final Map<Integer, AABB> savedBox = new ConcurrentHashMap<>();

    public Backtrack() {
        super("Backtrack", "Откатывает хитбокс цели назад во времени под пинг", Category.COMBAT);
        INSTANCE = this;
    }

    public static Backtrack getInstance() {
        return INSTANCE;
    }

    public static class Position {
        public final Vec3 position;
        public final AABB box;
        public final int entityId;
        public final long timestamp;

        public Position(Vec3 position, AABB box, int entityId, long timestamp) {
            this.position = position;
            this.box = box;
            this.entityId = entityId;
            this.timestamp = timestamp;
        }
    }

    @Override
    public void onEnable() {
        clearPositions();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        restoreAll();
        clearPositions();
        super.onDisable();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;
        if (mc.level == null || mc.player == null) {
            clearPositions();
            return;
        }

        long now = System.currentTimeMillis();
        long windowTime = window.getValue().longValue();
        long maxHistory = Math.max(windowTime + 200L, 650L);

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && entity != mc.player && living.isAlive()) {
                if (onlyPlayers.getValue() && !(entity instanceof Player)) continue;
                if (mc.player.distanceToSqr(living) > 144.0) continue;

                List<Position> list = positions.computeIfAbsent(living.getId(), k -> new ArrayList<>());
                synchronized (list) {
                    list.add(new Position(living.position(), living.getBoundingBox(), living.getId(), now));
                    list.removeIf(p -> now - p.timestamp > maxHistory);
                }
            }
        }

        positions.keySet().removeIf(id -> {
            Entity entity = mc.level.getEntity(id);
            return entity == null || !entity.isAlive();
        });
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled() || !showBox.getValue() || mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        long allowedTime = calculateAllowedTime();

        for (Map.Entry<Integer, List<Position>> entry : positions.entrySet()) {
            Entity entity = mc.level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;

            Position best = getBestPosition(living);
            if (best != null && best.box != null) {
                Color fill = new Color(0, 180, 255, 35);
                Color outline = new Color(0, 220, 255, 180);
                Render3D.drawBox(best.box, fill, outline, true, true, false);
            }
        }
    }

    public Position getBestPosition(LivingEntity target) {
        if (!isEnabled() || target == null || mc.player == null || mc.level == null) {
            return null;
        }

        List<Position> list = positions.get(target.getId());
        if (list == null || list.isEmpty()) {
            return null;
        }

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 rotVec = mc.player.getViewVector(1.0F);
        double maxReach = reach.getValue();
        double currentDist = getDistanceToBox(eyePos, target.getBoundingBox());

        if (currentDist > maxReach + 1.5) {
            return null;
        }

        boolean lookingAtCurrent = target.getBoundingBox().clip(eyePos, eyePos.add(rotVec.scale(maxReach))).isPresent();
        if (smartOnly.getValue() && currentDist <= 2.85 && lookingAtCurrent) {
            return null;
        }

        long now = System.currentTimeMillis();
        long allowedTime = calculateAllowedTime();
        Position bestCandidate = null;
        double bestScore = Double.MAX_VALUE;

        synchronized (list) {
            for (Position p : list) {
                long age = now - p.timestamp;
                if (age >= 15L && age <= allowedTime && p.box != null) {
                    double dist = getDistanceToBox(eyePos, p.box);
                    if (dist > maxReach) continue;
                    if (checkWalls.getValue() && checkWall(eyePos, p.box.getCenter())) continue;

                    boolean lookingAtOld = p.box.clip(eyePos, eyePos.add(rotVec.scale(maxReach + 0.5))).isPresent();

                    double score = dist + (lookingAtOld ? 0.0 : 1.2) + (double) age * 0.003;
                    if (score < bestScore) {
                        bestScore = score;
                        bestCandidate = p;
                    }
                }
            }
        }

        return bestCandidate;
    }

    public boolean shouldTargetBacktrack(LivingEntity target) {
        if (!isEnabled() || target == null || mc.player == null) return false;
        Vec3 eyePos = mc.player.getEyePosition();
        double currentDist = getDistanceToBox(eyePos, target.getBoundingBox());
        float reachVal = reach.getValue();
        if (currentDist > reachVal) return true;
        if (!smartOnly.getValue()) return true;

        Vec3 rotVec = mc.player.getViewVector(1.0F);
        boolean lookingAtCurrent = target.getBoundingBox().clip(eyePos, eyePos.add(rotVec.scale(reachVal))).isPresent();
        return !(currentDist <= 2.85 && lookingAtCurrent);
    }

    public void apply(Entity entity) {
        try {
            if (!isEnabled() || !(entity instanceof LivingEntity living) || mc.player == null) {
                return;
            }

            Position best = getBestPosition(living);
            if (best == null || best.position == null || best.box == null) {
                return;
            }

            savedPos.put(living.getId(), living.position());
            savedBox.put(living.getId(), living.getBoundingBox());
            living.setPos(best.position.x, best.position.y, best.position.z);
            living.setBoundingBox(best.box);
        } catch (Throwable ignored) {
        }
    }

    public void restore(Entity entity) {
        try {
            if (entity instanceof LivingEntity living) {
                Vec3 pos = savedPos.remove(living.getId());
                AABB box = savedBox.remove(living.getId());
                if (pos != null) {
                    living.setPos(pos.x, pos.y, pos.z);
                }
                if (box != null) {
                    living.setBoundingBox(box);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public void restoreAll() {
        try {
            for (Map.Entry<Integer, Vec3> entry : savedPos.entrySet()) {
                Entity entity = mc.level != null ? mc.level.getEntity(entry.getKey()) : null;
                if (entity instanceof LivingEntity living) {
                    Vec3 pos = entry.getValue();
                    AABB box = savedBox.get(entry.getKey());
                    if (pos != null) {
                        living.setPos(pos.x, pos.y, pos.z);
                    }
                    if (box != null) {
                        living.setBoundingBox(box);
                    }
                }
            }
            clearPositions();
        } catch (Throwable ignored) {
        }
    }

    public void clearPositions() {
        positions.clear();
        savedPos.clear();
        savedBox.clear();
    }

    public static double getDistanceToBox(Vec3 point, AABB box) {
        double x = Mth.clamp(point.x, box.minX, box.maxX);
        double y = Mth.clamp(point.y, box.minY, box.maxY);
        double z = Mth.clamp(point.z, box.minZ, box.maxZ);
        return point.distanceTo(new Vec3(x, y, z));
    }

    public static int getPlayerPing() {
        try {
            if (mc.player != null && mc.getConnection() != null) {
                PlayerInfo entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                return entry != null ? Math.max(0, entry.getLatency()) : 50;
            }
        } catch (Throwable ignored) {
        }
        return 50;
    }

    public long calculateAllowedTime() {
        try {
            int ping = getPlayerPing();
            long maxTime = window.getValue().longValue();
            long pingVal = pingCompensation.getValue() ? Math.max(30L, (long) ping + 30L) : maxTime;
            return Math.max(30L, Math.min(maxTime, pingVal));
        } catch (Throwable ignored) {
            return 100L;
        }
    }

    private boolean checkWall(Vec3 start, Vec3 end) {
        if (mc.level == null || mc.player == null) {
            return false;
        }
        BlockHitResult result = mc.level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));
        return result.getType() == HitResult.Type.BLOCK;
    }
}
